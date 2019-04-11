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

import chocoreserve.solver.feature.Feature;
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.IntVar;

import java.util.Set;

/**
 *
 */
public class RichestInNeighborhoodVarSelector implements VariableSelector<IntVar> {

    private PoorestVarSelector poorest;
    public NeighborhoodVarSelector neighborhood;


    public RichestInNeighborhoodVarSelector(Region region, double threshold, boolean deterministic, Feature... features) {
        this.poorest = new PoorestVarSelector(region.getReserveModel(), threshold, deterministic, features);
        this.neighborhood = new NeighborhoodVarSelector(region);
    }

    @Override
    public IntVar getVariable(IntVar[] intVars) {
        Set<Integer> neighorhood = neighborhood.getNeighborhood();
        for (int i = 0; i < poorest.ranking.length; i++) {
            IntVar var = poorest.reserveModel.getSites()[poorest.ranking[i]];
            if (neighorhood.contains(i) && !var.isInstantiated()) {
                return var;
            }
        }
        for (int i = 0; i < poorest.ranking.length; i++) {
            IntVar var = poorest.reserveModel.getSites()[poorest.ranking[i]];
            if (!var.isInstantiated()) {
                return var;
            }
        }
        return null;
    }
}
