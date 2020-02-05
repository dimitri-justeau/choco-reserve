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

package chocoreserve.util;

import chocoreserve.grid.Grid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

public class Export {

    public static String exportDot(UndirectedGraph g, Grid grid, double scale) {
        String arc = " -- ";
        StringBuilder sb = new StringBuilder();
        sb.append("graph ").append("{\n");
        sb.append("node [color = black, fontcolor=black];\n{\n");
        for (int shapeId : g.getNodes()) {
            sb.append("    " + shapeId + " [pos=\"" + (grid.getCartesianCoordinates(shapeId)[0]) * scale +
                    ", " + (-grid.getCartesianCoordinates(shapeId)[1]) * scale + "!\"];\n");
        }
        sb.append("\n}\n");
        for (int shapeId : g.getNodes()) {
            for (int neigh : g.getNeighOf(shapeId)) {
                if (shapeId > neigh) {
                    sb.append("  " + shapeId + arc + neigh + ";\n");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String exportDotWithRasterCoordinates(String filePath, UndirectedGraph g, RegularSquareGrid grid, double scale) throws IOException, TransformException {

        File file = new File(filePath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        GridGeometry2D gridGeom = gridCov.getGridGeometry();

        double minX = gridGeom.getEnvelope2D().getMinX();
        double minY = gridGeom.getEnvelope2D().getMinY();

        String arc = " -- ";
        StringBuilder sb = new StringBuilder();
        sb.append("graph ").append("{\n");
        sb.append("node [color = black, fontcolor=black];\n{\n");
        for (int shapeId : g.getNodes()) {
            int[] gridCoords = grid.getCoordinatesFromIndex(shapeId);
            double[] coords = gridGeom.gridToWorld(new GridCoordinates2D(gridCoords[1], gridCoords[0])).getCoordinate();

            sb.append("    " + shapeId + " [pos=\"" + (coords[0] - minX) * scale + ", " + (coords[1] - minY) * scale + "!\"];\n");
        }
        sb.append("\n}\n");
        for (int shapeId : g.getNodes()) {
            for (int neigh : g.getNeighOf(shapeId)) {
                if (shapeId > neigh) {
                    sb.append("  " + shapeId + arc + neigh + ";\n");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
