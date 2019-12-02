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
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;

/**
 *
 */
public class StructuralConnectivity extends SpatialConstraint {

    private Region region;
    public IntVar N;
    public IntVar[] struct;

      private int[][] overlappingStructuralUnits;

    public StructuralConnectivity(ReserveModel reserveModel, Region region, int min, int max,
                                  int[][] overlappingStructuralUnits) {
        super(reserveModel);
        assert overlappingStructuralUnits.length == reserveModel.getGrid().getNbCells();
        this.overlappingStructuralUnits = overlappingStructuralUnits;
        this.region = region;
        this.N = chocoModel.intVar("struct_N", min, max);

        this.struct = Arrays.stream(overlappingStructuralUnits)
//                .filter(i -> i.length > 0)
                .map(i -> chocoModel.intVar(ArrayUtils.concat(i, -1))).toArray(IntVar[]::new);

    }

    @Override
    public void post() {

        for (int i = 0; i < reserveModel.getSites().length; i++) {
            if (overlappingStructuralUnits[i].length == 0) {
                chocoModel.notMember(i, region.getSetVar()).post();
            }

            chocoModel.ifThen(
                    chocoModel.member(i, region.getSetVar()),
                    chocoModel.arithm(struct[i], "!=", -1)
            );
        }
        chocoModel.nValues(struct, N).post();
//        chocoModel.allEqual(struct).post();
    }
}
