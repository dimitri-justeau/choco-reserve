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
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Propagator for the Integral Index of Connectivity in a landscape graph.
 */
public class PropIIC extends Propagator<Variable> {

    public final static double EPSILON = 1e-5;

    private UndirectedGraphVar g;
    private RealVar iic;
    private int areaLandscape;
    private Set<Integer> fixedNodes;
    public int[][] allPairsShortestPathsLB;
    public int[][] allPairsShortestPathsUB;

    public PropIIC(UndirectedGraphVar g, RealVar iic) {
        super(new Variable[] {g, iic});
        this.g = g;
        this.iic = iic;
        this.areaLandscape = g.getUB().getNbMaxNodes();
        this.fixedNodes = new HashSet<>();
        for (int i : g.getLB().getNodes()) {
            this.fixedNodes.add(i);
        }
        this.allPairsShortestPathsLB = new int[areaLandscape][];
        this.allPairsShortestPathsUB = new int[areaLandscape][];
        // Initialize every distance to -1
        for (int i = 0; i < areaLandscape; i++) {
            allPairsShortestPathsLB[i] = new int[areaLandscape];
            allPairsShortestPathsUB[i] = new int[areaLandscape];
            Arrays.fill(allPairsShortestPathsLB[i], -1);
            Arrays.fill(allPairsShortestPathsUB[i], -1);
        }
    }

    @Override
    public void propagate(int i) throws ContradictionException {

    }

    @Override
    public ESat isEntailed() {
        return null;
    }

    public double computeIIC(UndirectedGraph graph) {
        int[][] allPairsShortestPaths = allPairsShortestPaths(graph);
        double iic = 0;
        for (int i = 0; i < areaLandscape; i++) {
            if (graph.getNodes().contains(i)) {
                for (int j = 0; j < areaLandscape; j++) {
                    if (graph.getNodes().contains(j)) {
                        if (allPairsShortestPaths[i][j] != Integer.MAX_VALUE) {
                            iic += 1.0 / (1 + allPairsShortestPaths[i][j]);
                        }
                    }
                }
            }
        }
        return iic / Math.pow(areaLandscape, 2);
    }

    public double computeIIC_LB() {
        double iic = 0;
        for (int i = 0; i < areaLandscape; i++) {
            if (g.getLB().getNodes().contains(i)) {
                for (int j = 0; j < areaLandscape; j++) {
                    if (g.getLB().getNodes().contains(j)) {
                        if (allPairsShortestPathsLB[i][j] != Integer.MAX_VALUE) {
                            iic += 1.0 / (1 + allPairsShortestPathsLB[i][j]);
                        }
                    }
                }
            }
        }
        System.out.println("IICnumMDA = " + iic);
        System.out.println("IICAreaLandscape = " + areaLandscape);
        return iic / Math.pow(areaLandscape, 2);
    }


    public double computeIIC_MDA(RegularSquareGrid grid, UndirectedGraph graph) {
        int[][] allPairsShortestPaths = allPairsShortestPathsMDA(grid, graph);
        double iic = 0;
        for (int i = 0; i < areaLandscape; i++) {
            if (graph.getNodes().contains(i)) {
                for (int j = 0; j < areaLandscape; j++) {
                    if (graph.getNodes().contains(j)) {
                        if (allPairsShortestPaths[i][j] != Integer.MAX_VALUE) {
                            iic += 1.0 / (1 + allPairsShortestPaths[i][j]);
                        }
                    }
                }
            }
        }
        System.out.println("IICnumMDA = " + iic);
        System.out.println("IICAreaLandscape = " + areaLandscape);
        return iic / Math.pow(areaLandscape, 2);
    }

    /**
     * Repeat Dijkstra's algorithm on each vertex of the graph, avoiding symmetries.
     * @return
     */
    public int[][] allPairsShortestPaths(UndirectedGraph graph) {
        int[][] allPairsShortestPaths = new int[areaLandscape][];
        int[] prev = new int[areaLandscape];

        for (int source = 0; source < areaLandscape; source++) {

            int[] dist = new int[areaLandscape];

            // If node not in graph we label everything related to it with -1
            if (!graph.getNodes().contains(source)) {
                Arrays.fill(dist, -1);
            } else { // Dijkstra's algorithm
                Set<Integer> unvisited = new HashSet<>();

                for (int i = source; i < areaLandscape; i++) {
                    dist[i] = graph.getNodes().contains(i) ? Integer.MAX_VALUE : -1;
                    prev[i] = -1;
                    unvisited.add(i);
                }

                // Paste previous symmetric results
                for (int i = 0; i < source; i++) {
                    dist[i] = allPairsShortestPaths[i][source];
                }

                dist[source] = 0;

                int current;

                while (!unvisited.isEmpty()) {

                    current = IntStream.range(0, areaLandscape)
                            .filter(i -> unvisited.contains(i))
                            .reduce((i, j) -> dist[i] < dist[j] ? i : j)
                            .getAsInt();

                    unvisited.remove(current);

                    for (int neigh : graph.getNeighOf(current)) {
                        if (unvisited.contains(neigh)) {
                            int newDist = dist[current] == Integer.MAX_VALUE ? dist[current] : dist[current] + 1;
                            if (newDist < dist[neigh]) {
                                dist[neigh] = newDist;
                                prev[neigh] = current;
                            }
                        }
                    }
                }
            }
            allPairsShortestPaths[source] = dist.clone();
        }

        return allPairsShortestPaths;
    }

    public void computeAllPairsShortestPathsLB(RegularSquareGrid grid) {
        // Reset non fixed distances
        for (int source = 0; source < areaLandscape; source++) {
            for (int dest = source; dest < areaLandscape; dest++) {
                if (!fixedNodes.contains(source) || !fixedNodes.contains(dest)) {
                    allPairsShortestPathsLB[source][dest] = -1;
                    allPairsShortestPathsLB[dest][source] = -1;
                }
            }
        }

        for (int source = 0; source < areaLandscape; source++) {
            // MDA algorithm
            for (int dest = source; dest < areaLandscape; dest++) {
                // Avoid recomputing
                if (allPairsShortestPathsLB[source][dest] != -1) {
                    continue;
                }
                int[][] mdaResult = minimumDetour(grid, g.getLB(), source, dest);
                int minDist = mdaResult[0][0];
                int[] shortestPath = mdaResult[1];
                // Case when there is no shortest path (node not in graph of nodes in different connected component)
                if (shortestPath == null) {
                    allPairsShortestPathsLB[source][dest] = minDist;
                    allPairsShortestPathsLB[dest][source] = minDist;
                    continue;
                }
                // Every subpath of the shortest path between source and dist is a shortest path
                // cf. Theorem 3 of Hadlock 1977.
                for (int x = 0; x < shortestPath.length; x++) {
                    for (int y = 0; y < shortestPath.length; y++) {
                        allPairsShortestPathsLB[shortestPath[x]][shortestPath[y]] = Math.abs(y - x);
                    }
                }
            }
        }
    }

    /**
     * Repeat Minimum Detour Algorithm on each vertex of the graph, avoiding symmetries.
     * @return
     */
    public int[][] allPairsShortestPathsMDA(RegularSquareGrid grid, UndirectedGraph graph) {
        int[][] allPairsShortestPaths = new int[areaLandscape][];
        // Initialize every distance to -1
        for (int i = 0; i < areaLandscape; i++) {
            allPairsShortestPaths[i] = new int[areaLandscape];
            Arrays.fill(allPairsShortestPaths[i], -1);
        }
        for (int source = 0; source < areaLandscape; source++) {
            // MDA algorithm
            for (int dest = source; dest < areaLandscape; dest++) {
                // Avoid recomputing
                if (allPairsShortestPaths[source][dest] != -1) {
                    continue;
                }
                int[][] mdaResult = minimumDetour(grid, graph, source, dest);
                int minDist = mdaResult[0][0];
                int[] shortestPath = mdaResult[1];
                // Case when there is no shortest path (node not in graph of nodes in different connected component)
                if (shortestPath == null) {
                    allPairsShortestPaths[source][dest] = minDist;
                    allPairsShortestPaths[dest][source] = minDist;
                    continue;
                }
                // Every subpath of the shortest path between source and dist is a shortest path
                // cf. Theorem 3 of Hadlock 1977.
                for (int x = 0; x < shortestPath.length; x++) {
                    for (int y = 0; y < shortestPath.length; y++) {
                        allPairsShortestPaths[shortestPath[x]][shortestPath[y]] = Math.abs(y - x);
                    }
                }
            }
        }
        return allPairsShortestPaths;
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
            UndirectedGraphIncrementalCC gincr = (UndirectedGraphIncrementalCC) g.getLB();
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
            //System.out.println(next);

            if (next == -1) {
                return new int[][] { {Integer.MAX_VALUE}, null };
            }
            if (potPrev != -1) {
                prev[next] = potPrev;
            }
            current = next;

        }
        //System.out.println("Nb detours = " + detours);
        int dist = manhattanDistance(grid, source, dest) + 2 * detours;
        int[] path = new int[dist + 1];
        path[dist] = dest;
//        System.out.println("Prev = " + Arrays.toString(prev));
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
