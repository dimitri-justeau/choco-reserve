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
import chocoreserve.solver.feature.raster.BinaryRasterFeature;
import chocoreserve.solver.feature.raster.ProbabilisticRasterFeature;
import chocoreserve.solver.feature.raster.QuantitativeRasterFeature;

import java.io.IOException;

/**
 * Factory for easy feature instantiation.
 */
public interface IFeatureFactory {

    // --------------------- //
    // Binary feature makers //
    // --------------------- //

    default BinaryFeature binaryFeature(String name, int[] data) {
        return new BinaryArrayFeature(name, data);
    }

    default BinaryFeature binaryFeature(String rasterFilePath) throws IOException {
        return new BinaryRasterFeature(rasterFilePath);
    }

    default BinaryFeature binaryFeature(String name, String rasterFilePath) throws IOException {
        return new BinaryRasterFeature(rasterFilePath, name);
    }

    // --------------------------- //
    // Quantitative feature makers //
    // --------------------------- //

    default QuantitativeFeature quantitativeFeature(String name, int[] data) {
        return new QuantitativeArrayFeature(name, data);
    }

    default QuantitativeFeature quantitativeFeature(String rasterFilePath) throws IOException {
        return new QuantitativeRasterFeature(rasterFilePath);
    }

    default QuantitativeFeature quantitativeFeature(String name, String rasterFilePath) throws IOException {
        return new QuantitativeRasterFeature(rasterFilePath, name);
    }

    // ---------------------------- //
    // Probabilistic feature makers //
    // ---------------------------- //

    default ProbabilisticFeature probabilisticFeature(String name, double[] data) {
        return new ProbabilisticArrayFeature(name, data);
    }

    default ProbabilisticFeature probabilisticFeature(String rasterFilePath) throws IOException {
        return new ProbabilisticRasterFeature(rasterFilePath);
    }

    default ProbabilisticFeature probabilisticFeature(String name, String rasterFilePath) throws IOException {
        return new ProbabilisticRasterFeature(rasterFilePath, name);
    }
}
