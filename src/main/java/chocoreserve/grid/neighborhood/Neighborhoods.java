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

package chocoreserve.grid.neighborhood;

import chocoreserve.grid.neighborhood.regulare.square.FourConnected;
import chocoreserve.grid.neighborhood.regulare.square.HeightConnected;
import chocoreserve.grid.neighborhood.regulare.square.TwoWideHeightConnected;

/**
 * Utility class for accessing neighborhoods.
 */
public class Neighborhoods {

    public final static FourConnected FOUR_CONNECTED = new FourConnected();
    public final static HeightConnected HEIGHT_CONNECTED = new HeightConnected();
    public final static TwoWideHeightConnected TWO_WIDE_HEIGHT_CONNECTED = new TwoWideHeightConnected();
    public final static ShapefileNeighborhood SHAPEFILE_NEIGHBORHOOD = new ShapefileNeighborhood();

}
