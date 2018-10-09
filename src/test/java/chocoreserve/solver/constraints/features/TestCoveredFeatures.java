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

import chocoreserve.exception.ModelNotInstantiatedError;
import chocoreserve.grid.Grid;
import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.IBinaryFeature;
import chocoreserve.solver.feature.IQuantitativeFeature;
import chocoreserve.solver.feature.array.BinaryArrayFeature;
import chocoreserve.solver.feature.array.QuantitativeArrayFeature;
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
     *     - 3x3 4-connected square grid
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
        Grid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        IBinaryFeature feature = new BinaryArrayFeature(
                "test_binary",
                new int[] {0, 0, 0, 1, 0, 0, 0, 0, 0 }
        );
        reserveModel.coveredFeatures(feature).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                try {
                    ISet nodes = reserveModel.getSelectedPlanningUnitsAsSet();
                    Assert.assertTrue(nodes.contains(3));
                } catch (ModelNotInstantiatedError modelNotInstantiatedError) {
                    modelNotInstantiatedError.printStackTrace();
                    Assert.fail();
                }
            } while (solver.solve());
        } else {
            Assert.fail();
        }
    }

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid
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
        Grid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        IBinaryFeature featureA = new BinaryArrayFeature(
                "A",
                new int[] {0, 0, 0, 0, 0, 1, 0, 0, 0}
        );
        IQuantitativeFeature featureB = new QuantitativeArrayFeature(
                "B",
                new int[] {12, 0, 5, 0, 0, 0, 0, 0, 3}
        );
        reserveModel.coveredFeatures(featureA, featureB).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                try {
                    ISet nodes = reserveModel.getSelectedPlanningUnitsAsSet();
                    Assert.assertTrue(nodes.contains(5));
                    Assert.assertTrue(nodes.contains(0) || nodes.contains(2) || nodes.contains(8));
                } catch (ModelNotInstantiatedError modelNotInstantiatedError) {
                    modelNotInstantiatedError.printStackTrace();
                    Assert.fail();
                }
            } while (solver.solve());
        } else {
            Assert.fail();
        }
    }

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid
     *     - 1 binary feature present in no planning unit.
     */
    @Test
    public void testFail() {
        Grid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        IBinaryFeature feature = new BinaryArrayFeature(
                "A",
                new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0}
        );
        reserveModel.coveredFeatures(feature).post();
        Solver solver = reserveModel.getChocoSolver();
        Assert.assertFalse(solver.solve());
    }
}
