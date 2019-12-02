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

package chocoreserve.vector;

import chocoreserve.grid.ShapefileGrid;
import chocoreserve.solver.feature.BinaryFeature;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 *
 */
public class TestShapefileFeatureReader {

    @Test
    public void testShapefileFeatureReader() throws IOException {
        String gridPath = getClass().getClassLoader().getResource("vector/BV_RGNC/BV_RGNC.shp").getPath();
        String occPath = getClass().getClassLoader().getResource("vector/testOcc/testOcc.shp").getPath();
        ShapefileGrid grid = new ShapefileGrid(gridPath);
        Map<String, BinaryFeature> fs = ShapefileFeatureReader.binaryFeaturesFromShapefile(occPath, "occType", grid);
        Assert.assertTrue(Arrays.equals(fs.get("species_a").getBinaryData(), new int[] {
                0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        }));
        Assert.assertTrue(Arrays.equals(fs.get("species_b").getBinaryData(), new int[] {
                1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        }));
        Assert.assertTrue(Arrays.equals(fs.get("species_c").getBinaryData(), new int[] {
                0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        }));
        Assert.assertTrue(Arrays.equals(fs.get("species_d").getBinaryData(), new int[] {
                0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0
        }));
        Assert.assertTrue(Arrays.equals(fs.get("species_e").getBinaryData(), new int[] {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        }));
        Assert.assertTrue(Arrays.equals(fs.get("species_f").getBinaryData(), new int[] {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        }));
    }

}
