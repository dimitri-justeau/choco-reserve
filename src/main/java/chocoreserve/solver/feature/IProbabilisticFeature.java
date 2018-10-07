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

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import java.io.IOException;

/**
 * Interface describing a probabilistic feature.
 */
public interface IProbabilisticFeature extends IFeature {

    /**
     * @return The data associated with the feature as probabilistic data.
     */
    default double[] getProbabilisticData() throws ValueException, IOException {
        double[] data = getData();
        for (double d : data) {
            if (d > 1) {
                throw new ValueException("There are values strictly greater than 1 in the raster. They cannot be" +
                        " interpreted as probabilistic data");
            }
        }
        return getData();
    }
}
