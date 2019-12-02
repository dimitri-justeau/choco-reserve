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

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.region.Region;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.ProbabilisticFeature;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Test case for MinProbability constraint.
 */
public class TestMinProbability {

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid.
     *     - 1 probabilistic feature.
     *     - Must have a minimum probability of presence of 0.8.
     *
     *     -----------------
     *    | 0.1 | 0.2 | 0.1 |
     *     -----------------
     *    | 0.7 | 0.1 | 0.1 |
     *     -----------------
     *    | 0.3 | 0.3 | 0.1 |
     *     -----------------
     */
    @Test
    public void testSuccess1() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        double[] data = new double[] {0.1, 0.2, 0.1, 0.7, 0.1, 0.1, 0.3, 0.3, 0.1};
        ProbabilisticFeature feature = reserveModel.probabilisticFeature("probabilistic", data);
        reserveModel.minProbability(core, 0.8, feature).post();
        Solver solver = reserveModel.getChocoSolver();
        solver.setSearch(Search.inputOrderLBSearch(reserveModel.getSites()));
        int nbSol = 0;
        if (solver.solve()) {
            do {
                nbSol ++;
                try {
                    int[] nodes = core.getSetVar().getLB().toArray();
                    double prob = IntStream.of(nodes).mapToDouble(i -> 1 - data[i]).reduce(1, (a, b) -> a * b);
                    Assert.assertTrue(prob <= 0.2);
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            } while (solver.solve());
        } else {
            Assert.fail();
        }
        // Now assert that the solver found every solution.
        Region unconstrainedCore = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region unconstrainedOut = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel unconstrainedReserveModel = new ReserveModel(grid, unconstrainedCore, unconstrainedOut);
        Solver solver1 = unconstrainedReserveModel.getChocoSolver();
        solver.setSearch(Search.inputOrderLBSearch(reserveModel.getSites()));
        int nbNotSol = 0;
        if (solver1.solve()) {
            do {
                try {
                    int[] nodes = unconstrainedCore.getSetVar().getLB().toArray();
                    double prob = IntStream.of(nodes).mapToDouble(i -> 1 - data[i]).reduce(1, (a, b) -> a * b);
                    if (prob > 0.2) {
                        nbNotSol ++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            } while (solver1.solve());
        }
        Assert.assertEquals((int) Math.pow(2, 3*3), nbNotSol + nbSol);
    }

    /**
     * Success test case 1:
     *     - 3x3 4-connected square grid.
     *     - 2 probabilistic feature (A and B).
     *     - Must have a minimum probability of presence of 0.8.
     *
     *     -----------------     -----------------------
     *    | 0.1 | 0.2 | 0.1 |   | 0.7  | 0.201  | 0.051 |
     *     -----------------     -----------------------
     *    | 0.7 | 0.1 | 0.1 |   | 0.5  |  0.1   | 0.01  |
     *     -----------------     -----------------------
     *    | 0.3 | 0.3 | 0.1 |   | 0.25 | 0.333  | 0.21  |
     *     -----------------     -----------------------
     */
    @Test
    public void testSuccess2() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        double[] dataA = new double[] {0.1, 0.2, 0.1, 0.7, 0.1, 0.1, 0.3, 0.3, 0.1};
        ProbabilisticFeature featureA = reserveModel.probabilisticFeature("A", dataA);
        double[] dataB = new double[] {0.7, 0.201, 0.051, 0.5, 0.1, 0.01, 0.25, 0.333, 0.21};
        ProbabilisticFeature featureB = reserveModel.probabilisticFeature("B", dataB);
        reserveModel.minProbability(core, 0.8, featureA, featureB).post();
        Solver solver = reserveModel.getChocoSolver();
        if (solver.solve()) {
            do {
                try {
                    int[] nodes = core.getSetVar().getLB().toArray();
                    double probA = IntStream.of(nodes).mapToDouble(i -> 1 - dataA[i]).reduce(1, (a, b) -> a * b);
                    double probB = IntStream.of(nodes).mapToDouble(i -> 1 - dataB[i]).reduce(1, (a, b) -> a * b);
                    Assert.assertTrue(probA <= 0.2 && probB <= 0.2);
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
     * Fail test case 1:
     *     - 3x3 4-connected square grid.
     *     - 1 probabilistic feature.
     *     - Must have a minimum probability of presence of 0.99.
     *     Probability with every planning selected is ~= 0.93.
     *
     *     -----------------
     *    | 0.1 | 0.2 | 0.1 |
     *     -----------------
     *    | 0.7 | 0.1 | 0.1 |
     *     -----------------
     *    | 0.3 | 0.3 | 0.1 |
     *     -----------------
     */
    @Test
    public void testFail() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, core, out);
        double[] data = new double[] {0.1, 0.2, 0.1, 0.7, 0.1, 0.1, 0.3, 0.3, 0.1};
        ProbabilisticFeature feature = reserveModel.probabilisticFeature("probabilistic", data);
        reserveModel.minProbability(core, 0.99, feature).post();
        Solver solver = reserveModel.getChocoSolver();
        Assert.assertFalse(solver.solve());
    }
}
