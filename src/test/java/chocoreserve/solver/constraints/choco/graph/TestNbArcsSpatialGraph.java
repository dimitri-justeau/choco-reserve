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

package chocoreserve.solver.constraints.choco.graph;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.spatial.NbEdges;
import chocoreserve.solver.region.Region;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.Solver;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Test;

public class TestNbArcsSpatialGraph {

    /**
     * -------------------
     * | 0 | 1 | 2 | 3 | 4 |
     * -------------------
     * | 5 |   |   |   | 9 |
     * -------------------
     * |   |   |   |   |   |
     * -------------------
     * |   |   |   |   |   |
     * -------------------
     * |   |   |   |   |   |
     * -------------------
     */
    @Test
    public void testNbArcs() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        INeighborhood n4 = Neighborhoods.FOUR_CONNECTED;
        Region in = new Region(
                "in",
                n4, SetType.BIPARTITESET,
                new int[]{0, 1, 2, 3, 4, 5, 9}
        );
        Region out = new Region("out", n4);
        ReserveModel resModel = new ReserveModel(grid, out, in);
        GraphModel model = resModel.getChocoModel();

        resModel.maxDiameterSpatial(in, 3).post();

        NbEdges cNbEdges = new NbEdges(resModel, in);
        cNbEdges.post();

        Solver solver = model.getSolver();

        solver.findAllSolutions();
    }
}
