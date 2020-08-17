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

package chocoreserve.util.fragmentation;

import chocoreserve.util.ConnectivityFinderSpatialGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;

/**
 * Utility class to compute fragmentation indices on static objects.
 * e.g. to get the initial value of a landscape before solving.
 */
public class FragmentationIndices {

    public static double effectiveMeshSize(UndirectedGraph g, int landscapeArea) {
        ConnectivityFinderSpatialGraph connFinder = new ConnectivityFinderSpatialGraph(g);
        connFinder.findAllCC();
        double mesh = 0;
        for (int i = 0; i < connFinder.getNBCC(); i++) {
            int s = connFinder.getSizeCC()[i];
            mesh += s * s;
        }
        mesh /= 1.0 * landscapeArea;
        return mesh;
    }

    public static double aggregationIndex(UndirectedGraph g) {
        int nbEdges = 0;
        for (int i : g.getNodes()) {
            nbEdges += g.getNeighOf(i).size();
        }
        nbEdges /= 2;
        int nbNodes = g.getNodes().size();
        return aggregationIndex(nbNodes, nbEdges);
    }

    public static double aggregationIndex(int nbNodes, int nbEdges) {
        int n = (int) Math.floor(Math.sqrt(nbNodes));
        int m = nbNodes - n * n;
        int maxGi;
        if (m == 0) {
            maxGi = 2 * n * (n - 1);
        } else {
            if (m <= m) {
                maxGi = 2 * n * (n - 1) + 2 * m - 1;
            } else {
                maxGi = 2 * n * (n - 1) + 2 * m - 2;
            }
        }
        return  (1.0 * nbEdges / maxGi);
    }

    public static int getNbEdges(UndirectedGraph graph) {
        int nbE = 0;
        for (int i : graph.getNodes()) {
            nbE += graph.getNeighOf(i).size();
        }
        nbE /= 2;
        return nbE;
    }
}
