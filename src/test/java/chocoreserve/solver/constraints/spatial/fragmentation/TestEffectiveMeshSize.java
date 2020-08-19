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

public class TestEffectiveMeshSize {

    /**
     * All the landscape is covered by core region. MESH = landscapeArea.
     */
    @Test
    public void testMaxValue() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        reserveModel.mandatorySites(core, IntStream.range(0, 25).toArray()).post();
        IntVar MESH = reserveModel.effectiveMeshSize(core, 0);
        Solver solver = reserveModel.getChocoSolver();
        solver.solve();
        Assert.assertEquals(25, MESH.getValue());
    }

    /**
     * Core region is empty, MESH = 0.
     */
    @Test
    public void testMinValue() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        reserveModel.mandatorySites(out, IntStream.range(0, 25).toArray()).post();
        IntVar MESH = reserveModel.effectiveMeshSize(core, 0);
        Solver solver = reserveModel.getChocoSolver();
        solver.solve();
        Assert.assertEquals(0, MESH.getValue());
    }

    /**
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 1 1 1 1 0 0 0 0 0 0
     *  | 0 0 0 0 0 0 0 0 0 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  | 1 1 0 0 0 0 1 1 1 0
     *  | 1 1 0 0 0 0 1 1 1 0
     */
    @Test
    public void testSeveralPatches() {
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
        IntVar MESH = reserveModel.effectiveMeshSize(core, 2);
        Solver solver = reserveModel.getChocoSolver();
        solver.solve();
        Assert.assertEquals(Math.round(100.0 * (12 * 12 + 6 * 6 + 9 * 9) / grid.getNbCells()), MESH.getValue());
    }

    /**
     *  | 1 1 1 1
     *  | 1 - - 1
     *  | 1 - - 1
     *  | - - - -
     *  Min = 4
     *  Max = 16
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
        IntVar MESH = reserveModel.effectiveMeshSize(core, 0);
        Solver solver = reserveModel.getChocoSolver();
        // Minimize
        Solution sol = solver.findOptimalSolution(MESH, false);
        sol.restore();
        reserveModel.printSolution();
        Assert.assertEquals(4, MESH.getValue());
        // Maximize
        solver.reset();
        sol = solver.findOptimalSolution(MESH, true);
        sol.restore();
        reserveModel.printSolution();
        Assert.assertEquals(16, MESH.getValue());
    }
}
