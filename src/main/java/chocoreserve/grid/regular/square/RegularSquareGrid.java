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
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

/**
 * Abstract base class for regular square grids.
 */
public abstract class RegularSquareGrid extends Grid {

    protected int nbRows, nbCols;
    protected int border;

    public RegularSquareGrid(int nbRows, int nbCols, int border) {
        assert nbCols > 0;
        assert nbRows > 0;
        assert border >= 0;
        this.nbRows = nbRows + 2 * border;
        this.nbCols = nbCols + 2 * border;
        this.border = border;
    }

    public RegularSquareGrid(int nbRows, int nbCols) {
        this(nbRows, nbCols, 0);
    }

    @Override
    public int getNbCells() {
        return getNbCells(false);
    }

    public int getNbCells(boolean ignoreBorder) {
        int offset = ignoreBorder ? 2 * border : 0;
        return (nbRows - offset) * (nbCols - offset);
    }

    /**
     * @param row The row.
     * @param col The column.
     * @return The flattened index of a cell from its grid coordinates.
     */
    public int getIndexFromCoordinates(int row, int col) {
        return getIndexFromCoordinates(row, col, false);
    }

    public int getIndexFromCoordinates(int row, int col, boolean ignoreBorder) {
        int offset = ignoreBorder ? border : 0;
        assert row >= 0;
        assert row < nbRows - offset;
        assert col >= 0;
        assert col < nbCols - offset;
        return getNbCols(false) * (row + offset) + (col + offset);
    }

    /**
     * @param index The flattened index of a cell.
     * @return The grid coordinates [row, col] from its flattened index.
     */
    public int[] getCoordinatesFromIndex(int index) {
        return getCoordinatesFromIndex(index, false);
    }

    public int[] getCoordinatesFromIndex(int index, boolean ignoreBorder) {
        int row = Math.floorDiv(index, getNbCols(ignoreBorder));
        int col = index % getNbCols(ignoreBorder);
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
        return getNbRows(false);
    }

    public int getNbRows(boolean ignoreBorder) {
        int offset = ignoreBorder ? 2 * border : 0;
        return nbRows - offset;
    }

    /**
     * @return The number of columns.
     */
    public int getNbCols() {
        return getNbCols(false);
    }

    public int getNbCols(boolean ignoreBorder) {
        int offset = ignoreBorder ? 2 * border : 0;
        return nbCols - offset;
    }

    public int getBorder() {
        return border;
    }

    public boolean isInBorder(int index) {
        int[] coord = getCoordinatesFromIndex(index, false);
        int i = coord[0];
        int j = coord[1];
        return  (i < border || i > getNbRows(true) + border || j < border || j >= getNbCols(true) + border);
    }

    public int[] getBordered(int[] array) {
        if (border == 0) {
            assert array.length == getNbCells(false);
            return array;
        }
        assert array.length == getNbCells(true);
        int[] bordered = new int[getNbCells(false)];
        for (int i = 0; i < getNbRows(false); i++) {
            for (int j = 0; j < getNbCols(false); j++) {
                // If in border of last line val = 0
                if (i < border || i >= getNbRows(true) + border || j < border || j >= getNbCols(true) + border) {
                    bordered[getIndexFromCoordinates(i, j, false)] = 0;
                    continue;
                } else {
                    bordered[getIndexFromCoordinates(i, j, false)] = array[getIndexFromCoordinates(i, j, true)];
                }
            }
        }
        return bordered;
    }

    public double[] getBordered(double[] array) {
        if (border == 0) {
            assert array.length == getNbCells(false);
            return array;
        }
        assert array.length == getNbCells(true);
        double[] bordered = new double[getNbCells(false)];
        for (int i = 0; i < getNbRows(false); i++) {
            for (int j = 0; j < getNbCols(false); j++) {
                // If in border of last line val = 0
                if (i < border || i >= getNbRows(true) + border || j < border || j >= getNbCols(true) + border) {
                    bordered[getIndexFromCoordinates(i, j, false)] = 0;
                    continue;
                } else {
                    bordered[getIndexFromCoordinates(i, j, false)] = array[getIndexFromCoordinates(i, j, true)];
                }
            }
        }
        return bordered;
    }

    /**
     * @return The cartesian coordinates of the pixels of the grid.
     */
    public double[][][] getCartesianCoordinates() {
        double[][][] coords = new double[getNbRows()][getNbCols()][];
        for (int y = 0; y < getNbRows(); y++) {
            for (int x = 0; x < getNbCols(); x++) {
                coords[y][x] = new double[] {x, y};
            }
        }
        return coords;
    }
}
