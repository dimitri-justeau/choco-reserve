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
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;

/**
 * Size of a region's connected components.
 */
public class SizeConnectedComponents extends SpatialConstraint {

    private SetVar set;
    private IntVar minSizeCC, maxSizeCC;

    public SizeConnectedComponents(ReserveModel reserveModel, SetVar set, IntVar minSizeCC, IntVar maxSizeCC) {
        super(reserveModel);
        this.set = set;
        this.minSizeCC = minSizeCC;
        this.maxSizeCC = maxSizeCC;
    }

    @Override
    public void post() {
        UndirectedGraphVar g;
        if (set == reserveModel.getCore()) {
            g = reserveModel.getGraphCore();
        } else {
            if (set == reserveModel.getBuffer()) {
                g = reserveModel.getGraphBuffer();
            } else {
                g = reserveModel.getGraphOut();
            }
        }
        chocoModel.sizeConnectedComponents(g, minSizeCC, maxSizeCC).post();
    }
}
