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

import chocoreserve.solver.IReserveModel;
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

    IReserveModel self();


    // --------------------- //
    // Binary feature makers //
    // --------------------- //

    /**
     * @param name The name of the feature.
     * @param data The data representing the feature.
     * @return A binary feature from an int array.
     */
    default BinaryFeature binaryFeature(String name, int[] data) {
        BinaryFeature f = new BinaryArrayFeature(name, data);
        self().addFeature(f);
        return f;
    }

    /**
     * @param rasterFilePath The path to the raster file representing the feature.
     * @return A binary feature from a raster file. The name of the feature will be the name of the file.
     * @throws IOException
     */
    default BinaryFeature binaryFeature(String rasterFilePath) throws IOException {
        BinaryFeature f = new BinaryRasterFeature(rasterFilePath);
        self().addFeature(f);
        return f;
    }

    /**
     * @param name           The name of the feature.
     * @param rasterFilePath The path to the raster file representing the feature.
     * @return A binary feature from a raster file.
     * @throws IOException
     */
    default BinaryFeature binaryFeature(String name, String rasterFilePath) throws IOException {
        BinaryFeature f = new BinaryRasterFeature(rasterFilePath, name);
        self().addFeature(f);
        return f;
    }

    // --------------------------- //
    // Quantitative feature makers //
    // --------------------------- //

    /**
     * @param name The name of the feature.
     * @param data The data representing the feature.
     * @return A quantitative feature from an int array.
     */
    default QuantitativeFeature quantitativeFeature(String name, int[] data) {
        QuantitativeFeature f = new QuantitativeArrayFeature(name, data);
        self().addFeature(f);
        return f;
    }

    /**
     * @param rasterFilePath The path to the raster file representing the feature.
     * @return A quantitative feature from a raster file. The name of the feature will be the name of the file.
     * @throws IOException
     */
    default QuantitativeFeature quantitativeFeature(String rasterFilePath) throws IOException {
        QuantitativeFeature f = new QuantitativeRasterFeature(rasterFilePath);
        self().addFeature(f);
        return f;
    }

    /**
     * @param name           The name of the feature.
     * @param rasterFilePath The path to the raster file representing the feature.
     * @return A quantitative feature from a raster file.
     * @throws IOException
     */
    default QuantitativeFeature quantitativeFeature(String name, String rasterFilePath) throws IOException {
        QuantitativeFeature f = new QuantitativeRasterFeature(rasterFilePath, name);
        self().addFeature(f);
        return f;
    }

    // ---------------------------- //
    // Probabilistic feature makers //
    // ---------------------------- //

    /**
     * @param name The name of the feature.
     * @param data The data representing the feature.
     * @return A probabilistic feature from an int array.
     */
    default ProbabilisticFeature probabilisticFeature(String name, double[] data) {
        ProbabilisticFeature f = new ProbabilisticArrayFeature(name, data);
        self().addFeature(f);
        return f;
    }

    /**
     * @param rasterFilePath The path to the raster file representing the feature.
     * @return A probabilistic feature from a raster file. The name of the feature will be the name of the file.
     * @throws IOException
     */
    default ProbabilisticFeature probabilisticFeature(String rasterFilePath) throws IOException {
        ProbabilisticFeature f = new ProbabilisticRasterFeature(rasterFilePath);
        self().addFeature(f);
        return f;
    }

    /**
     * @param name           The name of the feature.
     * @param rasterFilePath The path to the raster file representing the feature.
     * @return A probabilistic feature from a raster file.
     * @throws IOException
     */
    default ProbabilisticFeature probabilisticFeature(String name, String rasterFilePath) throws IOException {
        ProbabilisticFeature f = new ProbabilisticRasterFeature(rasterFilePath, name);
        self().addFeature(f);
        return f;
    }
}
