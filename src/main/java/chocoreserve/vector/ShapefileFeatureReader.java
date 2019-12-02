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
import chocoreserve.solver.feature.QuantitativeFeature;
import chocoreserve.solver.feature.array.BinaryArrayFeature;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

/**
 *
 */
public class ShapefileFeatureReader {

    public static Map<String, BinaryFeature> binaryFeaturesFromShapefile(String path, String featureNameColumn,
                                                                         ShapefileGrid grid) throws IOException {
        File shp = new File(path);
        Map<String, Object> map = new HashMap<>();
        map.put("url", shp.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Filter filter = Filter.INCLUDE;
        FeatureCollection<SimpleFeatureType, SimpleFeature> pointCollection = source.getFeatures(filter);

        Map<String, Set<Integer>> binaryFeatures = new HashMap<>();

        try (FeatureIterator<SimpleFeature> points = pointCollection.features()) {
            while (points.hasNext()) {
                SimpleFeature pointFeature = points.next();
                Point geom = (Point) pointFeature.getDefaultGeometry();
                Filter f = ff.contains(ff.property("the_geom"), ff.literal(geom));
                FeatureCollection<SimpleFeatureType, SimpleFeature> sitesCollection = grid.getFeatureCollection(f);
                assert sitesCollection.size() <= 1;
                if (sitesCollection.size() == 1) {
                    try (FeatureIterator<SimpleFeature> iterator = sitesCollection.features()) {
                        String shapeId = grid.getFeatureId(iterator.next());
                        int internalId = grid.getInternalId(shapeId);
                        String featName = pointFeature.getAttribute(featureNameColumn).toString();
                        if (!binaryFeatures.keySet().contains(featName)) {
                            binaryFeatures.put(featName, new HashSet<>());
                        }
                        binaryFeatures.get(featName).add(internalId);
                    }
                }
            }
        }
        Map<String, BinaryFeature> result = new HashMap<>();
        for (String key : binaryFeatures.keySet()) {
            int[] array = IntStream.range(0, grid.getNbCells())
                    .map(i -> binaryFeatures.get(key).contains(i) ? 1 : 0)
                    .toArray();
            result.put(key, new BinaryArrayFeature(key, array));
        }
        return result;
    }

//    public static Map<String, QuantitativeFeature> quantitativeFeatureFromShapefile(String path, String featureNameColumn,
//                                                                                    ShapefileGrid grid) throws IOException {
//        File shp = new File(path);
//        Map<String, Object> map = new HashMap<>();
//        map.put("url", shp.toURI().toURL());
//        DataStore dataStore = DataStoreFinder.getDataStore(map);
//        String typeName = dataStore.getTypeNames()[0];
//        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
//        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
//        Filter filter = Filter.INCLUDE;
//        FeatureCollection<SimpleFeatureType, SimpleFeature> pointCollection = source.getFeatures(filter);
//
//        Map<String, Set<Integer>> binaryFeatures = new HashMap<>();
//
//        try (FeatureIterator<SimpleFeature> points = pointCollection.features()) {
//            while (points.hasNext()) {
//                SimpleFeature pointFeature = points.next();
//                Point geom = (Point) pointFeature.getDefaultGeometry();
//                Filter f = ff.contains(ff.property("the_geom"), ff.literal(geom));
//                FeatureCollection<SimpleFeatureType, SimpleFeature> sitesCollection = grid.getFeatureCollection(f);
//                assert sitesCollection.size() <= 1;
//                if (sitesCollection.size() == 1) {
//                    try (FeatureIterator<SimpleFeature> iterator = sitesCollection.features()) {
//                        String shapeId = iterator.next().getID();
//                        int internalId = grid.getInternalId(shapeId);
//                        String featName = pointFeature.getAttribute(featureNameColumn).toString();
//                        if (!binaryFeatures.keySet().contains(featName)) {
//                            binaryFeatures.put(featName, new HashSet<>());
//                        }
//                        binaryFeatures.get(featName).add(internalId);
//                    }
//                }
//            }
//        }
//        Map<String, BinaryFeature> result = new HashMap<>();
//        for (String key : binaryFeatures.keySet()) {
//            int[] array = IntStream.range(0, grid.getNbCells())
//                    .map(i -> binaryFeatures.get(key).contains(i) ? 1 : 0)
//                    .toArray();
//            result.put(key, new BinaryArrayFeature(key, array));
//        }
//        return result;
//    }
}
