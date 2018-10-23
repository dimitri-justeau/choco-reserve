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

import chocoreserve.solver.IReserveModel;
import org.chocosolver.solver.variables.BoolVar;

/**
 *
 */
public class BufferArea extends SpatialConstraint {

    public BufferArea(IReserveModel reserveModel) {
        super(reserveModel);
    }

    @Override
    public void post() {
        BoolVar[] buffer = reserveModel.getBufferSites();
        BoolVar[] sites = reserveModel.getSites();
        for (int i = 0; i < buffer.length; i++) {
            chocoModel.not(chocoModel.and(buffer[i], sites[i])).post();
            int[] neigh = reserveModel.getGrid().getNeighbors(i);
            for (int j : neigh) {
                BoolVar b = chocoModel.and(sites[i], chocoModel.boolNotView(sites[j])).reify();
                chocoModel.ifThen(b, chocoModel.arithm(buffer[j], "=", 1));
            }
        }
    }
}
