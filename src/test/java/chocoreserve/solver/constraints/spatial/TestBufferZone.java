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
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test case for buffer zone spatial constraint.
 */
public class TestBufferZone {

    /**
     * Two solutions.
     */
    @Test
    public void testSuccess1() {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 2);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.mandatorySites(0, 1, 2, 3, 4).post();
        reserveModel.bufferZone().post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(2, solutions.size());
    }

    /**
     * Three solutions.
     */
    @Test
    public void testSuccess2() {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        reserveModel.mandatorySites(0, 1, 2, 3, 4, 5).post();
        reserveModel.bufferZone().post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(8, solutions.size());
    }
}
