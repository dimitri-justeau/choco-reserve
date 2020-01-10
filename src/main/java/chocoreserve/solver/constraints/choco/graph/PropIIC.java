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

import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Propagator for the Integral Index of Connectivity in a landscape graph.
 */
public class PropIIC extends Propagator<Variable> {

    public final static double EPSILON = 1e-5;

    private UndirectedGraphVar g;
    private RealVar iic;
    private int areaLandscape;

    public PropIIC(UndirectedGraphVar g, RealVar iic) {
        super(new Variable[] {g, iic});
        this.g = g;
        this.iic = iic;
        this.areaLandscape = g.getUB().getNbMaxNodes();
    }

    @Override
    public void propagate(int i) throws ContradictionException {

    }

    @Override
    public ESat isEntailed() {
        return null;
    }

    public double computeIIC(UndirectedGraph graph) {
        return 0;
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
}
