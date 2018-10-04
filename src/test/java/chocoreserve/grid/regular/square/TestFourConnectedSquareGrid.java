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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Test case for FourConnectedSquareGrid.
 * Tests are on a 5x5 grid :
 *     -------------------
 *    | 0 | 1 | 2 | 3 | 4 |
 *     -------------------
 *    | 5 | 6 | 7 | 8 | 9 |
 *     -------------------
 *    | 10| 11| 12| 13| 14|
 *     -------------------
 *    | 15| 16| 17| 18| 19|
 *     -------------------
 *    | 20| 21| 22| 23| 24|
 *     -------------------
 */
public class TestFourConnectedSquareGrid {

    @Test
    public void testGetNbCells() {
        FourConnectedSquareGrid grid = new FourConnectedSquareGrid(5, 5);
        Assert.assertEquals(grid.getNbCells(), 25);
    }

    @Test
    public void testGetNeighbors() {
        FourConnectedSquareGrid grid = new FourConnectedSquareGrid(5, 5);
        // Cell in the middle: 12
        int[] neighbors = grid.getNeighbors(12);
        Arrays.sort(neighbors);;
        int[] expected = new int[] {7, 11, 13, 17};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 0
        neighbors = grid.getNeighbors(0);
        Arrays.sort(neighbors);
        expected = new int[] {1, 5};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 15
        neighbors = grid.getNeighbors(15);
        Arrays.sort(neighbors);
        expected = new int[] {10, 16, 20};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 9
        neighbors = grid.getNeighbors(9);
        Arrays.sort(neighbors);
        expected = new int[] {4, 8, 14};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 24
        neighbors = grid.getNeighbors(24);
        Arrays.sort(neighbors);
        expected = new int[] {19, 23};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
    }

    @Test
    public void testGetFullGraph() {

    }

    @Test
    public void testGetPartialGraph() {

    }
}
