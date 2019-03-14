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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 2-wide-8-connected regular square grid.
 */
public class TwoWideHeightConnectedSquareGrid extends RegularSquareGrid {

    public TwoWideHeightConnectedSquareGrid(int nbRows, int nbCols) {
        super(nbRows, nbCols);
    }

    public TwoWideHeightConnectedSquareGrid(int nbRows, int nbCols, int border) {
        super(nbRows, nbCols, border);
    }

    @Override
    public int[] getNeighbors(int i) {
        HeightConnectedSquareGrid height = new HeightConnectedSquareGrid(nbRows, nbCols);
        Set<Integer> neighbors = new HashSet<>();
        int[] heightneigh = height.getNeighbors(i);
        for (int neigh : heightneigh) {
            neighbors.add(neigh);
            for (int nneigh : height.getNeighbors(neigh)) {
                neighbors.add(nneigh);
            }
        }
        return neighbors.stream().mapToInt(v -> v).toArray();
    }
}
