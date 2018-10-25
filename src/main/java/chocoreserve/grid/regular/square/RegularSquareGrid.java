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

import chocoreserve.grid.Grid;

import java.util.Arrays;

/**
 * Abstract base class for regular square grids.
 */
public abstract class RegularSquareGrid extends Grid {

    protected int nbRows, nbCols;

    public RegularSquareGrid(int nbRows, int nbCols) {
        this.nbRows = nbRows;
        this.nbCols = nbCols;
    }

    @Override
    public int getNbCells() {
        return nbRows * nbCols;
    }

    /**
     * @param row The row.
     * @param col The column.
     * @return The flattened index of a cell from its grid coordinates.
     */
    public int getIndexFromCoordinates(int row, int col) {
        return getNbCols() * row + col;
    }

    /**
     * @param index The flattened index of a cell.
     * @return The grid coordinates [row, col] from its flattened index.
     */
    public int[] getCoordinatesFromIndex(int index) {
        int row = Math.floorDiv(index, getNbCols());
        int col = index % getNbCols();
        return new int[] {row, col};
    }

    public int[][] getMatrixNeighbors(int i, int j) {
        int[] indexNeighbors = getNeighbors(getIndexFromCoordinates(i, j));
        int[][] matrixNeighbors = new int[indexNeighbors.length][];
        for (int k = 0; k < indexNeighbors.length; k++) {
            matrixNeighbors[k] = getCoordinatesFromIndex(indexNeighbors[k]);
        }
        return matrixNeighbors;
    }

    /**
     * @return The number of rows.
     */
    public int getNbRows() {
        return nbRows;
    }

    /**
     * @return The number of columns.
     */
    public int getNbCols() {
        return nbCols;
    }

    /**
     * @return The cartesian coordinates of the pixels of the grid.
     */
    public double[][][] getCoordinates() {
        double[][][] coords = new double[getNbRows()][getNbCols()][];
        for (int y = 0; y < getNbRows(); y++) {
            for (int x = 0; x < getNbCols(); x++) {
                coords[y][x] = new double[] {x, y};
            }
        }
        return coords;
    }
}
