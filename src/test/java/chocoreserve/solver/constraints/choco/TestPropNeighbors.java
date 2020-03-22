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

package chocoreserve.solver.constraints.choco;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.SetVar;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Test class for PropNeighbors.
 */
public class TestPropNeighbors {

    @Test
    public void testNeighBuff() {
        // 4-connected and 8-connected grid for neighborhood initialization
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        INeighborhood n8 = Neighborhoods.HEIGHT_CONNECTED;
        int[] universe = IntStream.range(0, grid.getNbCells()).toArray();
        int[][] adjLists = new int[grid.getNbCells()][];
        IntStream.range(0, grid.getNbCells())
                .forEach(i -> adjLists[i] = n8.getNeighbors(grid, i).toArray());

        // ----------- //
        // Choco model //
        // ----------- //

        Model model = new Model();

        // Set variables
        SetVar U = model.setVar(universe); // Universe
        SetVar C = model.setVar("C", new int[] {}, universe); // Core
        SetVar neighC = model.setVar("neighC", new int[] {}, universe); // Core neigh.
        SetVar O = model.setVar("O", new int[] {}, universe); // Out
        SetVar neighO = model.setVar("neighO", new int[] {}, universe); // Out neigh.
        SetVar B = model.setVar("B", new int[] {}, universe); // Buffer

        // Constraints
        PropNeighbors propNeighC = new PropNeighbors(C, neighC, adjLists);
        PropNeighbors propNeighO = new PropNeighbors(O, neighO, adjLists);
        Constraint partition = model.partition(new SetVar[]{C, O, B}, U);
        partition.post();
        model.post(new Constraint("propNeighC", propNeighC));
        model.post(new Constraint("propNeighO", propNeighO));
        model.disjoint(C, neighO).post();
        model.disjoint(O, neighC).post();
        model.intersection(new SetVar[] {neighC, neighO}, B).post();

        // Solve
        Solver solver = model.getSolver();
        solver.findAllSolutions();
    }
}
