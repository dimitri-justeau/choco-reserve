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

package chocoreserve.solver.feature;

import chocoreserve.solver.feature.array.BinaryArrayFeature;
import chocoreserve.solver.feature.array.ProbabilisticArrayFeature;
import chocoreserve.solver.feature.array.QuantitativeArrayFeature;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for ArrayFeature class.
 */
public class TestArrayFeature {

    @Test
    public void testBinary() {
        BinaryArrayFeature feature = new BinaryArrayFeature("test_binary", new int[] {0, 1, 1, 0, 1, 0, 1, 4});
        Assert.assertEquals(feature.getName(), "test_binary");
        int[] data = feature.getBinaryData();
        for (int d : data) {
            Assert.assertTrue(d == 0 || d == 1);
        }
    }

    @Test
    public void testGetDataProbabilistic() {
        ProbabilisticArrayFeature feature = new ProbabilisticArrayFeature(
                "probTest",
                new double[] {0.1, 0.5, 0, 0.88, 0.5, 0.11}
        );
        Assert.assertEquals(feature.getName(), "probTest");
        double[] data = feature.getProbabilisticData();
        for (double d : data) {
            Assert.assertTrue(d >= 0 && d <= 1);
        }
        ProbabilisticArrayFeature featureFail = new ProbabilisticArrayFeature(
                "probTest",
                new double[] {0.1, 0.5, 0, 1.1, 0.5, 0.11}
        );
        try {
            featureFail.getProbabilisticData();
            Assert.fail();
        } catch (ValueException e) { }
    }

    @Test
    public void testGetDataQuantitative() {
        QuantitativeArrayFeature feature = new QuantitativeArrayFeature("test", new int[] {10, 20, 0, 30, 45});
        feature.getQuantitativeData();
    }
}
