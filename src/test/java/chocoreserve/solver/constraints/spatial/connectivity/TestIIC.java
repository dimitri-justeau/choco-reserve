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

package chocoreserve.solver.constraints.spatial.connectivity;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.connectivity.PropIIC;
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class TestIIC {

    /**
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 0 0 0 0 0 0 0 0 0 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *
     *  EXPECTED IIC = 1/70^2 * (12^2 + 6^2 + 9^2 + 2*(12*6)/2) = 0,067959184 -> rounded 5 => 6796
     */
    @Test
    public void testAdjacencyLB() {
        RegularSquareGrid grid = new RegularSquareGrid(7, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        int[] sitesCore = new int[] {
                0, 1, 2, 3,
                10, 11, 12, 13,
                20, 21, 22, 23,
                40, 41,
                50, 51,
                60, 61,
                46, 47, 48,
                56, 57, 58,
                66, 67, 68
        };
        reserveModel.mandatorySites(core, sitesCore).post();
        reserveModel.sizeRegion(out, grid.getNbCells() - sitesCore.length, grid.getNbCells() - sitesCore.length).post();

        IntVar iic = reserveModel.integralIndexOfConnectivity(core, Neighborhoods.TWO_WIDE_FOUR_CONNECTED, 5);

        Solver solver = reserveModel.getChocoSolver();
        solver.solve();

        Assert.assertEquals(6796, iic.getValue());
    }

    @Test
    public void testFullIIC() {
        RegularSquareGrid grid = new RegularSquareGrid(7, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        int[] sitesCore = IntStream.range(0, 7 * 10).toArray();
        reserveModel.mandatorySites(core, sitesCore).post();

        IntVar iic = reserveModel.integralIndexOfConnectivity(core, Neighborhoods.TWO_WIDE_FOUR_CONNECTED, 2);

        Solver solver = reserveModel.getChocoSolver();
        solver.solve();
        Assert.assertEquals(100, iic.getValue());
    }


    @Test
    public void testZeroIIC() {
        RegularSquareGrid grid = new RegularSquareGrid(7, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        int[] sitesOut = IntStream.range(0, 7 * 10).toArray();
        reserveModel.mandatorySites(out, sitesOut).post();

        IntVar iic = reserveModel.integralIndexOfConnectivity(core, Neighborhoods.TWO_WIDE_FOUR_CONNECTED, 2);

        Solver solver = reserveModel.getChocoSolver();
        solver.solve();

        Assert.assertEquals(0, iic.getValue());
    }

    @Test
    public void testMaximize() throws ContradictionException {
        RegularSquareGrid grid = new RegularSquareGrid(7, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        int[] sitesCore = new int[] {
                0, 1, 2, 3,
                10, 11, 12, 13,
                20, 21, 22, 23,
                40, 41,
                50, 51,
                60, 61,
                46, 47, 48,
                56, 57, 58,
                66, 67, 68
        };
        reserveModel.mandatorySites(core, sitesCore).post();

        IntVar iic = reserveModel.integralIndexOfConnectivity(core, Neighborhoods.TWO_WIDE_FOUR_CONNECTED, 2);

        Solver solver = reserveModel.getChocoSolver();
        Solution s = solver.findOptimalSolution(iic, true);
        s.restore();
        Assert.assertEquals(100, iic.getValue());
    }

    @Test
    public void testMinimize() throws ContradictionException {
        RegularSquareGrid grid = new RegularSquareGrid(7, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        int[] sitesCore = new int[] {
                0, 1, 2, 3,
                10, 11, 12, 13,
                20, 21, 22, 23,
                40, 41,
                50, 51,
                60, 61,
                46, 47, 48,
                56, 57, 58,
                66, 67, 68
        };
        reserveModel.mandatorySites(core, sitesCore).post();

        IntVar iic = reserveModel.integralIndexOfConnectivity(core, Neighborhoods.TWO_WIDE_FOUR_CONNECTED, 5);

        Solver solver = reserveModel.getChocoSolver();
        Solution s = solver.findOptimalSolution(iic, false);
        s.restore();
        Assert.assertEquals(6796, iic.getValue());
    }
}
