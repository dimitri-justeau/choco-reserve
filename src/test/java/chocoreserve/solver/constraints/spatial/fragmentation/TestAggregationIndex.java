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

package chocoreserve.solver.constraints.spatial.fragmentation;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

public class TestAggregationIndex {

    /**
     * All the landscape is covered by core region. AI must be equal to precision (here 100).
     */
    @Test
    public void testMaxValue() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        reserveModel.mandatorySites(core, IntStream.range(0, 25).toArray()).post();
        IntVar AI = reserveModel.aggregationIndex(core, 2);
        Solver solver = reserveModel.getChocoSolver();
        solver.solve();
        Assert.assertEquals(100, AI.getValue());
    }

    /**
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 0 0 0 0 0 0 0 0 0 0
     *  | 0 0 0 0 0 0 0 0 0 0
     *  | 0 0 0 0 0 0 0 0 0 0
     *  AI = 100
     */
    @Test
    public void test0() {
        RegularSquareGrid grid = new RegularSquareGrid(7, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        int[] sitesCore = new int[] {
                0, 1, 2, 3,
                10, 11, 12, 13,
                20, 21, 22, 23,
                30, 31, 32, 33
        };
        reserveModel.mandatorySites(core, sitesCore).post();
        reserveModel.sizeRegion(out, grid.getNbCells() - sitesCore.length, grid.getNbCells() - sitesCore.length).post();
        IntVar AI = reserveModel.aggregationIndex(core, 2);
        Solver solver = reserveModel.getChocoSolver();
        solver.showStatistics();
        solver.solve();
        Assert.assertEquals(100, AI.getValue());
    }

    /**
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 0 0 0 0 0 0 0 0 0 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  total edges = 36
     *  total nodes = 27
     *  n = 5
     *  m = 2
     *  max_gii = 43
     *  AI = 83.7
     */
    @Test
    public void test1() {
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
        IntVar AI = reserveModel.aggregationIndex(core, 3);
        Solver solver = reserveModel.getChocoSolver();
        solver.showStatistics();
        solver.solve();
        Assert.assertEquals(837, AI.getValue());
    }

    /**
     *  | 1 1 1 1
     *  | 1 - - 1
     *  | 1 - - 1
     *  | - - - -
     *  Min = 58.3
     *  Max = 100
     */
    @Test
    public void testOptimization() throws ContradictionException {
        RegularSquareGrid grid = new RegularSquareGrid(4, 4);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, out, core);
        int[] sitesCore = new int[] {
                0,1,2,3,4,7,8,11
        };
        reserveModel.mandatorySites(core, sitesCore).post();
        IntVar AI = reserveModel.aggregationIndex(core, 3);
        Solver solver = reserveModel.getChocoSolver();
        // Minimize
        Solution sol = solver.findOptimalSolution(AI, false);
        sol.restore();
        reserveModel.printSolution();
        Assert.assertEquals(583, AI.getValue());
        // Maximize
        solver.reset();
        sol = solver.findOptimalSolution(AI, true);
        sol.restore();
        reserveModel.printSolution();
        Assert.assertEquals(1000, AI.getValue());
    }
}
