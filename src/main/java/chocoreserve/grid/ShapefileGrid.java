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

import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDumper;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Grid loaded from a shapefile.
 */
public class ShapefileGrid extends Grid {

    private String filePath;
    private String[] shapeIds;
    private Map<String, Integer> shapeIdToInternalId;
    private Map<String, Set<String>> neighbors;

    private static final Logger LOGGER = Logger.getLogger(ShapefileGrid.class.getName());

    public ShapefileGrid(String filePath) throws IOException {
        this.filePath = filePath;

        LOGGER.info("Loading shapefile '" + filePath + "'...");

        File shp = new File(filePath);
        Map<String, Object> map = new HashMap<>();
        map.put("url", shp.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

        LOGGER.info("Shapefile successfully loaded (" +  collection.size() + " sites)!");
        LOGGER.info("Computing neighborhood...");

        this.shapeIds = new String[collection.size()];
        this.shapeIdToInternalId = new HashMap<>();
        this.neighbors = new HashMap<>();

        int i = 0;

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                shapeIds[i] = feature.getID();
                shapeIdToInternalId.put(feature.getID(), i);
                neighbors.put(feature.getID(), new HashSet<>());
                MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
                try (FeatureIterator<SimpleFeature> potNeigh = collection.features()) {
                    while (potNeigh.hasNext()) {
                        SimpleFeature f = potNeigh.next();
                        MultiPolygon geom2 = (MultiPolygon) f.getDefaultGeometryProperty().getValue();
                        if (geom.touches(geom2) || geom.overlaps(geom2)) {
                            neighbors.get(feature.getID()).add(f.getID());
                        }
                    }
                }
                i++;
            }
        }

        LOGGER.info("Neighborhood successfully computed!");
    }

    public Map<String, Set<String>> getNeighbors() {
        return neighbors;
    }

    public int getInternalId(String shapeId) {
        return shapeIdToInternalId.get(shapeId);
    }

    public String getShapeId(int internalId) {
        return shapeIds[internalId];
    }

    @Override
    public int getNbCells() {
        return neighbors.size();
    }

    @Override
    public double[][] getCartesianCoordinates() {
        try {
            File shp = new File(filePath);
            Map<String, Object> map = new HashMap<>();
            map.put("url", shp.toURI().toURL());
            DataStore dataStore = null;
            dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];
            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            Filter filter = Filter.INCLUDE;
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
            double[][] coords = new double[getNbCells()][2];
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
                    coords[getInternalId(feature.getID())] = new double[]
                            {geom.getCentroid().getCoordinate().getX(), geom.getCentroid().getCoordinate().getX()};
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void export(String destPath, int[] ids) throws IOException {
        // Load existing
        File shp = new File(filePath);
        Map<String, Object> map = new HashMap<>();
        map.put("url", shp.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter = ff.id(
                Arrays.stream(ids)
                        .mapToObj(i -> ff.featureId(getShapeId(i)))
                        .toArray(FeatureId[]::new)
        );
        SimpleFeatureCollection collection = (SimpleFeatureCollection) source.getFeatures(filter);
        // Export subset
        ShapefileDumper dumper = new ShapefileDumper(new File(destPath));
        dumper.dump(collection);
    }
}
