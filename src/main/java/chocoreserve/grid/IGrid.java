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

package chocoreserve.grid;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Interface for Grids.
 */
public interface IGrid {

    /**
     *
     * @return The number of cells of the grid.
     */
    int getNbCells();

    /**
     *
     * @param i The index of a cell.
     * @return The neighbors of i in the grid.
     */
    int[] getNeighbors(int i);

    /**
     *
     * @param model The GraphModel to be associated with the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The full spatial graph associated to the grid. Full means that there will be one node for each cell.
     */
    default UndirectedGraph getFullGraph(GraphModel model, SetType setType) {
        int nbCells = getNbCells();
        UndirectedGraph g = new UndirectedGraph(model, nbCells, setType, false);
        for (int i = 0; i < nbCells; i++) {
            g.addNode(i);
            int[] neighbors = getNeighbors(i);
            for (int ii : neighbors) {
                g.addEdge(i, ii);
            }
        }
        return g;
    }

    /**
     *
     * @param model The GraphModel to be associated with the graph.
     * @param nodes The nodes to be included in the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The partial graph associated to a subset of cells of the grid.
     */
    default UndirectedGraph getPartialGraph(GraphModel model, int[] nodes, SetType setType) {
        int nbCells = getNbCells();
        UndirectedGraph partialGraph = new UndirectedGraph(model, nbCells, setType, false);
        for (int i : nodes) {
            partialGraph.addNode(i);
        }
        for (int i : nodes) {
            int[] neighbors = getNeighbors(i);
            for (int ii : neighbors) {
                if (partialGraph.getNodes().contains(ii)) {
                    partialGraph.addEdge(i, ii);
                }
            }
        }
        return partialGraph;
    }
}
