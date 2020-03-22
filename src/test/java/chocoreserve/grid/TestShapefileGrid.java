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

package chocoreserve.grid;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestShapefileGrid {

    @Test
    public void testReadShapeAndConnectivity() throws IOException {
        String path = getClass().getClassLoader().getResource("vector/BV_RGNC/BV_RGNC.shp").getPath();
        ShapefileGrid grid = new ShapefileGrid(path);
        int[][] expectedNeighborhoods = new int[][] {
                {2, 3},
                {14, 15, 29, 2},
                {1, 29, 0},
                {0, 6, 24, 23},
                {26, 27, 28, 8},
                {6, 11, 26, 9},
                {3, 24, 25, 5, 26, 21, 22, 11},
                {12, 13},
                {4, 13},
                {10, 5, 26},
                {26, 9},
                {6, 5},
                {7},
                {8, 7},
                {1, 15, 20, 19, 18},
                {14, 1},
                {17},
                {18, 16},
                {17, 19, 14},
                {18, 20, 14},
                {14, 19},
                {22, 6},
                {25, 6, 21},
                {3, 24},
                {25, 6, 3, 23},
                {22, 6, 24},
                {5, 6, 9, 10, 27, 4},
                {26, 28, 4},
                {27, 4},
                {2, 1},
        };
        for (int i = 0; i < grid.getNbCells(); i++) {
            int[] expected = expectedNeighborhoods[i];
            int[] computed = Neighborhoods.SHAPEFILE_NEIGHBORHOOD.getNeighbors(grid, i).toArray();
            Arrays.sort(expected);
            Arrays.sort(computed);
            if (!Arrays.equals(expected, computed)) {
                System.out.println(i + " / " + Arrays.toString(expected) + " // " + Arrays.toString(computed));
            }
            Assert.assertTrue(Arrays.equals(expected, computed));
        }
    }

//    @Test
//    public void testBasicShapefileProblem() throws IOException {
//        String path = getClass().getClassLoader().getResource("vector/BV_UnitesGestionProvinceSud_COTE_OUBLIEE/SousUnitesGestion.shp").getPath();
//        ShapefileGrid grid = new ShapefileGrid(path);
//        Region region1 = new Region("region1", Neighborhoods.SHAPEFILE_NEIGHBORHOOD);
//        Region region2 = new Region("region2", Neighborhoods.SHAPEFILE_NEIGHBORHOOD);
//        Region region3 = new Region("region3", Neighborhoods.SHAPEFILE_NEIGHBORHOOD);
//        ReserveModel reserveModel = new ReserveModel(grid, region1, region2, region3);
//        Model model = reserveModel.getChocoModel();
//        reserveModel.nbConnectedComponents(region1, 1, 1).post();
//        reserveModel.nbConnectedComponents(region2, 1, 1).post();
//        reserveModel.nbConnectedComponents(region3, 1, 1).post();
//        reserveModel.bufferZone(Neighborhoods.SHAPEFILE_NEIGHBORHOOD, region1, region2, region3).post();
//        reserveModel.sizeRegion(region1, 10, 2000).post();
//        reserveModel.sizeRegion(region2, 10, 2000).post();
//        reserveModel.sizeConnectedComponents(region1, model.intVar(40, 2000), model.intVar(40, 2000)).post();
//        reserveModel.sizeConnectedComponents(region2, model.intVar(40, 2000), model.intVar(40, 2000)).post();
//        reserveModel.sizeRegion(region3, 5, 2000).post();
//        Solver solver = model.getSolver();
//        solver.showStatistics();
//        if (solver.solve()) {
//            System.out.println(region1.getSetVar().getValue());
//            System.out.println(region2.getSetVar().getValue());
//            System.out.println(region3.getSetVar().getValue());
//            grid.export("/home/djusteau/testRegion/testRegion1", region1.getSetVar().getValue().toArray());
//            grid.export("/home/djusteau/testRegion/testRegion2", region2.getSetVar().getValue().toArray());
//            grid.export("/home/djusteau/testRegion/testRegion3", region3.getSetVar().getValue().toArray());
//        }
//    }
}
