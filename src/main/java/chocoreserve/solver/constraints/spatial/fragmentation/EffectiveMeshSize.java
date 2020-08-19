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

package chocoreserve.solver.constraints.spatial.fragmentation;

import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.fragmentation.PropAggregationIndex;
import chocoreserve.solver.constraints.choco.fragmentation.PropEffectiveMeshSize;
import chocoreserve.solver.constraints.spatial.NbEdges;
import chocoreserve.solver.constraints.spatial.SpatialConstraint;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

public class EffectiveMeshSize extends SpatialConstraint {

    private AbstractRegion region;
    public IntVar mesh;
    public int landscapeArea;
    public int precision;
    private boolean maximize;

    public EffectiveMeshSize(ReserveModel reserveModel, AbstractRegion region, int landscapeArea, int precision,
                             boolean maximize) {
        super(reserveModel);
        this.region = region;
        this.landscapeArea = landscapeArea;
        this.precision = precision;
        this.mesh = reserveModel.getChocoModel().intVar(
                "MESH_" + region.getName(),
                0, (int) (landscapeArea * Math.pow(10, precision))
        );
        this.maximize = maximize;
    }

    @Override
    public void post() {
        chocoModel.post(
                new Constraint(
                        "MESH_" + region.getName(),
                        new PropEffectiveMeshSize(
                                region.getSetVar(),
                                mesh,
                                landscapeArea,
                                precision,
                                maximize
                        )
                )
        );
    }
}
