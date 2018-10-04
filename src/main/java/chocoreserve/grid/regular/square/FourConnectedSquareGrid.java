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

package chocoreserve.grid.regular.square;

import chocoreserve.grid.IGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * 4-connected regular square grid.
 */
public class FourConnectedSquareGrid implements IGrid {

    int nbRows, nbCols;

    public FourConnectedSquareGrid(int nbRows, int nbCols) {
        this.nbRows = nbRows;
        this.nbCols = nbCols;
    }

    @Override
    public int getNbCells() {
        return nbRows * nbCols;
    }

    @Override
    public int[] getNeighbors(int i) {
        List<Integer> neighbors = new ArrayList<Integer>();
        if (i % nbCols != 0) {
            neighbors.add(i - 1);
        }
        if ((i + 1) % nbCols != 0) {
            neighbors.add(i + 1);
        }
        if (i >= nbCols) {
            neighbors.add(i - nbCols);
        }
        if (i < nbCols * (nbRows - 1)) {
            neighbors.add(i + nbCols);
        }
        return neighbors.stream().mapToInt(v -> v).toArray();
    }
}
