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
import chocoreserve.grid.regular.square.RegularSquareGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * The height-connected neighborhood in a regular square grid.
 */
public class HeightConnected<T extends RegularSquareGrid> implements INeighborhood<T> {

    public int[] getNeighbors(T grid, int i) {
        int nbCols = grid.getNbCols();
        int nbRows = grid.getNbRows();
        List<Integer> neighbors = new ArrayList<>();
        if (i % nbCols != 0) {
            neighbors.add(i - 1);
        }
        if (i >= nbCols) {
            neighbors.add(i - nbCols);
        }
        if ((i + 1) % nbCols != 0) {
            neighbors.add(i + 1);
        }
        if (i < nbCols * (nbRows - 1)) {
            neighbors.add(i + nbCols);
        }
        if ((i < nbCols * (nbRows - 1)) && ((i + 1) % nbCols != 0)) {
            neighbors.add(i + nbCols + 1);
        }
        if ((i < nbCols * (nbRows - 1)) && (i % nbCols != 0)) {
            neighbors.add(i + nbCols - 1);
        }
        if ((i % nbCols != 0) && i >=nbCols) {
            neighbors.add(i - nbCols - 1);
        }
        if (((i + 1) % nbCols != 0) && i >= nbCols) {
            neighbors.add(i - nbCols + 1);
        }
        return neighbors.stream().mapToInt(v -> v).toArray();
    }

}
