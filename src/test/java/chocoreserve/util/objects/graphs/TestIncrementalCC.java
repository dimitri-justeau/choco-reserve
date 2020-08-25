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

package chocoreserve.util.objects.graphs;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class TestIncrementalCC {

    @Test
    public void testIncrementalCC() {
        GraphModel model = new GraphModel();
        UndirectedGraphIncrementalCC g = new UndirectedGraphIncrementalCC(model, 10, SetType.BIPARTITESET, false);
        Assert.assertEquals(g.getNbCC(), 0);
        Assert.assertEquals(g.getRoots().length, 0);
        g.addNode(0);
        Assert.assertEquals(g.getNbCC(), 1);
        Assert.assertEquals(g.getSizeCC(0), 1);
        Assert.assertEquals(g.getRoots().length, 1);
        Assert.assertEquals(g.getRoots()[0], 0);
        g.addNode(1);
        Assert.assertEquals(g.getNbCC(), 2);
        Assert.assertEquals(g.getSizeCC(0), 1);
        Assert.assertEquals(g.getSizeCC(1), 1);
        Assert.assertEquals(g.getRoots().length, 2);
        g.addEdge(0, 1);
        Assert.assertEquals(g.getNbCC(), 1);
        Assert.assertEquals(g.getSizeCC(0), 2);
        Assert.assertEquals(g.getSizeCC(1), 2);
        Assert.assertEquals(g.getRoots().length, 1);
        g.addNode(2);
        g.addNode(3);
        Assert.assertEquals(g.getNbCC(), 3);
        Assert.assertEquals(g.getRoots().length, 3);
        g.addNode(4);
        g.addEdge(1, 4);

        Assert.assertEquals(g.getSizeCC(4), 3);
        Assert.assertEquals(g.getSizeCC(0), 3);
        Assert.assertEquals(g.getSizeCC(1), 3);
        Assert.assertEquals(g.getRoots().length, 3);

        g.addNode(5);
        g.addEdge(0, 5);

        Assert.assertEquals(g.getSizeCC(0), 4);
        Assert.assertEquals(g.getSizeCC(5), 4);
        Assert.assertEquals(g.getRoots().length, 3);

        g.addNode(6);
        g.addEdge(6, 2);
        g.addNode(7);

        Assert.assertEquals(g.getRoots().length, 4);

        System.out.println(g.getNbCC());
        System.out.println(g.getRoots());
        int[][] ccs = g.getConnectedComponents();
        for (int[] cc : ccs) {
            System.out.println(Arrays.toString(cc));
        }
    }

}
