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

package chocoreserve.solver.search;

import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.BinaryFeature;
import org.chocosolver.solver.Solver;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for ReserveSearchFactory class
 */
public class TestReserveSearchFactory {


    @Test
    public void testDiscardPoor() {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        BinaryFeature featureA = reserveModel.binaryFeature(
                "A",
                new int[] {1, 1, 0, 0, 0, 0, 0, 0, 0}
        );
        BinaryFeature featureB = reserveModel.binaryFeature(
                "B",
                new int[] {1, 0, 1, 0, 1, 0, 1, 0, 0}
        );
        reserveModel.redundantFeatures(2, featureA, featureB).post();
        Solver solver = reserveModel.getChocoSolver();
        solver.setSearch(ReserveSearchFactory.discardPoor(reserveModel, 0));
        Assert.assertTrue(solver.solve());
        reserveModel.printSolution(false);
    }

    @Test
    public void testDiscardPoorDeterministic() {
        RegularSquareGrid grid = new FourConnectedSquareGrid(3, 3);
        ReserveModel reserveModel = new ReserveModel(grid);
        BinaryFeature featureA = reserveModel.binaryFeature(
                "A",
                new int[] {1, 1, 0, 0, 0, 0, 0, 0, 0}
        );
        BinaryFeature featureB = reserveModel.binaryFeature(
                "B",
                new int[] {1, 0, 1, 0, 1, 0, 1, 0, 0}
        );
        reserveModel.redundantFeatures(2, featureA, featureB).post();
        Solver solver = reserveModel.getChocoSolver();
        solver.setSearch(ReserveSearchFactory.discardPoorDeterministic(reserveModel, 0));
        Assert.assertTrue(solver.solve());
        reserveModel.printSolution(true);
    }
}
