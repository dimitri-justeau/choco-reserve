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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 *
 */
public class TestDecrementalCC {

    @Test
    public void testDecrementalCC() {
        GraphModel model = new GraphModel();
        UndirectedGraphDecrementalCC g = new UndirectedGraphDecrementalCC(model, 5, SetType.BIPARTITESET, false);
        for (int i = 0; i < 5; i++) {
            g.addNode(i);
        }
        int[][] edges = new int[][]{{0, 1},{1, 2},{2, 3},{3, 4}};
        for (int[] e : edges) {
            g.addEdge(e[0], e[1]);
        }
        g.init();
        // 1 cc
        Set<Integer> cc0 = new HashSet<>();
        IntStream.of(new int[] {0, 1, 2, 3, 4}).forEach(i -> cc0.add(i));
        Assert.assertEquals(g.getConnectedComponentOfNode(0), cc0);
        Assert.assertEquals(g.getNbCC(), 1);
        // 2 cc
        g.removeEdge(1, 2);
        Set<Integer> cc1 = new HashSet<>();
        IntStream.of(new int[] {2, 3, 4}).forEach(i -> cc1.add(i));
        Assert.assertEquals(g.getConnectedComponentOfNode(2), cc1);
        Assert.assertEquals(g.getNbCC(), 2);
        // 3 cc
        g.removeEdge(3, 4);
        Set<Integer> cc2 = new HashSet<>();
        IntStream.of(new int[] {4}).forEach(i -> cc2.add(i));
        Assert.assertEquals(g.getConnectedComponentOfNode(4), cc2);
        Assert.assertEquals(g.getNbCC(), 3);
        // 4 cc
        g.removeEdge(0, 1);
        Set<Integer> cc3 = new HashSet<>();
        IntStream.of(new int[] {1}).forEach(i -> cc3.add(i));
        Assert.assertEquals(g.getConnectedComponentOfNode(1), cc3);
        Assert.assertEquals(g.getNbCC(), 4);
        g.removeNode(1);
        Assert.assertEquals(g.getNbCC(), 3);
    }

}
