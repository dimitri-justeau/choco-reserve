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
import chocoreserve.util.objects.graphs.UndirectedGraphDecrementalCC;
import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.memory.IStateDouble;
import org.chocosolver.memory.IStateIntVector;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.monitor.SetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.procedure.IntProcedure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Propagator for the Integral Index of Connectivity in a landscape graph.
 */
public class PropIICSpatialGraphAlt extends Propagator<Variable> {

    private RegularSquareGrid grid;
    private SpatialGraphVar g;
    private IStateDouble iic_lb;
    private IStateDouble iic_ub;
    private IntVar iic;
    private int areaLandscape, precision;
    public int[][] manDists;

    public PropIICSpatialGraphAlt(SpatialGraphVar g, IntVar iic, int precision) {
        super(new Variable[]{g, iic}, PropagatorPriority.CUBIC, false);
        this.grid = (RegularSquareGrid) g.getGrid();
        this.g = g;
        this.iic = iic;
        this.precision = precision;
        this.iic_lb = getModel().getEnvironment().makeFloat(0);
        this.iic_ub = getModel().getEnvironment().makeFloat(1);
        this.areaLandscape = g.getGrid().getNbCells();
        // Initialize every distance to -1
        this.manDists = new int[areaLandscape][areaLandscape];
        for (int i = 0; i < areaLandscape; i++) {
            for (int j = i; j < areaLandscape; j++) {
                manDists[i][j] = manhattanDistance(grid, i, j);
            }
        }
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
        if (g.isInstantiated()) {
            System.out.println("IIC Initial (LB) = " + ((double) (iic.getLB()) / Math.pow(10, precision)));
            System.out.println("IIC Initial (UB) = " + ((double) (iic.getUB()) / Math.pow(10, precision)));
            computeIIC_LB();
            computeIIC_UB();
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
        int[] nodes = Arrays.stream(g.getGLB().getNodes().toArray()).sorted().toArray();
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i; j < nodes.length; j++) {
                int source = nodes[i];
                int dest = nodes[j];
                int manDist = source < dest ? manDists[source][dest] : manDists[dest][source];
                int dist = minimumDetour(grid, g.getGLB(), source, dest)[0][0];
                if (dist != -1 && dist != Integer.MAX_VALUE) {
                    iicVal += (i != j) ? 2.0 / (1 + dist - manDist) : 1.0 / (1 + dist - manDist);
                }
            }
        }
        iic_lb.set(iicVal);
        int val = (int) (iic_lb.get() * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
//        System.out.println("LB = " + val);
        iic.updateLowerBound(val, this);
    }

    public void computeIIC_UB() throws ContradictionException {
        double iicVal = 0;
        int[] nodes = Arrays.stream(g.getGUB().getNodes().toArray()).sorted().toArray();
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i; j < nodes.length; j++) {
                int source = nodes[i];
                int dest = nodes[j];
                int manDist = source < dest ? manDists[source][dest] : manDists[dest][source];
                int dist = minimumDetour(grid, g.getGUB(), source, dest)[0][0];
                if (dist != -1 && dist != Integer.MAX_VALUE) {
                    iicVal += (i != j) ? 2.0 / (1 + dist - manDist) : 1.0 / (1 + dist - manDist);
                }
            }
        }
            iic_ub.set(iicVal);
            int val = (int) (iic_ub.get() * Math.pow(10, precision) / Math.pow(areaLandscape, 2));
//            System.out.println("UB = " + val);
            iic.updateUpperBound(val, this);
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
        if (graph instanceof UndirectedGraphIncrementalCC) {
            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) graph;
            int sourceRoot = gincr.getRoot(source);
            int destRoot = gincr.getRoot(dest);
            if (sourceRoot != destRoot) {
                return new int[][] { {Integer.MAX_VALUE}, null };
            }
        }


        ISet visited = SetFactory.makeBitSet(0);

        int current = source;
        int detours = 0;

        int[] prev = new int[graph.getNbMaxNodes()];
        for (int i = 0; i < graph.getNbMaxNodes(); i++) {
            prev[i] = -1;
        }

        Stack<Integer> positives = new Stack<>();
        Stack<Integer> negatives = new Stack<>();
        Stack<Integer> currentPos = new Stack<>();
        Stack<Integer> currentNeg = new Stack<>();

        boolean skip2 = false;

        prev[source] = source;

        while (current != dest) {

            if (!skip2) {
                // 2
                visited.add(current);

                for (int neigh : graph.getNeighOf(current)) { // #2
                    if (!visited.contains(neigh)) {
                        if (manhattanDistance(grid, neigh, dest) < manhattanDistance(grid, current, dest)) {
                            positives.add(neigh);
                            currentPos.add(current);
                        } else {
                            negatives.add(neigh);
                            currentNeg.add(current);
                        }
                    }
                }
            }

            skip2 = false;
            int next = -1;
            int potPrev = -1;
            // 3
            while (positives.size() > 0) {
                int candidate = positives.pop();
                int prevCandidate = currentPos.pop();
                if (!visited.contains(candidate)) {
                    next = candidate;
                    potPrev = prevCandidate;
                    break;
                }
            }
            // 4
            if (positives.size() == 0 && negatives.size() > 0 && next == -1) {
                detours += 1;
                while (!negatives.isEmpty()) {
                    positives.add(negatives.pop());
                }
                while (!currentNeg.isEmpty()) {
                    currentPos.add(currentNeg.pop());
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
        int dist = manhattanDistance(grid, source, dest) + 2 * detours;
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
