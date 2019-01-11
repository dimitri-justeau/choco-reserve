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

package chocoreserve.solver.constraints.spatial;

import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test class for the maxDiameter constraint.
 */
public class TestMaxDiameter {

    /**
     * Only solutions (except the empty reserve).
     */
    @Test
    public void testSuccess1() {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 2);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.maxDiameter(reserveModel.getCore(), 3).post();
        reserveModel.getChocoModel().arithm(reserveModel.getNbSitesBuffer(), "=", 0).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals((int)(Math.pow(2, 6) - 1), solutions.size());
    }

    /**
     * Only 1 solution.
     */
    @Test
    public void testSuccess2() {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 2);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.mandatorySites(reserveModel.getCore(), 0).post();
        reserveModel.getChocoModel().arithm(reserveModel.getNbSitesBuffer(), "=", 0).post();
        reserveModel.maxDiameter(reserveModel.getCore(), 0.5).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(1, solutions.size());
    }

    /**
     * No solution.
     */
    @Test
    public void testFail1() throws ContradictionException {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 2);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.maxDiameter(reserveModel.getCore(), 1).post();
        reserveModel.getChocoModel().arithm(reserveModel.getNbSitesBuffer(), "=", 0).post();
        reserveModel.mandatorySites(reserveModel.getCore(), 0, 5).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            reserveModel.printSolution(false);
        }
    }
}
