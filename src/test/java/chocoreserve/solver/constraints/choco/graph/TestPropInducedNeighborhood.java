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

import chocoreserve.grid.IGrid;
import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test for PropInducedNeighborhood constraint.
 * Tests are performed on a 5x5 4-connected square grid.
 *     -------------------
 *    | 0 | 1 | 2 | 3 | 4 |
 *     -------------------
 *    | 5 | 6 | 7 | 8 | 9 |
 *     -------------------
 *    | 10| 11| 12| 13| 14|
 *     -------------------
 *    | 15| 16| 17| 18| 19|
 *     -------------------
 *    | 20| 21| 22| 23| 24|
 */
public class TestPropInducedNeighborhood {

    @Test
    public void testPropInducedNeighborhood() {
        IGrid grid = new FourConnectedSquareGrid(5, 5);
        GraphModel model = new GraphModel("TestPropInducedNeigh");
        UndirectedGraphVar g = model.graphVar(
            "testGraph",
            new UndirectedGraph(model, grid.getNbCells(), SetType.BIPARTITESET, false),
            grid.getFullGraph(model, SetType.BIPARTITESET)
        );
        BoolVar[] cells = model.boolVarArray(grid.getNbCells());
        model.nodesChanneling(g, cells).post();
        model.post(new Constraint("inducedNeigh", new PropInducedNeighborhood(g)));
        int[] selected = new int[] {6, 7, 8, 11, 12, 13, 17, 22};
        int[] unselected = new int[] {0, 1, 2, 3, 4, 5, 9, 10, 14, 15, 16, 18, 19, 20, 21, 23, 24};
        for (int i : selected) {
            model.arithm(cells[i], "=", 1).post();
        }
        for (int i : unselected) {
            model.arithm(cells[i], "=", 0).post();
        }
        Solver solver = model.getSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(solutions.size(), 1);
        try {
            solutions.get(0).restore();
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
        int[] nodes = g.getMandatoryNodes().toArray();
        Arrays.sort(nodes);
        Assert.assertTrue(Arrays.equals(nodes, selected));
        // 6 neighbors
        int[] neigh = g.getMandNeighOf(6).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {7, 11}));
        // 7 neighbors
        neigh = g.getMandNeighOf(7).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {6, 8, 12}));
        // 8 neighbors
        neigh = g.getMandNeighOf(8).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {7, 13}));
        // 11 neighbors
        neigh = g.getMandNeighOf(11).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {6, 12}));
        // 12 neighbors
        neigh = g.getMandNeighOf(12).toArray();
        Assert.assertTrue(Arrays.equals(neigh, new int[] {7, 11, 13, 17}));
        // 13 neighbors
        neigh = g.getMandNeighOf(13).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {8, 12}));
        // 17 neighbors
        neigh = g.getMandNeighOf(17).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {12, 22}));
        // 22 neighbors
        neigh = g.getMandNeighOf(22).toArray();
        Arrays.sort(neigh);
        Assert.assertTrue(Arrays.equals(neigh, new int[] {17}));
    }
}
