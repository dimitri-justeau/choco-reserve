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

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test for default methods of IGrid.
 */
public class TestIGrid {

    @Test
    public void testGetFullGraph() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        GraphModel model = new GraphModel();
        UndirectedGraph g = Neighborhoods.HEIGHT_CONNECTED.getFullGraph(grid, model, SetType.BIPARTITESET);
        for (int i = 0; i < 25; i++) {
            Assert.assertTrue(g.getNodes().contains(i));
            int[] nodeNeighbors = g.getNeighOf(i).toArray();
            int[] cellNeighbors = Neighborhoods.HEIGHT_CONNECTED.getNeighbors(grid, i).toArray();
            Arrays.sort(cellNeighbors);
            Arrays.sort(nodeNeighbors);
            Assert.assertTrue(Arrays.equals(cellNeighbors, nodeNeighbors));
        }
    }

    @Test
    public void testGetPartialGraph() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        GraphModel model = new GraphModel();
        int[] cells = new int[]{6, 7, 10, 11, 12, 15};
        List<Integer> listCells = IntStream.of(cells).boxed().collect(Collectors.toList());
        UndirectedGraph g = Neighborhoods.FOUR_CONNECTED.getPartialGraph(grid, model, cells, SetType.BIPARTITESET);
        int[] nodes = g.getNodes().toArray();
        Arrays.sort(nodes);
        for (int i : cells) {
            int[] nodeNeighbors = g.getNeighOf(i).toArray();
            int[] cellNeighbors = Arrays.stream(Neighborhoods.FOUR_CONNECTED.getNeighbors(grid, i).toArray())
                    .filter(v -> listCells.contains(v))
                    .toArray();
            Arrays.sort(cellNeighbors);
            Arrays.sort(nodeNeighbors);
            Assert.assertTrue(Arrays.equals(cellNeighbors, nodeNeighbors));
        }
    }

}
