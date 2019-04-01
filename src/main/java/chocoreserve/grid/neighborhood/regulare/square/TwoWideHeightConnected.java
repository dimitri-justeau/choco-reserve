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

package chocoreserve.grid.neighborhood.regulare.square;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.Neighborhood;
import chocoreserve.grid.regular.square.RegularSquareGrid;

import java.util.HashSet;
import java.util.Set;

/**
 * The 2-wide height-connected neighborhood in a regular square grid.
 */
public class TwoWideHeightConnected<T extends RegularSquareGrid> implements INeighborhood<T> {

    public int[] getNeighbors(T grid, int i) {
        int nbCols = grid.getNbCols();
        int nbRows = grid.getNbRows();
        HeightConnected height = Neighborhood.HEIGHT_CONNECTED;
        Set<Integer> neighbors = new HashSet<>();
        int[] heightneigh = height.getNeighbors(grid, i);
        for (int neigh : heightneigh) {
            neighbors.add(neigh);
            for (int nneigh : height.getNeighbors(grid, neigh)) {
                neighbors.add(nneigh);
            }
        }
        return neighbors.stream().mapToInt(v -> v).toArray();
    }

}