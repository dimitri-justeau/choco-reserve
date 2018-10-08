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

package chocoreserve.solver.constraints;

import chocoreserve.grid.IGrid;
import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.solver.ReserveModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test for NbComponents constraint.
 */
public class TestNbComponents {

    /**
     * Test case 1: 3x3 4-connected square grid, 5 CC -> 1 solution (0, 2, 4, 6, 8).
     *     -----------
     *    | 0 | 1 | 2 |
     *     -----------
     *    | 3 | 4 | 5 |
     *     -----------
     *    | 6 | 7 | 8 |
     *     -----------
     */
    @Test
    public void testNbComponentsSuccessCase1() {
        IGrid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.nbComponents(5, 5).post();
        UndirectedGraphVar g = reserveModel.getSpatialGraphVar();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(1, solutions.size());
        try {
            solutions.get(0).restore();
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
        int[] nodes = reserveModel.getSpatialGraphVar().getMandatoryNodes().toArray();
        Arrays.sort(nodes);
        Assert.assertTrue(Arrays.equals(nodes, new int[] {0, 2, 4, 6, 8}));
    }

    /**
     * Test case 1: 2x2 4-connected square grid, 2 CC -> 2 solution (0, 3) and (1, 2).
     *     -------
     *    | 0 | 1 |
     *     -------
     *    | 2 | 3 |
     *     -------
     */
    @Test
    public void testNbComponentsSuccessCase2() {
        IGrid grid = new FourConnectedSquareGrid(2, 2);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.nbComponents(2, 2).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(2, solutions.size());
    }

    /**
     * Test case 1: 1x2 4-connected square grid, 0 - 1 CC -> 4 solutions (), (0), (1) and (0, 1).
     *     -------
     *    | 0 | 1 |
     *     -------
     */
    @Test
    public void testNbComponentsSuccessCase3() {
        IGrid grid = new FourConnectedSquareGrid(1, 2);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.nbComponents(0, 1).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(4, solutions.size());
    }


    /**
     * Test case 1: 3x3 4-connected square grid, 6 CC -> Fail.
     *     -----------
     *    | 0 | 1 | 2 |
     *     -----------
     *    | 3 | 4 | 5 |
     *     -----------
     *    | 6 | 7 | 8 |
     *     -----------
     */
    @Test
    public void testNbComponentsFailCase1() {
        IGrid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.nbComponents(6, 6).post();
        UndirectedGraphVar g = reserveModel.getSpatialGraphVar();
        Solver solver = reserveModel.getChocoSolver();
        boolean solution = solver.solve();
        Assert.assertFalse(solution);
    }
}
