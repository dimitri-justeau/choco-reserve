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

package chocoreserve.util;

import chocoreserve.solver.variable.SpatialGraphVar;
import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

import java.util.BitSet;

/**
 * @author Jean-Guillaume FAGES (cosling)
 * @since 18/01/2019.
 */
public class SpatialGraphVarConnectivityHelper {

    // input data
    private final SpatialGraphVar g;
    private final int n;

    // internal variable for graph exploration
    private final int[] fifo;

    // internal variables for Articulation Points and Bridge detection
    private TIntArrayList bridgeFrom, bridgeTo;
    private BitSet hasMandInSubtree, visited;
    private ISet articulationPoints;
    private int[] parent, time, minT;
    private int[] timer = new int[1];

    public SpatialGraphVarConnectivityHelper(SpatialGraphVar g) {
        this.g = g;
        this.n = g.getNbMaxNodes();
        this.fifo = new int[n];
    }

    //***********************************************************************************
    // CONNECTIVITY
    //***********************************************************************************

    public void exploreFrom(int root, BitSet visited) {
        int first = 0;
        int last = 0;
        int i = root;
        fifo[last++] = i;
        visited.set(i);
        while (first < last) {
            i = fifo[first++];
            for (int j : g.getPotNeighOf(i)) {
                if (!visited.get(j)) {
                    visited.set(j);
                    fifo[last++] = j;
                }
            }
        }
    }

    //***********************************************************************************
    // ARTICULATION POINTS AND BRIDGES
    //***********************************************************************************

    public ISet getArticulationPoints() {
        return articulationPoints;
    }

    public TIntArrayList getBridgeFrom() {
        return bridgeFrom;
    }

    public TIntArrayList getBridgeTo() {
        return bridgeTo;
    }

    public void findMandatoryArticulationPointsAndBridges() {
        if (articulationPoints == null) {
            articulationPoints = SetFactory.makeBipartiteSet(0);
            bridgeFrom = new TIntArrayList();
            bridgeTo = new TIntArrayList();
            hasMandInSubtree = new BitSet(n);
            visited = new BitSet(n);
            parent = new int[n];
            time = new int[n];
            minT = new int[n];
        }
        articulationPoints.clear();
        bridgeFrom.clear();
        bridgeTo.clear();
        ISet mNodes = g.getLB();
        if (mNodes.size() >= 2) {
            visited.clear();
            hasMandInSubtree.clear();
            for (int root : mNodes.toArray()) { // uses to array because default iterator may be used within the algorithm
                if (!visited.get(root)) {
                    // root node init
                    visited.set(root);
                    parent[root] = root;
                    timer[0] = 0;
                    // DFS from root
                    findMAPBFrom(root);
                }
            }
        }
    }

    private void findMAPBFrom(int i) {
        int nbMandChilds = 0;
        for (int j : g.getPotNeighOf(i)) {
            if (!visited.get(j)) {
                visited.set(j);
                parent[j] = i;
                timer[0]++;
                minT[j] = time[j] = timer[0];
                if (g.getLB().contains(j)) hasMandInSubtree.set(j);
                findMAPBFrom(j);

                // j sub-tree has been fully explored
                // propagates to i if subtrees of j have links to ancestors of i
                minT[i] = Math.min(minT[i], minT[j]);
                // propagates to i if subtrees of j include mandatory nodes
                if (hasMandInSubtree.get(j)) hasMandInSubtree.set(i);

                if (hasMandInSubtree.get(j)) {
                    nbMandChilds++;
                }

                // If the lowest vertex reachable from subtree under j is below i in DFS tree,
                // then (i,j) is a bridge
                if (minT[j] > time[i] && !g.getMandNeighOf(i).contains(j)) {
                    bridgeFrom.add(i);
                    bridgeTo.add(j);
                }

                // root node ?
                if (parent[i] == i) {
                    // root has >1 child with mandatory nodes in their subtrees
                    if (nbMandChilds > 1 && !g.getLB().contains(i))
                        articulationPoints.add(i);

                } else {
                    // j sub-tree has been explored and cannot go above i
                    if (minT[j] >= time[i] && hasMandInSubtree.get(j) && !g.getLB().contains(i))
                        articulationPoints.add(i);
                }
            }
            if (j != parent[i]) { // i can reach j (which might be above i)
                minT[i] = Math.min(minT[i], time[j]);
            }
        }
    }

    public boolean isBiconnected() {
        // connected ?
        int root = g.getUB().iterator().next();
        if (visited == null) visited = new BitSet(n);
        exploreFrom(root, visited);
        if (visited.cardinality() < g.getUB().size()) {
            return false;
        }
        // articulation point exist?
        findMandatoryArticulationPointsAndBridges();
        return articulationPoints.isEmpty();
    }
}
