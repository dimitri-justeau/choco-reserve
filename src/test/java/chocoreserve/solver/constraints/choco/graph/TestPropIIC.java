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

import chocoreserve.grid.Grid;
import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


/**
 * Test for PropIIC constraint.
 * LB =
 *     -------------------
 *    | 0 | 1 | 2 |   | 4 |
 *     -------------------
 *    |   |   | 7 |   | 9 |
 *     -------------------
 *    | 10| 11| 12|   |   |
 *     -------------------
 *    | 15|   |   |   |   |
 *     -------------------
 *    | 20| 21| 22| 23| 24|
 *     -------------------
 * UB =
 *     -------------------
 *    | 0 | 1 | 2 | 3 | 4 |
 *     -------------------
 *    | 5 | 6 | 7 | 8 | 9 |
 *     -------------------
 *    | 10| 11| 12| 13| 14|
 *     -------------------
 *    | 15| 16| 17| 18| 19|
 *     -------------------
 *    | 20| 21| 22| 23| 24|
 *     -------------------
 */
public class TestPropIIC {

    @Test
    public void testDijkstra() {
        Grid grid = new RegularSquareGrid(5, 5);
        INeighborhood n4 = Neighborhoods.FOUR_CONNECTED;
        GraphModel model = new GraphModel("TestPropInducedNeigh");
        UndirectedGraph LB = new UndirectedGraph(model, grid.getNbCells(), SetType.BIPARTITESET, false);
        for (int i : new int[] {0, 1, 2, 7, 10, 11, 12, 15, 20, 21, 22, 23, 24, 4, 9}) {
            LB.addNode(i);
        }
        int[][] edges = new int[][] {
                {0, 1}, {1, 2}, {2, 7}, {7, 12}, {11, 12}, {10, 11}, {10, 15}, {15, 20}, {20, 21}, {21, 22}, {22, 23},
                {23, 24}, {4, 9}
        };
        for (int[] e : edges) {
            LB.addEdge(e[0], e[1]);
        }
        UndirectedGraphVar g = model.graphVar(
            "testGraph",
            LB,
            n4.getFullGraph(grid, model, SetType.BIPARTITESET)
        );
        RealVar iic = model.realVar(0, 1, 1e-5);
        PropIIC propIIC = new PropIIC(g, iic);

        int[][] dists = propIIC.allPairsShortestPaths(g.getUB());

        int[] expected0 = new int[] {
                0, 1, 2, 3, 4,
                1, 2, 3, 4, 5,
                2, 3, 4, 5, 6,
                3, 4, 5, 6, 7,
                4, 5, 6, 7, 8
        };

        int[] expected18 = new int[] {
                6, 5, 4, 3, 4,
                5, 4, 3, 2, 3,
                4, 3, 2, 1, 2,
                3, 2, 1, 0, 1,
                4, 3, 2, 1, 2
        };

//        for (int[] dist : dists) {
//            System.out.println(Arrays.toString(dist));
//        }

        Assert.assertTrue(Arrays.equals(dists[0], expected0));
        Assert.assertTrue(Arrays.equals(dists[18], expected18));

        int[][] distsLB = propIIC.allPairsShortestPaths(g.getLB());

        Assert.assertEquals(distsLB[0][24], 12);
        Assert.assertEquals(distsLB[0][16], -1);
        Assert.assertEquals(distsLB[0][4], Integer.MAX_VALUE);

//        for (int[] dist : distsLB) {
//            System.out.println(Arrays.toString(dist));
//        }

        System.out.println("IIC(LB) = " + propIIC.computeIIC(g.getLB()));
        System.out.println("IIC(UB) = " + propIIC.computeIIC(g.getUB()));
    }
}
