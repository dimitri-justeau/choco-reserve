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
import chocoreserve.solver.region.Region;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Assert;
import org.junit.Test;

public class TestPropPerimeterSquareGridFourConnected {

    /**
     *     -----------
     *    | 0 | 1 | 2 |
     *     -----------
     *    | 3 | 4 | 5 |
     *     -----------
     *    | 6 | 7 | 8 |
     *     -----------
     */
    @Test
    public void testGetExteriorNodes() {
        RegularSquareGrid grid = new RegularSquareGrid(3, 3);
        INeighborhood n4 = Neighborhoods.FOUR_CONNECTED;
        Region in = new Region(
                "in",
                n4, SetType.BIPARTITESET, SetType.BIPARTITESET,
                new int[] {0, 1, 2, 3, 4, 6}
        );
        Region out = new Region("out", n4);
        ReserveModel resModel = new ReserveModel(grid, out, in);
        GraphModel model = resModel.getChocoModel();
        UndirectedGraphVar g = in.getGraphVar();
        IntVar perimeter = model.intVar(0, 100);
        PropPerimeterSquareGridFourConnected propPerimeter = new PropPerimeterSquareGridFourConnected(grid, g, perimeter);
        int expectedExtUB = 12;
        Assert.assertEquals(expectedExtUB, propPerimeter.getPerimeterGUB());
        int expectedExtLB = 12;
        Assert.assertEquals(expectedExtLB, propPerimeter.getPerimeterGLB());
    }

    /**
     *     -------------------
     *    | 0 | 1 | 2 | 3 | 4 |
     *     -------------------
     *    | 5 |   |   |   | 9 |
     *     -------------------
     *    |   |   |   |   |   |
     *     -------------------
     *    |   |   |   |   |   |
     *     -------------------
     *    |   |   |   |   |   |
     *     -------------------
     */
    @Test
    public void testPerimeter() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        INeighborhood n4 = Neighborhoods.FOUR_CONNECTED;
        Region in = new Region(
                "in",
                n4, SetType.BIPARTITESET, SetType.BIPARTITESET,
                new int[] {0, 1, 2, 3, 4, 5, 9}
        );
        Region out = new Region("out", n4);
        ReserveModel resModel = new ReserveModel(grid, out, in);
        GraphModel model = resModel.getChocoModel();
        UndirectedGraphVar g = in.getGraphVar();
        IntVar perimeter = model.intVar(0, 100);
        PropPerimeterSquareGridFourConnected propPerimeter = new PropPerimeterSquareGridFourConnected(grid, g, perimeter);
        int expectedExtLB = 16;
        Assert.assertEquals(expectedExtLB, propPerimeter.getPerimeterGLB());
        int[] bounds = propPerimeter.getBounds();
        System.out.println("PLB = " + bounds[0]);
        System.out.println("PUB = " + bounds[1]);
        System.out.println("P(GLB) = " + propPerimeter.getPerimeterGLB());
        System.out.println("P(GUB) = " + propPerimeter.getPerimeterGUB());
    }
}
