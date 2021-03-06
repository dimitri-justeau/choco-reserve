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

import chocoreserve.grid.Grid;
import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.IntVar;

/**
 * Variable selector prioritizing sites both in the neighborhood of the LB of a region and in is UB.
 */
public class AdjacentSitesVarSelector implements VariableSelector<IntVar> {

    private AbstractRegion region;
    private int previous;

    public AdjacentSitesVarSelector(AbstractRegion region) {
        this.region = region;
        this.previous = -1;
    }

    @Override
    public IntVar getVariable(IntVar[] intVars) {
        if (previous == -1) {
            for (int i = 0; i < intVars.length; i++) {
                if (!intVars[i].isInstantiated()) {
                    previous = i;
                    return intVars[i];
                }
            }
        } else {
            Grid grid = region.getReserveModel().getGrid();
            INeighborhood neigh = region.getNeighborhood();
            for (int j : neigh.getNeighbors(grid, previous)) {
                if (!region.getSetVar().getLB().contains(j) && region.getSetVar().getUB().contains(j)) {
                    previous = j;
                    return intVars[j];
                }
            }
        }
        for (int i = 0; i < intVars.length; i++) {
            if (!intVars[i].isInstantiated()) {
                previous = i;
                return intVars[i];
            }
        }
        return null;
    }
}
