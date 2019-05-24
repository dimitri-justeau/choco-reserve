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

package chocoreserve.solver.search.selectors.variables;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TestNeighborhoodVarSelector {

    @Test
    public void testGetNeighborhood() {
        RegularSquareGrid grid = new RegularSquareGrid(10, 10);
        Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
        Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
        ReserveModel reserveModel = new ReserveModel(grid, out, core);
        NeighborhoodVarSelector varSelector = new NeighborhoodVarSelector(core);
        Set<Integer> neigh = varSelector.getNeighborhood();
        Assert.assertEquals(neigh.size(), 0);
        IntVar first = varSelector.getVariable(reserveModel.getSites());
        Assert.assertEquals(first, reserveModel.getSites()[0]);
        reserveModel.mandatorySites(core, 0, 3).post();
        Solver solver = reserveModel.getChocoSolver();
        final boolean[] b = {false};
        solver.setSearch(Search.intVarSearch(
                intVars -> {
                    if (!b[0]) {
                        Set<Integer> expected = new HashSet<>();
                        expected.add(1); expected.add(10);
                        expected.add(2); expected.add(4); expected.add(13);
                        Assert.assertEquals(expected, varSelector.getNeighborhood());
                        b[0] = true;
                    }
                    IntVar var = varSelector.getVariable(intVars);
                    return var;
                },
                variable -> variable.getLB(),
                reserveModel.getSites()
        ));
        solver.solve();
    }

}
