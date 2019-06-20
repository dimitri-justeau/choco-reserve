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
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;

/**
 *
 */
public class StructuralConnectivity extends SpatialConstraint {

    private Region region;
    private int[][] overlappingStructuralUnits;

    public StructuralConnectivity(ReserveModel reserveModel, Region region, int[][] overlappingStructuralUnits) {
        super(reserveModel);
        this.region = region;
        assert overlappingStructuralUnits.length == reserveModel.getGrid().getNbCells();
        this.overlappingStructuralUnits = overlappingStructuralUnits;
    }

    @Override
    public void post() {
        IntVar[] struct = new IntVar[reserveModel.getSites().length];
        for (int i = 0; i < reserveModel.getSites().length; i++) {
            if (overlappingStructuralUnits[i].length == 0) {
                struct[i] = chocoModel.intVar(-1);
            }
            else {
                struct[i] = chocoModel.intVar(overlappingStructuralUnits[i]);
            }
        }
        chocoModel.allEqual(struct).post();
    }
}
