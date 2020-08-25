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

import org.chocosolver.solver.Model;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.HashMap;
import java.util.Map;

/**
 * Backtrackable graph data structure decrementally maintaining connected components.
 */
public class UndirectedGraphDecrementalFromSubgraph extends UndirectedGraph {

    private UndirectedGraphIncrementalCC GLB;
    public FindCCs findCCs;

    public UndirectedGraphDecrementalFromSubgraph(Model model, int n, SetType type, UndirectedGraphIncrementalCC GLB, boolean allNodes) {
        super(model, n, type, allNodes);
        this.GLB = GLB;
    }

    public void findCCs() {
        this.findCCs = new FindCCs();
    }

    public int getNbCC() {
        return findCCs.nbCC;
    }

    public int getSizeCC(int i) {
        return findCCs.sizeCC[findCCs.parent[i]];
    }

    public int[] getRoots() {
        int[] roots = new int[getNbCC()];
        boolean[] added = new boolean[getNbMaxNodes()];
        int idx = 0;
        for (int i = 0; i < getNbMaxNodes(); i ++) {
            int a = findCCs.parent[i];
            if (a >= 0) {
                int root = findCCs.find(i);
                if (!added[root]) {
                    roots[idx++] = root;
                    added[root] = true;
                }
            }
        }
        return roots;
    }

    public int getRoot(int node) {
        return findCCs.find(node);
    }

    public int[][] getConnectedComponents() {
        int[] roots = getRoots();
        int[][] ccs = new int[roots.length][];
        int[] idx = new int[roots.length];
        Map<Integer, Integer> mapRoot = new HashMap<>();
        for (int i = 0; i < roots.length; i++) {
            idx[i] = 0;
            ccs[i] = new int[getSizeCC(roots[i])];
            mapRoot.put(roots[i], i);
        }
        for (int i : getNodes()) {
            int r = getRoot(i);
            int j = mapRoot.get(r);
            ccs[j][idx[j++]] = i;
        }
        return ccs;
    }

    public class FindCCs {

        private  int[] parent, rank, sizeCC;
        private int nbCC;

        public FindCCs() {
            int n = getNbMaxNodes();
            parent = new int[n];
            rank = new int[n];
            sizeCC = new int[n];
            nbCC = GLB.nbCC.get();
            for (int i = 0; i < n; i++) {
                parent[i] = GLB.parent.quickGet(i);
                rank[i] = GLB.rank.quickGet(i);
                sizeCC[i] = GLB.sizeCC.quickGet(i);
            }
            findAllCCFromGLB();
        }

        private void findAllCCFromGLB() {
            int[] nodes = getNodes().toArray();
            for (int i : nodes) {
                if (!GLB.getNodes().contains(i)) {
                    parent[i] = i;
                    rank[i] = 0;
                    sizeCC[i] = 1;
                    nbCC += 1;
                }
            }
            for (int i : nodes) {
                if (!GLB.getNodes().contains(i)) {
                    for (int j : getNeighOf(i)) {
                        if (!GLB.edgeExists(i, j)) {
                            union(i, j);
                        }
                    }
                }
            }
        }

        private int find(int i) {
            int root = i;
            int p = parent[root];
            while (p != root) {
                root = p;
                p = parent[root];
            }
            int x = i;
            p = parent[x];
            while (p != root) {
                parent[x] = root;
                x = p;
                p = parent[x];
            }
            return root;
        }

        private void union(int i, int j) {
            int iRoot = find(i);
            int jRoot = find(j);
            if (iRoot == jRoot) {
                return;
            }
            int minRank = jRoot;
            int maxRank = iRoot;
            if (rank[iRoot] < rank[jRoot]) {
                minRank = iRoot;
                maxRank = jRoot;
            }
            parent[minRank] = maxRank;
            if (rank[maxRank] == rank[minRank]) {
                rank[maxRank] = rank[maxRank] + 1;
            }
            int s1 = sizeCC[minRank];
            int s2 = sizeCC[maxRank];
            sizeCC[minRank] = s1 + s2;
            sizeCC[maxRank] = s1 + s2;
            nbCC += -1;
        }
    }
}
