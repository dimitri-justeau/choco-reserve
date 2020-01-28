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

package chocoreserve.grid.neighborhood;

import chocoreserve.grid.Grid;
import chocoreserve.util.objects.graphs.UndirectedGraphDecrementalCC;
import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Interface specifying a neighborhood definition in a grid.
 */
public interface INeighborhood<T extends Grid> {

    /**
     * @param grid A grid.
     * @param i The index of a cell.
     * @return The neighbors of i in the grid.
     */
    int[] getNeighbors(T grid, int i);

    /**
     * @param grid A grid.
     * @param model The GraphModel to be associated with the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The full spatial graph associated to the grid. Full means that there will be one node for each cell.
     */
    default UndirectedGraph getFullGraph(T grid, GraphModel model, SetType setType) {
        int nbCells = grid.getNbCells();
        UndirectedGraphDecrementalCC g = new UndirectedGraphDecrementalCC(model, nbCells, setType, false);
        for (int i = 0; i < nbCells; i++) {
            g.addNode(i);
            int[] neighbors = getNeighbors(grid, i);
            for (int ii : neighbors) {
                g.addEdge(i, ii);
            }
        }
        g.init();
        return g;
    }

    /**
     * @param grid A grid.
     * @param model The GraphModel to be associated with the graph.
     * @param cells The cells to be included in the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The partial graph associated to a subset of cells of the grid.
     */
    default UndirectedGraph getPartialGraph(T grid, GraphModel model, int[] cells, SetType setType) {
        int nbCells = grid.getNbCells();
        UndirectedGraph partialGraph = new UndirectedGraphIncrementalCC(model, nbCells, setType, false);
        for (int i : cells) {
            partialGraph.addNode(i);
        }
        for (int i : cells) {
            int[] neighbors = getNeighbors(grid, i);
            for (int ii : neighbors) {
                if (partialGraph.getNodes().contains(ii)) {
                    partialGraph.addEdge(i, ii);
                }
            }
        }
        return partialGraph;
    }
}
