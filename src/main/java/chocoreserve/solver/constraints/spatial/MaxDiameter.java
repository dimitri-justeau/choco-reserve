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

package chocoreserve.solver.constraints.spatial;

import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.IReserveModel;
import chocoreserve.solver.constraints.choco.PropSmallestEnclosingCircle;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

/**
 * MaxDiameter constraint
 */
public class MaxDiameter extends SpatialConstraint {

    private IntVar maxDiameter;

    public MaxDiameter(IReserveModel reserveModel, IntVar maxDiameter) {
        super(reserveModel);
        this.maxDiameter = maxDiameter;
    }

    @Override
    public void post() {
        RegularSquareGrid grid = (RegularSquareGrid) reserveModel.getGrid();
//        int nbRows = grid.getNbRows();
//        int nbCols = grid.getNbCols();
//        Constraint maxDiam = new Constraint(
//                "maxDiameter",
//                new PropSmallestEnclosingCircle(reserveModel.getSitesMatrix(), maxDiameter, nbRows, nbCols)
//        );
//        chocoModel.post(maxDiam);
    }
}
