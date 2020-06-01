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

import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.graph.spatial.PropSmallestEnclosingCircleSpatialGraph;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.RealVar;

import java.util.Arrays;

/**
 *
 */
public class RadiusSpatialGraph extends SpatialConstraint {

    private AbstractRegion region;
    public RealVar radius;
    public RealVar centerX;
    public RealVar centerY;

    public RadiusSpatialGraph(ReserveModel reserveModel, AbstractRegion region, RealVar radius,
                              RealVar centerX, RealVar centerY) {
        super(reserveModel);
        this.region = region;
        this.radius = radius;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public RadiusSpatialGraph(ReserveModel reserveModel, AbstractRegion region, RealVar radius) {
        this(
                reserveModel,
                region,
                radius,
                reserveModel.getChocoModel().realVar(
                        Arrays.stream(reserveModel.getGrid().getCartesianCoordinates())
                                .mapToDouble(c -> c[0]).min().getAsDouble(),
                        Arrays.stream(reserveModel.getGrid().getCartesianCoordinates())
                                .mapToDouble(c -> c[0]).max().getAsDouble(),
                        1e-5
                ),
                reserveModel.getChocoModel().realVar(
                        Arrays.stream(reserveModel.getGrid().getCartesianCoordinates())
                                .mapToDouble(c -> c[1]).min().getAsDouble(),
                        Arrays.stream(reserveModel.getGrid().getCartesianCoordinates())
                                .mapToDouble(c -> c[1]).max().getAsDouble(),
                        1e-5
                )
        );
    }

    @Override
    public void post() {
        Constraint c = new Constraint("minEnclosingCircle", new PropSmallestEnclosingCircleSpatialGraph(
                region.getSetVar(),
                radius,
                centerX,
                centerY
        ));
        chocoModel.post(c);
    }
}
