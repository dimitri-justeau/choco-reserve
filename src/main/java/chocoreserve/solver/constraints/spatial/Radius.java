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

import chocoreserve.solver.Region;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.PropSmallestEnclosingCircle;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.util.tools.ArrayUtils;

/**
 *
 */
public class Radius extends SpatialConstraint {

    private Region region;
    public RealVar radius;
    public RealVar centerX;
    public RealVar centerY;
    public double[][] coordinates;

    public Radius(ReserveModel reserveModel, Region region, double[][] coordinates, RealVar radius,
                  RealVar centerX, RealVar centerY) {
        super(reserveModel);
        this.region = region;
        this.radius = radius;
        this.centerX = centerX;
        this.centerY = centerY;
        this.coordinates = coordinates;
    }

    public Radius(ReserveModel reserveModel, Region region, RealVar radius) {
        this(
                reserveModel,
                region,
                ArrayUtils.flatten(reserveModel.getGrid().getCartesianCoordinates()),
                radius,
                reserveModel.getChocoModel().realVar(0, reserveModel.getNbCols(), 1e-5),
                reserveModel.getChocoModel().realVar(0, reserveModel.getNbRows(), 1e-5)
        );
    }

    @Override
    public void post() {
        BoolVar[] boolVars = chocoModel.boolVarArray(reserveModel.getGrid().getNbCells());
        chocoModel.setBoolsChanneling(boolVars, region.getSetVar()).post();
        Constraint c = new Constraint("minEnclosingCircle", new PropSmallestEnclosingCircle(
                boolVars,
                coordinates,
                radius,
                centerX,
                centerY
        ));
        chocoModel.post(c);
    }
}
