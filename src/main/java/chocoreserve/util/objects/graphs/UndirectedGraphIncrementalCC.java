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

import org.chocosolver.memory.IStateIntVector;
import org.chocosolver.solver.Model;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Backtrackable graph data structure incrementally maintaining connected components by using a
 * union-find data structure.
 */
public class UndirectedGraphIncrementalCC extends UndirectedGraph {

    private IStateIntVector parent;
    private IStateIntVector rank;

    public UndirectedGraphIncrementalCC(Model model, int n, SetType type, boolean allNodes) {
        super(model, n, type, allNodes);
        parent = model.getEnvironment().makeIntVector(getNbMaxNodes(), -1);
        rank = model.getEnvironment().makeIntVector(getNbMaxNodes(), -1);
        for (int i = 0; i < n; i++) {
            makeSet(i);
        }
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
    }

    public int getNbCC() {
        ISet roots = getRoots();
        return roots.size();
    }

    public int getRoot(int node) {
        return find(node);
    }

    public ISet getRoots() {
        ISet roots = SetFactory.makeBitSet(0);
        for (int i = 0; i < getNbMaxNodes(); i++) {
            int r = find(i);
            if (getNodes().contains(i) && !roots.contains(r)) {
                roots.add(r);
            }
        }
        return roots;
    }

    public ISet getConnectedComponent(int root) {
        ISet cc = SetFactory.makeBipartiteSet(0);
        for (int i = 0; i < getNbMaxNodes(); i++) {
            if (getRoot(i) == root && getNodes().contains(i)) {
                cc.add(i);
            }
        }
        return cc;
    }

    public ISet getConnectedComponent(int root, int startFrom, ISet exclude) {
        ISet cc = SetFactory.makeBipartiteSet(0);
        for (int i = startFrom; i < getNbMaxNodes(); i++) {
            if (getRoot(i) == root && !exclude.contains(i) && getNodes().contains(i)) {
                cc.add(i);
            }
        }
        return cc;
    }

    public Map<Integer, Set<Integer>> getConnectedComponents() {
        ISet roots = getRoots();
        Map<Integer, Set<Integer>> ccs = new HashMap<>();
        for (int r : roots) {
            ccs.put(r, new HashSet<>());
        }
        for (int i = 0; i < getNbMaxNodes(); i++) {
            if (getNodes().contains(i)) {
                ccs.get(find(i)).add(i);
            }
        }
        return ccs;
    }
}
