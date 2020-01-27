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

package chocoreserve.solver.constraints.choco.graph;

import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.memory.IStateIntVector;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.procedure.IntProcedure;

import java.util.*;

/**
 * Propagator for the Integral Index of Connectivity in a landscape graph.
 */
public class PropIIC extends Propagator<Variable> {

    public final static double EPSILON = 1e-5;

    private RegularSquareGrid grid;
    private UndirectedGraphVar g;
    private RealVar iic;
    private int areaLandscape;
    private GraphDeltaMonitor gdm;
    private IntProcedure forceG;
    public IStateIntVector[] allPairsShortestPathsLB;

    public PropIIC(RegularSquareGrid grid, UndirectedGraphVar g, RealVar iic) {
        super(new Variable[]{g, iic}, PropagatorPriority.CUBIC, true);
        this.grid = grid;
        this.g = g;
        this.gdm = g.monitorDelta(this);
        this.iic = iic;
        this.areaLandscape = g.getUB().getNbMaxNodes();
        this.allPairsShortestPathsLB = new IStateIntVector[areaLandscape];
        // Initialize every distance to -1
        for (int i = 0; i < areaLandscape; i++) {
            allPairsShortestPathsLB[i] = getModel().getEnvironment().makeIntVector(areaLandscape, -1);
        }
        forceG = node -> updateAddNode(node);
    }


    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ADD_NODE.getMask();
    }

    @Override
    public void propagate(int i) throws ContradictionException {

    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (idxVarInProp == 0) {
            gdm.freeze();
            gdm.forEachNode(forceG, GraphEventType.ADD_NODE);
            gdm.unfreeze();
        }
    }

    @Override
    public ESat isEntailed() {
        return ESat.TRUE;
    }

    private void updateAddNode(int node) {
        if (g.getLB() instanceof UndirectedGraphIncrementalCC) {
            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) g.getLB();
            // 1-  get the connected component of the added node
            Set<Integer> cc = gincr.getConnectedComponent(gincr.getRoot(node));
            int[] ccarray = cc.stream().mapToInt(i -> i).sorted().toArray();
            int[] dists = new int[ccarray.length];
            // 2- Compute the shortest paths between the added node and all connected component nodes.
            for (int i = 0; i < ccarray.length; i++) {
                int dest = ccarray[i];
                int[][] mdaResult = minimumDetour(grid, g.getLB(), node, dest);
                int minDist = mdaResult[0][0];
                int manDist = manhattanDistance(grid, node, dest);
                int delta = minDist - manDist;
                allPairsShortestPathsLB[node].quickSet(dest, delta);
                allPairsShortestPathsLB[dest].quickSet(node, delta);
                dists[i] = minDist;
            }
            // 3- For each pair of node in the CC, check whether the added node has created a path (two CC merged)
            //    or if it has shortened an existing path.
            for (int i = 0; i < ccarray.length; i++) {
                for (int j = ccarray.length - 1; j > i; j--) {
                    int source = ccarray[i];
                    int dest = ccarray[j];
                    int dist = allPairsShortestPathsLB[source].quickGet(dest);
                    // a. The shortest path is already optimal.
                    if (dist == 0) {
                        continue;
                    }
                    int a = dists[i];
                    int b = dists[j];
                    int distThrough = a + b;
                    int manDist = manhattanDistance(grid, source, dest);
                    // b. The added node has merged two CCs and is an articulation point.
                    if (dist == -1 || dist == Integer.MAX_VALUE) {
                        allPairsShortestPathsLB[source].quickSet(dest, distThrough - manDist);
                        allPairsShortestPathsLB[dest].quickSet(source, distThrough - manDist);
                        continue;
                    }
                    // c. The added node has shortened an existing path.
                    if ((dist + manDist) > distThrough) {
                        allPairsShortestPathsLB[source].quickSet(dest, distThrough - manDist);
                        allPairsShortestPathsLB[dest].quickSet(source, distThrough - manDist);
                    }
                }
            }
        }
    }

    public double computeIIC_LB() {
        double iic = 0;
        for (int i = 0; i < areaLandscape; i++) {
            if (g.getLB().getNodes().contains(i)) {
                for (int j = 0; j < areaLandscape; j++) {
                    int dist = allPairsShortestPathsLB[i].quickGet(j);
                    if (dist != -1 && dist != Integer.MAX_VALUE) {
                        iic += 1.0 / (1 + allPairsShortestPathsLB[i].quickGet(j));
                    }
                }
            }
        }
        System.out.println("IICnumMDA = " + iic);
        System.out.println("IICAreaLandscape = " + areaLandscape);
        return iic / Math.pow(areaLandscape, 2);
    }

    public void computeAllPairsShortestPathsLB(RegularSquareGrid grid) {
        // Reset non optimal distances
//        for (int source = 0; source < areaLandscape; source++) {
//            for (int dest = source; dest < areaLandscape; dest++) {
//                if (allPairsShortestPathsLB[source].quickGet(dest) != 0) {
//                    allPairsShortestPathsLB[source].quickSet(dest, -1);
//                    allPairsShortestPathsLB[dest].quickSet(source   , -1);
//                }
//            }
//        }

        if (g.getLB() instanceof UndirectedGraphIncrementalCC) {
            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) g.getLB();
            for (int root : gincr.getRoots()) {
                // 1-  get the connected component of the current root
                Set<Integer> cc = gincr.getConnectedComponent(root);
                int[] ccarray = cc.stream().mapToInt(i -> i).sorted().toArray();
                for (int i = 0; i < ccarray.length; i++) {
                    for (int j = ccarray.length - 1; j >= i; j--) {
                        int source = ccarray[i];
                        int dest = ccarray[j];
                        if (allPairsShortestPathsLB[source].quickGet(dest) != -1) {
                            continue;
                        }
                        // MDA algorithm
                        int[][] mdaResult = minimumDetour(grid, g.getLB(), source, dest);
                        int minDist = mdaResult[0][0];
                        int[] shortestPath = mdaResult[1];
                        // Every subpath of the shortest path between source and dist is a shortest path
                        // cf. Theorem 3 of Hadlock 1977.
                        for (int x = 0; x < shortestPath.length; x++) {
                            for (int y = 0; y < shortestPath.length; y++) {
                                int manDist = manhattanDistance(grid, shortestPath[x], shortestPath[y]);
                                int delta = Math.abs(y - x) - manDist;
                                allPairsShortestPathsLB[shortestPath[x]].quickSet(shortestPath[y], delta);
                            }
                        }
                    }
                }
            }
        }

//        for (int source = 0; source < areaLandscape; source++) {
//            // MDA algorithm
//            for (int dest = areaLandscape - 1; dest >= source; dest--) {
//                // Avoid recomputing
//                if (allPairsShortestPathsLB[source].quickGet(dest) != -1) {
//                    continue;
//                }
//                int[][] mdaResult = minimumDetour(grid, g.getLB(), source, dest);
//                int minDist = mdaResult[0][0];
//                int[] shortestPath = mdaResult[1];
//                // Case when there is no shortest path (node not in graph of nodes in different connected component)
//                if (shortestPath == null) {
//                    allPairsShortestPathsLB[source].quickSet(dest, minDist);
//                    allPairsShortestPathsLB[dest].quickSet(source   , minDist);
//                    continue;
//                }
//                // Every subpath of the shortest path between source and dist is a shortest path
//                // cf. Theorem 3 of Hadlock 1977.
//
//                for (int x = 0; x < shortestPath.length; x++) {
//                    for (int y = 0; y < shortestPath.length; y++) {
//
//                        // Experimental : Distance is the difference between manhattan distance and real distance.
//                        int manDist = manhattanDistance(grid, shortestPath[x], shortestPath[y]);
//                        int delta = Math.abs(y - x) - manDist;
//                        allPairsShortestPathsLB[shortestPath[x]].quickSet(shortestPath[y], delta);
////                        allPairsShortestPathsLB[shortestPath[x]][shortestPath[y]] = Math.abs(y - x);
//                    }
//                }
//            }
//        }
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


        Set<Integer> visited = new HashSet<>();

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
