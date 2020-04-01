/*
 * Copyright (c) 2018, Dimitri Justeau-Allaire
 *
 * CIRAD, UMR AMAP, F-34398 Montpellier, France
 * Institut Agronomique neo-Caledonien (IAC), 98800 Noumea, New Caledonia
 * AMAP, Univ Montpellier, CIRAD, CNRS, INRA, IRD, Montpellier, France
 *
 * This file is part of Choco-reserve.
 *
 * Choco-reserve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Choco-reserve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Choco-reserve.  If not, see <https://www.gnu.org/licenses/>.
 */

package chocoreserve.solver.constraints.choco.graph.spatial;

import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.variable.SpatialGraphVar;
import chocoreserve.util.ConnectivityFinderSpatialGraph;
import chocoreserve.util.objects.graphs.UndirectedGraphDecrementalCC;
import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.memory.IStateDouble;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.memory.IStateIntVector;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.monitor.SetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.procedure.IntProcedure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * Propagator for the Integral Index of Connectivity in a landscape graph.
 */
public class PropIICSpatialGraph extends Propagator<Variable> {

    private RegularSquareGrid grid;
    private SpatialGraphVar g;
    private IStateDouble iic_lb;
    private IStateDouble iic_ub;
    private IntVar iic;
    private int areaLandscape, precision;
    private SetDeltaMonitor gdm;
    private IntProcedure forceG;
    private IntProcedure removeG;
    public IStateIntVector[] allPairsShortestPathsLB;
    public IStateIntVector[] allPairsShortestPathsUB;
    public int[][] manDists;
    private ConnectivityFinderSpatialGraph connectivityFinderGUB;
    private ISet removed;
    private ISet added;
    private ISet[] fixed;
    private ISet onShortestPath;

    public PropIICSpatialGraph(SpatialGraphVar g, IntVar iic, int precision) {
        super(new Variable[]{g, iic}, PropagatorPriority.VERY_SLOW, true);
        this.grid = (RegularSquareGrid) g.getGrid();
        this.g = g;
        this.gdm = g.monitorDelta(this);
        this.iic = iic;
        this.precision = precision;
        this.iic_lb = getModel().getEnvironment().makeFloat(0);
        this.iic_ub = getModel().getEnvironment().makeFloat(1);
        this.areaLandscape = g.getGrid().getNbCells();
        this.allPairsShortestPathsLB = new IStateIntVector[areaLandscape];
        this.allPairsShortestPathsUB = new IStateIntVector[areaLandscape];
//        this.onShortestPath = new DirectedGraph(getModel(), areaLandscape, SetType.SMALLBIPARTITESET, true);
        // Initialize every distance to -1
        for (int i = 0; i < areaLandscape; i++) {
            if (g.getPotentialNodes().contains(i)) {
                allPairsShortestPathsLB[i] = getModel().getEnvironment().makeIntVector(areaLandscape, -1);
                allPairsShortestPathsUB[i] = getModel().getEnvironment().makeIntVector(areaLandscape, -1);
            }
        }
        this.manDists = new int[areaLandscape][areaLandscape];
        for (int i = 0; i < areaLandscape; i++) {
            for (int j = 0; j < areaLandscape; j++) {
                manDists[i][j] = manhattanDistance(grid, i, j);
            }
        }
        this.forceG = node -> added.add(node);
//        this.removeG = node -> removed.add(node); //updateRemoveNode(node);
        this.removeG = node -> removed.add(node);;
        this.connectivityFinderGUB = new ConnectivityFinderSpatialGraph(g.getGUB());
        removed = SetFactory.makeBitSet(0);
        added = SetFactory.makeBitSet(0);

        onShortestPath = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
        this.fixed = new ISet[areaLandscape];
    }


    @Override
    public int getPropagationConditions(int vIdx) {
        if (vIdx == 0) {
            return SetEventType.ADD_TO_KER.getMask() + SetEventType.REMOVE_FROM_ENVELOPE.getMask();
        }
        return 0;
    }

    @Override
    public void propagate(int i) throws ContradictionException {
        System.out.println("COMPUTE LB");
        computeAllPairsShortestPathsLB(grid);
        System.out.println("COMPUTE UB");
        connectivityFinderGUB.findAllCC();
        computeAllPairsShortestPathsUB(grid);
        computeIIC_LB();
        computeIIC_UB();
//        if (g.isInstantiated()) {
            System.out.println("IIC Initial (LB) = " + ((double) (iic.getLB()) / Math.pow(10, precision)));
            System.out.println("IIC Initial (UB) = " + ((double) (iic.getUB()) / Math.pow(10, precision)));
//        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (idxVarInProp == 0) {
            gdm.freeze();
            gdm.forEach(forceG, SetEventType.ADD_TO_KER);
            gdm.forEach(removeG, SetEventType.REMOVE_FROM_ENVELOPE);

            for (int node : added) {
                updateAddNode(node);
            }
            computeIIC_LB();
            added.clear();

            connectivityFinderGUB.findAllCC();

            boolean[][] checked = null;
            ISet checkedCC = SetFactory.makeBitSet(0);
            for (int node : removed) {
                if (onShortestPath.contains(node)) {
                    checked = updateRemoveNode(node, checked, checkedCC);
                    onShortestPath.remove(node);
                }
            }
            removed.clear();
            computeIIC_UB();
            gdm.unfreeze();

            if (g.isInstantiated()) {
                System.out.println("IIC (LB) = " + ( (iic.getLB())));
                System.out.println("IIC (UB) = " + ( (iic.getUB())));


//        for (int i = 0; i < areaLandscape; i++) {
//            String s = i + " : ";
//            for (int j = 0; j < areaLandscape; j++) {
//                s += allPairsShortestPathsUB[i].quickGet(j) + " ";
//            }
//            System.out.println(s);
//        }
//        System.out.println();
//        computeAllPairsShortestPathsUB(grid);
//        for (int i = 0; i < areaLandscape; i++) {
//            String s = i + " : ";
//            for (int j = 0; j < areaLandscape; j++) {
//                s += allPairsShortestPathsUB[i].quickGet(j) + " ";
//            }
//            System.out.println(s);
//        }

//                int val = (int) (iic_lb.get() * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
//                iic.updateUpperBound(val, this);
            } else {
//                computeIIC_UB();
            }
        }
    }

    @Override
    public ESat isEntailed() {
        if (g.isInstantiated()) {
            int val = (int) (iic_lb.get() * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
            if (iic.getLB() != val) {
                return ESat.FALSE;
            }
        }
//        for (int i = 0; i < areaLandscape; i++) {
//            String s = i + " : ";
//            for (int j = 0; j < areaLandscape; j++) {
//                s += allPairsShortestPathsLB[i].quickGet(j) + " ";
//            }
//            System.out.println(s);
//        }
//        System.out.println();
//        for (int i = 0; i < areaLandscape; i++) {
//            String s = i + " : ";
//            for (int j = 0; j < areaLandscape; j++) {
//                s += allPairsShortestPathsUB[i].quickGet(j) + " ";
//            }
//            System.out.println(s);
//        }
        return ESat.TRUE;
    }

    public void updateAddNode(int node) throws ContradictionException {
        double iicDelta = 0;
        if (g.getGLB() instanceof UndirectedGraphIncrementalCC) {
            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) g.getGLB();
            // 0- The added node has a zero length shortest path to itself
            iicDelta += 1.0;
            allPairsShortestPathsLB[node].quickSet(node, 0);
            // 1- get the connected component of the added node
            int[] cc = Arrays.stream(gincr.getConnectedComponent(gincr.getRoot(node)).toArray()).sorted().toArray();
            int[] dists = new int[areaLandscape];
            for (int i = 0; i < areaLandscape; i++) {
                dists[i] = -1;
            }
            // 2- Compute the shortest paths between the added node and all connected component nodes.
//            System.out.println();
            for (int dest : cc) {
                if (dists[dest] != -1) {
                    continue;
                }
                int[][] mdaResult = minimumDetour(grid, g.getGLB(), node, dest);
                int minDist = mdaResult[0][0];
                int[] shortestPath = mdaResult[1];
                for (int x = 1; x < shortestPath.length; x++) {
                    int manDist = manDists[node][shortestPath[x]];
                    int delta = x - manDist;
                    int start = shortestPath[x] > node ? node : shortestPath[x];
                    int end = shortestPath[x] > node ? shortestPath[x] : node;
                    int oldDist = allPairsShortestPathsLB[start].quickGet(end);
                    if (oldDist >= 0) {
                        iicDelta -= 2.0 / (1 + oldDist);
                    }
                    iicDelta += 2.0 / (1 + delta);
                    allPairsShortestPathsLB[start].quickSet(end, delta);
//                    allPairsShortestPathsLB[shortestPath[x]].quickSet(node, delta);

                    dists[shortestPath[x]] = x;
                }
            }
            // 3- For each pair of node in the CC, check whether the added node has created a path (two CC merged)
            //    or if it has shortened an existing path.
            for (int i = 0; i < cc.length; i++) {
                for (int j = i + 1; j < cc.length; j++) {
                    int source = cc[i];
                    int dest = cc[j];
                    int dist = allPairsShortestPathsLB[source].quickGet(dest);
                    // a. The shortest path is already optimal.
                    if (dist == 0) {
                        continue;
                    }
                    int a = dists[source];
                    int b = dists[dest];
                    int distThrough = a + b;
                    int manDist = manDists[source][dest];
                    // b. The added node has merged two CCs and is an articulation point.
                    if (dist == -1 || dist == Integer.MAX_VALUE) {
                        iicDelta += source == dest ? 0 : 2.0 / (1 + distThrough - manDist);
                        allPairsShortestPathsLB[source].quickSet(dest, distThrough - manDist);
                        continue;
                    }
                    // c. The added node has shortened an existing path.
                    if ((dist + manDist) > distThrough) {
                        iicDelta -= source == dest ? 0 : 2.0 / (1 + dist);
                        iicDelta += source == dest ? 0 : 2.0 / (1 + distThrough - manDist);
                        allPairsShortestPathsLB[source].quickSet(dest, distThrough - manDist);
//                        allPairsShortestPathsLB[dest].quickSet(source, distThrough - manDist);
                    }
                }
            }
        }
//        double oldIIC = iic_lb.get();
//        double newIIC = oldIIC + iicDelta;
//        iic_lb.set(newIIC);
//        int val = (int) (newIIC * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
//        System.out.println("Incr = " + val + " (old = " + iic_lb.get() + ")");
//        iic.updateLowerBound(val, this);
    }

    public boolean[][] updateRemoveNode(int node, boolean[][] checked_, ISet checkedCC) throws ContradictionException {

        boolean[][] checked;
        if (checked_ == null) {
            checked = new boolean[areaLandscape][areaLandscape];
        } else {
            checked = checked_;
        }

        // 1-  get the connected component(s) of the removed node
        ISet ccs = SetFactory.makeBipartiteSet(0);
        for (int i : g.getPotentialNodes().toArray()) {
            int d;
            if (i < node) {
                d = allPairsShortestPathsUB[i].quickGet(node);
            } else {
                d = allPairsShortestPathsUB[node].quickGet(i);
            }
            if (d != -1 && d != Integer.MAX_VALUE) {
                ccs.add(connectivityFinderGUB.getNodeCC()[i]);
            }
        }

        int[] ccsArray = ccs.toArray();

//        int useless = 0;
//        int done = 0;
//        int avoided = 0;

        for (int ci = 0; ci < ccsArray.length; ci++) {
            int ccIndex1 = ccsArray[ci];
            if (checkedCC.contains(ccIndex1)) {
                continue;
            }
            int[] cc1 = connectivityFinderGUB.getCC(ccIndex1);

            // 2- Disconnect separated nodes
//            for (int cj = ci + 1; cj < ccsArray.length; cj++) {
//                int ccIndex2 = ccsArray[cj];
//                if (ccIndex1 != ccIndex2) {
//                    int[] cc2 = connectivityFinderGUB.getCC(ccIndex2);
//                    for (int i = 0; i < cc1.length; i++) {
//                        int source = cc1[i];
//                        for (int n2 : cc2) {
//                            if (source < n2) {
//                                allPairsShortestPathsUB[source].quickSet(n2, -1);
//                            } else {
//                                allPairsShortestPathsUB[n2].quickSet(source, -1);
//                            }
//                        }
//                    }
//                } else {
//                    System.out.println("PROBLEM");
//                }
//            }
            // 3- Recompute distances within reduced ccs
            int nonFixedPairs = 0;
            for (int i = 0; i < cc1.length; i++) {
                int source = cc1[i];
                int[] cc1Bis;
                if (fixed[source] == null) {
                    cc1Bis = connectivityFinderGUB.getCC(ccIndex1, cc1[i]);
                } else {
                    cc1Bis = connectivityFinderGUB.getCC(ccIndex1, cc1[i], fixed[source]);
//                    System.out.println("FIXED AVOIDED = " + fixed[source].size());
                }
                for (int j = cc1Bis.length - 1; j >= 0; j--) {
                    int dest = cc1Bis[j];

                    int from = source > dest ? dest : source;
                    int to = source > dest ? source : dest;

                    if (checked[from][to]) {
                        continue;
                    }

                    int previousDist = allPairsShortestPathsUB[from].quickGet(to) + manDists[source][dest];

                    if (g.getMandatoryNodes().contains(from)) {
                        int LBDist = allPairsShortestPathsLB[from].quickGet(to) + manDists[source][dest];
                        if (previousDist == LBDist) {
                            if (fixed[from] == null) {
                                fixed[from] = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
                            }
                            fixed[from].add(to);
                            if (fixed[to] == null) {
                                fixed[to] = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
                            }
                            fixed[to].add(from);
//                            avoided++;
                            continue;
                        }
                    }

                    nonFixedPairs++;

                    int a = allPairsShortestPathsUB[Integer.min(source, node)].quickGet(Integer.max(source, node)) + manDists[source][node];
                    int b = allPairsShortestPathsUB[Integer.min(dest, node)].quickGet(Integer.max(dest, node)) + manDists[dest][node];

                    // Check whether removed node was on a shortest path between source and dest.

                    if ((a + b) > previousDist) {
                        continue;
                    }

                    // MDA algorithm
                    int[][] mdaResult = minimumDetour(grid, g.getGUB(), source, dest);
                    int[] shortestPath = mdaResult[1];
                    int minDist = mdaResult[0][0];

//                    done++;

                    if (previousDist == minDist) {
//                        useless++;
                        for (int x = 0; x < shortestPath.length; x++) {
                            for (int y = x + 1; y < shortestPath.length; y++) {
                                int start = shortestPath[x] > shortestPath[y] ? shortestPath[y] : shortestPath[x];
                                int end = shortestPath[x] > shortestPath[y] ? shortestPath[x] : shortestPath[y];
                                checked[start][end] = true;
                            }
                        }
                        continue;
                    }

                    // Every subpath of the shortest path between source and dist is a shortest path
                    // cf. Theorem 3 of Hadlock 1977.
                    for (int x = 0; x < shortestPath.length; x++) {
                        int z = shortestPath[x];
                        if (z != source && z != dest && !g.getMandatoryNodes().contains(z)) {
                            onShortestPath.add(z);
                        }
                        for (int y = x + 1; y < shortestPath.length; y++) {
                            int manDist = manDists[shortestPath[x]][shortestPath[y]];
                            int delta = Math.abs(y - x) - manDist;
                            int start = shortestPath[x] > shortestPath[y] ? shortestPath[y] : shortestPath[x];
                            int end = shortestPath[x] > shortestPath[y] ? shortestPath[x] : shortestPath[y];
                            if (!checked[start][end]) {
                                if (allPairsShortestPathsUB[start].quickGet(end) != delta) {
                                    allPairsShortestPathsUB[start].quickSet(end, delta);
                                }
                                checked[start][end] = true;
                            }
                        }
                    }
                }
            }
            if (nonFixedPairs == 0) {
                checkedCC.add(ccIndex1);
            }
        }
        // 1a- Disconnect node from every other nodes
//        for (int n : g.getGUB().getNodes()) {
//            if (n < node) {
//                allPairsShortestPathsUB[n].quickSet(node, -1);
//            } else {
//                allPairsShortestPathsUB[node].quickSet(n, -1);
//            }
//        }
//            // 1a- Disconnect node from itself
        allPairsShortestPathsUB[node].quickSet(node, -1);
//        System.out.println("USELESS = " + useless);
//        System.out.println("DONE = " + done);
//        System.out.println("AVOIDED = " + avoided);
//        System.out.println();
        return checked;
    }

    public void computeIIC_LB() throws ContradictionException {
        double iicVal = 0;
//        for (int i : g.getMandatoryNodes()) {
//            for (int j = 0; j < areaLandscape; j++) {
//                if (g.getGLB().getNodes().contains(j) && g.getGLB().getRoot(i) == g.getGLB().getRoot(j)) {
//                    int dist = allPairsShortestPathsLB[i].quickGet(j);
//                    if (dist != -1 && dist != Integer.MAX_VALUE) {
//                        iicVal += 1.0 / (1 + dist);
//                    }
//                }
//            }
//        }
        int[] roots = g.getGLB().getRoots().toArray();
        int[][] ccs = new int[roots.length][];
        for (int i =  0; i < roots.length; i++) {
            ccs[i] = Arrays.stream(g.getGLB().getConnectedComponent(roots[i]).toArray()).sorted().toArray();
        }
        for (int[] cc : ccs) {
            for (int i = 0; i < cc.length; i++) {
                for (int j = i; j < cc.length; j++) {
                    int source = cc[i];
                    int dest = cc[j];
                    int dist = allPairsShortestPathsLB[source].quickGet(dest);
                    if (dist != -1 && dist != Integer.MAX_VALUE) {
                        iicVal += (i != j) ? 2.0 / (1 + dist) : 1.0 / (1 + dist);
                    }
                }
            }
        }
        iic_lb.set(iicVal);
        int val = (int) (iic_lb.get() * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
//        System.out.println("LB = " + val + " // " + iic.getLB());
        iic.updateLowerBound(val, this);
    }

    public void computeIIC_UB() throws ContradictionException {
        double iicVal = 0;
        for (int ccIndex = 0; ccIndex < connectivityFinderGUB.getNBCC(); ccIndex++) {
            // 1-  get the connected component of the current root
            int[] ccarray = Arrays.stream(connectivityFinderGUB.getCC(ccIndex)).sorted().toArray();
            for (int i = 0; i < ccarray.length; i++) {
                for (int j = i; j < ccarray.length; j++) {
                    int source = ccarray[i];
                    int dest = ccarray[j];
                    int dist = allPairsShortestPathsUB[source].quickGet(dest);
                    if (dist != -1 && dist != Integer.MAX_VALUE) {
                        iicVal += (source != dest) ? 2.0 / (1 + dist) : 1.0 / (1 + dist);
                    }
                }
            }
        }
        iic_ub.set(iicVal);
        int val = (int) (iic_ub.get() * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
//            System.out.println("UB = " + val);
        iic.updateUpperBound(val, this);
    }

    public void computeAllPairsShortestPathsLB(RegularSquareGrid grid) {
        int maxDelta = 0;
        if (g.getGLB() instanceof UndirectedGraphIncrementalCC) {
            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) g.getGLB();
            // Reset every dist
//            int[] mandNodes = g.getMandatoryNodes().toArray();
//            for (int i = 0; i < mandNodes.length; i++) {
//                for (int j = i; j < mandNodes.length; j++) {
//                    int a = mandNodes[i];
//                    int b = mandNodes[j];
//                    if (a < b) {
//                        allPairsShortestPathsLB[a].quickSet(b, -1);
//                    } else {
//                        allPairsShortestPathsLB[b].quickSet(a, -1);
//                    }
//                }
//            }
            boolean[][] checked = new boolean[areaLandscape][areaLandscape];
            for (int root : gincr.getRoots()) {
                // 1-  get the connected component of the current root
                ISet cc = gincr.getConnectedComponent(root);
                int[] ccarray = Arrays.stream(cc.toArray()).sorted().toArray();
                for (int i = 0; i < ccarray.length; i++) {
                    int source = ccarray[i];
                    allPairsShortestPathsLB[source].quickSet(source, 0);
                    for (int j = ccarray.length - 1; j > i; j--) {
                        int dest = ccarray[j];
                        if (checked[source][dest]) {
                            continue;
                        }
                        // MDA algorithm
                        int[][] mdaResult = minimumDetour(grid, g.getGLB(), source, dest);
                        int minDist = mdaResult[0][0];
                        int[] shortestPath = mdaResult[1];
                        // Every subpath of the shortest path between source and dist is a shortest path
                        // cf. Theorem 3 of Hadlock 1977.
                        for (int x = 0; x < shortestPath.length; x++) {
                            for (int y = x + 1; y < shortestPath.length; y++) {
                                int manDist = manDists[shortestPath[x]][shortestPath[y]];
                                int delta = Math.abs(y - x) - manDist;
                                int start = shortestPath[x] > shortestPath[y] ? shortestPath[y] : shortestPath[x];
                                int end = shortestPath[x] > shortestPath[y] ? shortestPath[x] : shortestPath[y];
                                if (!checked[start][end]) {
                                    if (delta > maxDelta) {
                                        maxDelta = delta;
                                    }
                                    allPairsShortestPathsLB[start].quickSet(end, delta);
                                    checked[start][end] = true;
                                    // If shortest path is manhattan distance in LB, it is also in UB
                                    if (delta == 0) {
                                        allPairsShortestPathsUB[start].quickSet(end, delta);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.println("MAX DELTA UB = " + maxDelta);

    }

    public void computeAllPairsShortestPathsUB(RegularSquareGrid grid) {
        // Reset every dist
//        int[] potNodes = g.getPotentialNodes().toArray();
//        for (int i = 0; i < potNodes.length; i++) {
//            for (int j = i; j < potNodes.length; j++) {
//                int a = potNodes[i];
//                int b = potNodes[j];
//                if (a < b) {
//                    allPairsShortestPathsUB[a].quickSet(b, -1);
//                } else {
//                    allPairsShortestPathsUB[b].quickSet(a, -1);
//                }
//            }
//        }
        int maxDelta = 0;
        int nonFixed = 0;
        boolean[][] checked = new boolean[areaLandscape][areaLandscape];
        for (int ccIndex = 0; ccIndex < connectivityFinderGUB.getNBCC(); ccIndex++) {
            // 1-  get the connected component of the current root
            int[] ccarray = Arrays.stream(connectivityFinderGUB.getCC(ccIndex)).sorted().toArray();
            for (int i = 0; i < ccarray.length; i++) {
                int source = ccarray[i];
                allPairsShortestPathsUB[source].quickSet(source, 0);
                for (int j = ccarray.length - 1; j > i; j--) {
                    int dest = ccarray[j];
                    if (g.getMandatoryNodes().contains(source) && allPairsShortestPathsLB[source].quickGet(dest) == 0) {
                        if (fixed[source] == null) {
                            fixed[source] = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
                        }
                        fixed[source].add(dest);
                        if (fixed[dest] == null) {
                            fixed[dest] = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
                        }
                        fixed[dest].add(source);
//                        allPairsShortestPathsUB[source].quickSet(dest, 0);
//                        allPairsShortestPathsUB[dest].quickSet(source, 0);
                        continue;
                    } else {
                        nonFixed++;
                    }
                    if (checked[source][dest]) {
                        continue;
                    }
                    // MDA algorithm
                    int[][] mdaResult = minimumDetour(grid, g.getGUB(), source, dest);
                    int minDist = mdaResult[0][0];
                    int[] shortestPath = mdaResult[1];
                    // Every subpath of the shortest path between source and dist is a shortest path
                    // cf. Theorem 3 of Hadlock 1977.
                    for (int x = 0; x < shortestPath.length; x++) {
                        int z = shortestPath[x];
                        if (!g.getMandatoryNodes().contains(source) || allPairsShortestPathsLB[source].quickGet(dest) == minDist - manDists[source][dest]) {
                            if (z != source && z != dest && !g.getMandatoryNodes().contains(z)) {
                                onShortestPath.add(z);
                            }
                        }
                        for (int y = x + 1; y < shortestPath.length; y++) {
                            int manDist = manDists[shortestPath[x]][shortestPath[y]];
                            int delta = Math.abs(y - x) - manDist;
                            int start = shortestPath[x] > shortestPath[y] ? shortestPath[y] : shortestPath[x];
                            int end = shortestPath[x] > shortestPath[y] ? shortestPath[x] : shortestPath[y];
                            if (!checked[start][end]) {
                                if (delta > maxDelta) {
                                    maxDelta = delta;
                                }
                                allPairsShortestPathsUB[start].quickSet(end, delta);
                                //                            allPairsShortestPathsUB[end].quickSet(start, delta);
                                if (g.getMandatoryNodes().contains(start) && allPairsShortestPathsLB[start].quickGet(end) == delta) {
                                    if (fixed[start] == null) {
                                        fixed[start] = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
                                    }
                                    fixed[start].add(end);
                                    if (fixed[end] == null) {
                                        fixed[end] = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, getModel());
                                    }
                                    fixed[end].add(start);
                                }
//                                for (int z = x + 1; z < y; z++) {
//                                    int on = shortestPath[z];
//                                    if (!g.getMandatoryNodes().contains(on)) {
//                                        onShortestPath.addArc(on, start);
//                                        onShortestPath.addArc(start, end);
//                                    }
//                                }
                                checked[start][end] = true;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("NON FIXED = " + nonFixed);
//        System.out.println("NB PAIRS = " + nbPairs);
//        System.out.println("ON SHORTEST PATH = " + onShortestPath.size());
    }

    /**
     * Minimum Detour Algorithm for grid graphs (Hadlock 1977).
     * @param source
     * @param dest
     * @return [ [dist], [path] ]
     */
    public int[][] minimumDetour(RegularSquareGrid grid, UndirectedGraph graph, int source, int dest) {

        if (!graph.getNodes().contains(source)) {
            return new int[][] { {-1}, null };
        }
        if (!graph.getNodes().contains(dest)) {
            return new int[][] { {-1}, null };
        }

        // When the graph has maintains connected component, avoid running the algorithm for two nodes
        // in different connected components.
//        if (graph instanceof UndirectedGraphIncrementalCC) {
//            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) graph;
//            int sourceRoot = gincr.getRoot(source);
//            int destRoot = gincr.getRoot(dest);
//            if (sourceRoot != destRoot) {
//                return new int[][] { {Integer.MAX_VALUE}, null };
//            }
//        } else {
//            int sourceCC = connectivityFinderGUB.getNodeCC()[source];
//            int destCC = connectivityFinderGUB.getNodeCC()[dest];
//            if (sourceCC != destCC) {
//                return new int[][] { {Integer.MAX_VALUE}, null };
//            }
//        }

        boolean[] visited = new boolean[graph.getNbMaxNodes()];

        int current = source;
        int detours = 0;

        int[] prev = new int[graph.getNbMaxNodes()];
        for (int i = 0; i < graph.getNbMaxNodes(); i++) {
            prev[i] = -1;
        }

        int[] positives = new int[graph.getNbMaxNodes()];
        int posIndex = 0;
        int[] negatives = new int[graph.getNbMaxNodes()];
        int negIndex = 0;
        int[] currentPos = new int[graph.getNbMaxNodes()];
        int curPosIndex = 0;
        int[] currentNeg = new int[graph.getNbMaxNodes()];
        int curNegIndex = 0;

        boolean skip2 = false;

        prev[source] = source;

        while (current != dest) {

            if (!skip2) {
                // 2
                visited[current] = true;

                for (int neigh : graph.getNeighOf(current)) { // #2
                    if (!visited[neigh]) {
                        if (manDists[neigh][dest] < manDists[current][dest]) {
                            positives[posIndex++] = neigh;
                            currentPos[curPosIndex++] = current;
                        } else {
                            negatives[negIndex++] = neigh;
                            currentNeg[curNegIndex++] = current;
                        }
                    }
                }
            }

            skip2 = false;
            int next = -1;
            int potPrev = -1;
            // 3
            while (posIndex > 0) {
                int candidate = positives[--posIndex];
                int prevCandidate = currentPos[--curPosIndex];
                if (!visited[candidate]) {
                    next = candidate;
                    potPrev = prevCandidate;
                    break;
                }
            }
            // 4
            if (posIndex == 0 && negIndex > 0 && next == -1) {
                detours += 1;
                while (negIndex > 0) {
                    positives[posIndex++] = negatives[--negIndex];
                }
                while (curNegIndex > 0) {
                    currentPos[curPosIndex++] = currentNeg[--curNegIndex];
                }
                skip2 = true;
                next = current;
            }

            if (next == -1) {
                return new int[][] { {Integer.MAX_VALUE}, null };
            }
            if (potPrev != -1) {
                prev[next] = potPrev;
            }
            current = next;

        }
        int dist = manDists[source][dest] + 2 * detours;
        int[] path = new int[dist + 1];
        path[dist] = dest;
        for (int i = dist - 1; i >= 0; i--) {
            path[i] = prev[path[i + 1]];
        }
        return new int[][] { {dist}, path, prev};
    }

    public int manhattanDistance(RegularSquareGrid grid, int source, int dest) {
            int[] sourceCoords = grid.getCoordinatesFromIndex(source);
            int[] destCoords = grid.getCoordinatesFromIndex(dest);
            int dx = Math.abs(sourceCoords[1] - destCoords[1]);
            int dy = Math.abs(sourceCoords[0] - destCoords[0]);
            return dx + dy;
    }
}
