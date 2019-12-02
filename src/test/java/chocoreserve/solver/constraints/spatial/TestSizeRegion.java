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

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.region.Region;
import chocoreserve.solver.ReserveModel;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Test for AreaReserveSystem constraint.
 */
public class TestSizeRegion {

    /**
     * Success test case 1: 3x3 4-connected square grid, areaMin = areaMax = 9 -> 1 solution.
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
    public void testSizeRegionSuccess1() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        reserveModel.sizeRegion(core, 9, 9).post();
        Solver solver = reserveModel.getChocoSolver();
        List<Solution> solutions = solver.findAllSolutions();
        Assert.assertEquals(1, solutions.size());
        try {
            solutions.get(0).restore();
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
        int[] nodes = core.getSetVar().getLB().toArray();
        Arrays.sort(nodes);
        Assert.assertTrue(Arrays.equals(IntStream.range(0, 9).toArray(), nodes));
    }

    /**
     * Success test case 1: 3x3 4-connected square grid, areaMin = 2, areaMax = 4.
     * Many solutions, we just test that they all satisfy the constraint.
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
    public void testSizeRegionSuccess2() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        reserveModel.sizeRegion(core, 2, 4).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                try {
                    int n = core.getSetVar().getLB().size();
                    Assert.assertTrue(n >= 2 && n <= 4);
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            } while (solver.solve());
        } else {
            Assert.fail();
        }
    }

    /**
     * Fail test case 1: 3x3 4-connected square grid, areaMin = 10, areaMax = 20.
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
    public void testSizeRegionFail() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        reserveModel.sizeRegion(core, 10, 20).post();
        Solver solver = reserveModel.getChocoSolver();
        Assert.assertFalse(solver.solve());
    }
}
