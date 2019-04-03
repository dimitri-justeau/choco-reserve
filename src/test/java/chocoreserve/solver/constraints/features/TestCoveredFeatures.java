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
import chocoreserve.solver.region.Region;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.BinaryFeature;
import chocoreserve.solver.feature.QuantitativeFeature;
import org.chocosolver.solver.Solver;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for CoveredFeatures constraint.
 */
public class TestCoveredFeatures {

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid.
     *     - 1 binary feature present in only one planning unit (3).
     *
     *     -----------
     *    |   |   |   |
     *     -----------
     *    | X |   |   |
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
                "test_binary",
                new int[] {0, 0, 0, 1, 0, 0, 0, 0, 0 }
        );
        reserveModel.coveredFeatures(core, feature).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                ISet nodes = core.getSetVar().getLB();
                Assert.assertTrue(nodes.contains(3));
            } while (solver.solve());
        } else {
            Assert.fail();
        }
    }

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid.
     *     - 1 binary feature (A) present in only one planning unit (5).
     *     - 1 quantitative feature (B) present in three planning units (0, 8, 2).
     *
     *     -----------
     *    | B |   | B |
     *     -----------
     *    |   |   | A |
     *     -----------
     *    |   |   | B |
     *     -----------
     */
    @Test
    public void testSuccess2() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhood.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhood.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        BinaryFeature featureA = reserveModel.binaryFeature(
                "A",
                new int[] {0, 0, 0, 0, 0, 1, 0, 0, 0}
        );
        QuantitativeFeature featureB = reserveModel.quantitativeFeature(
                "B",
                new int[] {12, 0, 5, 0, 0, 0, 0, 0, 3}
        );
        reserveModel.coveredFeatures(core, featureA, featureB).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                ISet nodes = core.getSetVar().getLB();
                Assert.assertTrue(nodes.contains(5));
                Assert.assertTrue(nodes.contains(0) || nodes.contains(2) || nodes.contains(8));
            } while (solver.solve());
        } else {
            Assert.fail();
        }
    }

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid.
     *     - 1 binary feature present in no planning unit.
     */
    @Test
    public void testFail() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhood.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhood.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        BinaryFeature feature = reserveModel.binaryFeature(
                "A",
                new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0}
        );
        reserveModel.coveredFeatures(core, feature).post();
        Solver solver = reserveModel.getChocoSolver();
        Assert.assertFalse(solver.solve());
    }
}
