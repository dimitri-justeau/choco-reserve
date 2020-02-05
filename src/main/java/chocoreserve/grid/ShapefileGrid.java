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
import org.geotools.util.factory.GeoTools;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Grid loaded from a shapefile.
 */
public class ShapefileGrid extends Grid {

    private String filePath;
    public String[] shapeIds;
    private String idColumn;
    private Map<String, Integer> shapeIdToInternalId;
    private Map<String, Set<String>> neighbors;
    private Map<String, double[]> centroids;

    private static final Logger LOGGER = Logger.getLogger(ShapefileGrid.class.getName());

    public ShapefileGrid(String filePath) throws IOException {
        this(filePath, "");
    }

    public ShapefileGrid(String filePath, String idColumn) throws IOException {
        this.filePath = filePath;
        this.idColumn = idColumn;

        LOGGER.info("Loading shapefile '" + filePath + "'...");

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollection(Filter.INCLUDE);

        LOGGER.info("Shapefile successfully loaded (" +  collection.size() + " sites)!");
        LOGGER.info("Computing neighborhood...");

        this.shapeIds = new String[collection.size()];
        this.shapeIdToInternalId = new HashMap<>();
        this.neighbors = new HashMap<>(); // neighbors identified with shape id.
        this.centroids = new HashMap<>();

        int i = 0;
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                shapeIds[i] = getFeatureId(feature);
                shapeIdToInternalId.put(getFeatureId(feature), i);
                neighbors.put(getFeatureId(feature), new HashSet<>());
                MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
                centroids.put(shapeIds[i], new double[] {geom.getCentroid().getX(), geom.getCentroid().getY()});
                Filter filter = ff.or(
                        ff.touches(ff.property("the_geom"), ff.literal(geom)),
                        ff.overlaps(ff.property("the_geom"), ff.literal(geom))
//                        ff.intersects(ff.property("the_geom"), ff.literal(geom))
                );

                FeatureCollection<SimpleFeatureType, SimpleFeature> neighs = getFeatureCollection(filter);

                try (FeatureIterator<SimpleFeature> neigh = neighs.features()) {
                    while (neigh.hasNext()) {
                        SimpleFeature f = neigh.next();
                        neighbors.get(getFeatureId(feature)).add(getFeatureId(f));
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

    public String getFeatureId(SimpleFeature feature) {
        if (idColumn.equals("")) {
            return feature.getID();
        }
        return feature.getAttribute(idColumn).toString();
    }

    public int getInternalId(String shapeId) {
        return shapeIdToInternalId.get(shapeId);
    }

    public String getShapeId(int internalId) {
        return shapeIds[internalId];
    }

    public FeatureCollection getFeatureCollection(String filePath, Filter filter) throws IOException {
        File shp = new File(filePath);
        Map<String, Object> map = new HashMap<>();
        map.put("url", shp.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        return source.getFeatures(filter);
    }

    public FeatureCollection getFeatureCollection(Filter filter) throws IOException {
        return getFeatureCollection(filePath, filter);
    }

    public int[][] getOverlappingFeatures(String overlappingFilePath) throws IOException {
        int[][] overlapping = new int[getNbCells()][];

        // Assign integer id to overlappingLayer
        FeatureCollection<SimpleFeatureType, SimpleFeature> overlappping = getFeatureCollection(
                overlappingFilePath,
                Filter.INCLUDE
        );
        Map<String, Integer> mapping = new HashMap<>();
        int i = 0;
        try (FeatureIterator<SimpleFeature> feats = overlappping.features()) {
            while (feats.hasNext()) {
                SimpleFeature f = feats.next();
                mapping.put(f.getID(), i);
                i++;
            }
        }
        // Compute overlapping
        i = 0;
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollection(Filter.INCLUDE);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
                Filter filter = ff.intersects(ff.property("the_geom"), ff.literal(geom));
                FeatureCollection<SimpleFeatureType, SimpleFeature> neighs = getFeatureCollection(
                        overlappingFilePath,
                        filter
                );

                Set<Integer> overlap = new HashSet<>();
                try (FeatureIterator<SimpleFeature> neigh = neighs.features()) {
                    while (neigh.hasNext()) {
                        SimpleFeature f = neigh.next();
                        overlap.add(mapping.get(f.getID()));
                    }
                }
                overlapping[i] = overlap.stream().mapToInt(v -> v).toArray();
                i++;
            }
        }
        return overlapping;
    }

    public String[] getShapeIdsByAttribute(String attributeName, Object value) throws IOException {
        File shp = new File(filePath);
        Map<String, Object> map = new HashMap<>();
        map.put("url", shp.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter;
        filter = ff.or(
                Arrays.stream(shapeIds)
                        .map(i -> ff.equals(ff.property(attributeName), ff.literal(value)))
                        .collect(Collectors.toList())
        );
        SimpleFeatureCollection collection = (SimpleFeatureCollection) source.getFeatures(filter);
        String[] ids = new String[collection.size()];
        int i = 0;
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                ids[i] = getFeatureId(feature);
                i++;
            }
        }
        return ids;
    }

    public int[] getInternalIdsByAttribute(String attributeName, Object value) throws IOException {
        return Arrays.stream(getShapeIdsByAttribute(attributeName, value))
                .mapToInt(i -> getInternalId(i))
                .toArray();
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
                    coords[getInternalId(getFeatureId(feature))] = new double[]
                            {geom.getCentroid().getCoordinate().getX(), geom.getCentroid().getCoordinate().getX()};
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public double[] getCartesianCoordinates(int site) {
        return new double[0];
    }

    public String exportDot(double scale) {
        double minX = centroids.values().stream().mapToDouble(i -> i[0]).min().getAsDouble();
        double minY = centroids.values().stream().mapToDouble(i -> i[1]).min().getAsDouble();
        String arc = " -- ";
        StringBuilder sb = new StringBuilder();
        sb.append("graph ").append("{\n");
        sb.append("node [color = black, fontcolor=black];\n{\n");
        for (String shapeId : shapeIds) {
            sb.append("    " + shapeId + " [pos=\"" + (centroids.get(shapeId)[0] - minX) * scale + ", " + (centroids.get(shapeId)[1] - minY) * scale + "!\"];\n");
        }
        sb.append("\n}\n");
        for (String shapeId : shapeIds) {
            for (String neigh : neighbors.get(shapeId)) {
                if (getInternalId(shapeId) > getInternalId(neigh)) {
                    sb.append("  " + shapeId + arc + neigh + ";\n");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void export(String destPath, int[] internalIds) throws IOException {
        String[] shapeIds = Arrays.stream(internalIds).mapToObj(i -> getShapeId(i)).toArray(String[]::new);
        export(destPath, shapeIds);
    }

    public void export(String destPath, String[] shapeIds) throws IOException {
        // Load existing
        File shp = new File(filePath);
        Map<String, Object> map = new HashMap<>();
        map.put("url", shp.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter;
        if (idColumn.equals("")) {
            filter = ff.id(
                    Arrays.stream(shapeIds)
                            .map(i -> ff.featureId(i))
                            .toArray(FeatureId[]::new)
            );
        } else {
            filter = ff.or(
                    Arrays.stream(shapeIds)
                            .map(i -> ff.equals(ff.property(idColumn), ff.literal(i)))
                            .collect(Collectors.toList())
            );
        }
        SimpleFeatureCollection collection = (SimpleFeatureCollection) source.getFeatures(filter);
        // Export subset
        ShapefileDumper dumper = new ShapefileDumper(new File(destPath));
        dumper.dump(collection);
    }
}
