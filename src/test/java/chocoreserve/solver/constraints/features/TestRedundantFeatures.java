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

package chocoreserve.solver.constraints.features;

import chocoreserve.grid.neighborhood.Neighborhood;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.Region;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.BinaryFeature;
import org.chocosolver.solver.Solver;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for RedundantFeatures constraint.
 */
public class TestRedundantFeatures {

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid.
     *     - 1 binary feature present in only 3 planning units (0, 1, 2).
     *     - Must be 3-redundant.
     *
     *     -----------
     *    | X | X | X |
     *     -----------
     *    |   |   |   |
     *     -----------
     *    |   |   |   |
     *     -----------
     */
    @Test
    public void testSuccess1() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhood.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhood.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        BinaryFeature feature = reserveModel.binaryFeature(
                "binary",
                new int[] {1, 1, 1, 0, 0, 0, 0, 0, 0}
        );
        reserveModel.redundantFeatures(core, 3, feature).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                ISet nodes = core.getSetVar().getLB();
                Assert.assertTrue(nodes.contains(0) && nodes.contains(1) && nodes.contains(2));
            } while (solver.solve());
        }
    }

    /**
     * Success test case 2:
     *     - 3x3 4-connected square grid.
     *     - 1 binary feature (A) present in 2 planning units (0, 1).
     *     - 1 binary feature (B) present in 4 planning units (0, 2, 4, 6).
     *     - Both must be 2-redundant.
     *
     *     -----------
     *    | AB | A | B |
     *     ------------
     *    |    | B |   |
     *     ------------
     *    | B  |   |   |
     *     ------------
     */
    @Test
    public void testSuccess2() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhood.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhood.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        BinaryFeature featureA = reserveModel.binaryFeature(
                "A",
                new int[] {1, 1, 0, 0, 0, 0, 0, 0, 0}
        );
        BinaryFeature featureB = reserveModel.binaryFeature(
                "B",
                new int[] {1, 0, 1, 0, 1, 0, 1, 0, 0}
        );
        reserveModel.redundantFeatures(core, 2, featureA, featureB).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                ISet nodes = core.getSetVar().getLB();
                Assert.assertTrue(nodes.contains(0) && nodes.contains(1) &&
                        (nodes.contains(2) || nodes.contains(4) || nodes.contains(6)));
            } while (solver.solve());
        }
    }

    /**
     * Fail test case:
     *     - 3x3 4-connected square grid.
     *     - 1 binary feature present in 2 planning units (0, 1).
     *     - Must be 3-redundant.
     */
    @Test
    public void testFail() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhood.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhood.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        BinaryFeature feature = reserveModel.binaryFeature(
                "binary",
                new int[] {1, 1, 0, 0, 0, 0, 0, 0, 0}
        );
        reserveModel.redundantFeatures(core, 3, feature).post();
        Solver solver = reserveModel.getChocoSolver();
        Assert.assertFalse(solver.solve());
    }
}
