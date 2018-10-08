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

package chocoreserve.solver.constraints;

import chocoreserve.solver.IReserveModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;

/**
 * area_components constraint.
 */
public class AreaComponents extends ReserveConstraint {

    private IntVar minNCC, maxNCC;

    public AreaComponents(IReserveModel reserveModel, IntVar minNCC, IntVar maxNCC) {
        super(reserveModel);
        this.minNCC = minNCC;
        this.maxNCC = maxNCC;
    }

    @Override
    public void post() {
        UndirectedGraphVar g = reserveModel.getSpatialGraphVar();
        reserveModel.getChocoModel().sizeConnectedComponents(g, minNCC, maxNCC).post();
    }
}
