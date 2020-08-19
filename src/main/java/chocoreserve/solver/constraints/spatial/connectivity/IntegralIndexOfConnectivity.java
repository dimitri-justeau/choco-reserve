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

package chocoreserve.solver.constraints.spatial.connectivity;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.connectivity.PropIIC;
import chocoreserve.solver.constraints.spatial.SpatialConstraint;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

public class IntegralIndexOfConnectivity extends SpatialConstraint {

    private AbstractRegion region;
    public IntVar iic;
    public int landscapeArea;
    public INeighborhood distanceThreshold;
    public int precision;
    private boolean maximize;

    public IntegralIndexOfConnectivity(ReserveModel reserveModel, AbstractRegion region, int landscapeArea,
                                       INeighborhood distanceThreshold, int precision, boolean maximize) {
        super(reserveModel);
        this.region = region;
        this.landscapeArea = landscapeArea;
        this.precision = precision;
        this.distanceThreshold = distanceThreshold;
        this.iic = reserveModel.getChocoModel().intVar(
                "IIC_" + region.getName(),
                0, (int) (Math.pow(10, precision))
        );
        this.maximize = maximize;
    }

    @Override
    public void post() {
        chocoModel.post(
                new Constraint(
                        "IIC_" + region.getName(),
                        new PropIIC(
                                region.getSetVar(),
                                iic,
                                landscapeArea,
                                distanceThreshold,
                                precision,
                                maximize
                        )
                )
        );
    }
}
