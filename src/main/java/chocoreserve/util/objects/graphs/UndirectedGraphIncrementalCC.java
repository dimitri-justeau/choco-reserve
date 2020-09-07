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

import org.chocosolver.memory.IStateInt;
import org.chocosolver.memory.IStateIntVector;
import org.chocosolver.solver.Model;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.HashMap;
import java.util.Map;

/**
 * Backtrackable graph data structure incrementally maintaining connected components by using a
 * union-find data structure.
 */
public class UndirectedGraphIncrementalCC extends UndirectedGraph {

    protected IStateIntVector parent;
    protected IStateIntVector rank;
    protected IStateIntVector sizeCC;
    protected IStateInt nbCC;
    public int[] nodeCC;

    public UndirectedGraphIncrementalCC(Model model, int n, SetType type, boolean allNodes) {
        super(model, n, type, allNodes);
        parent = model.getEnvironment().makeIntVector(getNbMaxNodes(), -1);
        rank = model.getEnvironment().makeIntVector(getNbMaxNodes(), -1);
        sizeCC = model.getEnvironment().makeIntVector(getNbMaxNodes(), -1);
        nbCC = model.getEnvironment().makeInt(getNodes().size());
    }

    @Override
    public boolean removeNode(int x) {
        System.out.println("Nooo nooooode !");
        return super.removeNode(x);
    }

    @Override
    public boolean removeEdge(int x, int y) {
        System.out.println("Nooo eeeedge !");
        return super.removeEdge(x, y);
    }

    @Override
    public boolean addNode(int x) {
        if (!getNodes().contains(x)) {
            makeSet(x);
        }
        return super.addNode(x);
    }

    @Override
    public boolean addEdge(int x, int y) {
        boolean b = super.addEdge(x, y);
        if (b) {
            union(x, y);
        }
        return b;
    }

    private void makeSet(int i) {
        parent.quickSet(i, i);
        rank.quickSet(i, 0);
        sizeCC.quickSet(i, 1);
        nbCC.add(1);
    }

    private int find(int i) {
        int root = i;
        int p = parent.quickGet(root);
        while (p != root) {
            root = p;
            p = parent.quickGet(root);
        }
        int x = i;
        p = parent.quickGet(x);
        while (p != root) {
            parent.quickSet(x, root);
            x = p;
            p = parent.quickGet(x);
        }
        return root;
    }

    public void union(int i, int j) {
        int iRoot = find(i);
        int jRoot = find(j);
        if (iRoot == jRoot) {
            return;
        }
        int minRank = jRoot;
        int maxRank = iRoot;
        if (rank.quickGet(iRoot) < rank.quickGet(jRoot)) {
            minRank = iRoot;
            maxRank = jRoot;
        }
        parent.quickSet(minRank, maxRank);
        if (rank.quickGet(maxRank) == rank.quickGet(minRank)) {
            rank.quickSet(maxRank, rank.quickGet(maxRank) + 1);
        }
        int s1 = sizeCC.quickGet(minRank);
        int s2 = sizeCC.quickGet(maxRank);
        sizeCC.quickSet(minRank, s1 + s2);
        sizeCC.quickSet(maxRank, s1 + s2);
        nbCC.add(-1);
    }

    public int getNbCC() {
        return nbCC.get();
    }

    public int getSizeCC(int i) {
        return sizeCC.quickGet(parent.quickGet(i));
    }

    public int getRoot(int node) {
        return find(node);
    }

    public int[] getRoots() {
        int[] roots = new int[getNbCC()];
        boolean[] added = new boolean[getNbMaxNodes()];
        int idx = 0;
        for (int i = 0; i < getNbMaxNodes(); i ++) {
            int a = parent.quickGet(i);
            if (a >= 0) {
                int root = find(i);
                if (!added[root]) {
                    roots[idx++] = root;
                    added[root] = true;
                }
            }
        }
        return roots;
    }

    public int[][] getConnectedComponents() {
        nodeCC = new int[getNbMaxNodes()];
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
            nodeCC[i] = j;
            ccs[j][idx[j]] = i;
            idx[j] += 1;
        }
        return ccs;
    }
}
