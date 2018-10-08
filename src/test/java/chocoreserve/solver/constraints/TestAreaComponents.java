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

import chocoreserve.grid.Grid;
import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.solver.ReserveModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test for AreaComponents constraint.
 */
public class TestAreaComponents {

    /**
     * Success test case 1: 3x3 4-connected square grid, 2 CC of size 3 -> 4 solutions :
     *
     *  (0, 1, 2, 6, 7, 8) - horizontal split
     *  (0, 1, 3, 5, 7, 8) - diagonal split 1
     *  (0, 2, 3, 5, 6, 8) - vertical split
     *  (1, 2, 3, 5, 6, 7) - diagonal split 2
     *
     *     -----------
     *    | 0 | 1 | 2 |
     *     -----------
     *    | 3 | 4 | 5 |
     *     -----------
     *    | 6 | 7 | 8 |
     *     -----------
     */
    @Test
    public void testAreaComponentsSuccess1() {
        Grid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.nbComponents(2, 2).post();
        IntVar minNCC = reserveModel.getChocoModel().intVar(3, 4);
        IntVar maxNCC = reserveModel.getChocoModel().intVar(3, 4);
        reserveModel.areaComponents(minNCC, maxNCC).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(4, solutions.size());
    }

    /**
     * Success est case 2: 1x4 4-connected square grid, 1 CC of size [3, 4] -> 3 solutions :
     *
     *  (0, 1, 2)
     *  (1, 2, 3)
     *  (0, 1, 2, 3)
     *
     *     ---------------
     *    | 0 | 1 | 2 | 3 |
     *     ---------------
     */
    @Test
    public void testAreaComponentsSuccess2() {
        Grid grid = new FourConnectedSquareGrid(1, 4);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.nbComponents(1, 1).post();
        IntVar minNCC = reserveModel.getChocoModel().intVar("minNCC", 3, 4);
        IntVar maxNCC = reserveModel.getChocoModel().intVar("maxNCC", 3, 4);
        reserveModel.areaComponents(minNCC, maxNCC).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(3, solutions.size());
    }
}
