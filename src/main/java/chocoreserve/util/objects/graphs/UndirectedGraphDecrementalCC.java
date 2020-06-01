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
import org.chocosolver.util.objects.graphs.IGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.HashSet;
import java.util.Set;

/**
 * Backtrackable graph data structure decrementally maintaining connected components.
 * The implementation is based on [Even and Shiloach 1981] "An On-Line Edge-Deletion Problem",
 * BUT the implementation is incomplete (still more efficient that recomputing the CCs at each
 * constraint call).
 * Note that there are more efficient (and more complex) data structure, focused on dynamic node deletion.
 * e.g. [Duan 2010] "New Data Structures for Subgraph Connectivity".
 */
public class UndirectedGraphDecrementalCC extends UndirectedGraph {

    public IStateIntVector cc;
    private IStateInt nbCC;
    private IStateInt delta;
    private boolean init;

    public UndirectedGraphDecrementalCC(Model model, int n, SetType type, boolean allNodes) {
        super(model, n, type, allNodes);
        cc = model.getEnvironment().makeIntVector(getNbMaxNodes(), -1);
        nbCC = model.getEnvironment().makeInt(0);
        delta = model.getEnvironment().makeInt(0);
        init = false;
        for (int i = 0; i < n; i++) {
            cc.quickSet(i, -1);
        }
    }

    /**
     * Initialize the connected components table.
     */
    public void init() {
        int n = getNbMaxNodes();
        int[] fifo = new int[n];
        for (int x = 0; x < n; x++) {
            if (cc.quickGet(x) == -1) {
                int current = 0;
                int last = 1;
                fifo[current] = x;
                cc.quickSet(x, nbCC.get());
                while (current < last) {
                    int i = fifo[current];
                    for (int j : getSuccOrNeighOf(i)) {
                        if (cc.quickGet(j) == -1) {
                            cc.quickSet(j, nbCC.get());
                            fifo[last] = j;
                            last++;
                        }
                    }
                    current++;
                }
                nbCC.add(1);
            }
        }
        init = true;
    }

    @Override
    public boolean addNode(int x) {
        if (init) {
            throw new UnsupportedOperationException("No node must be added after initialization");
        }
        return super.addNode(x);
    }

    @Override
    public boolean removeNode(int x) {
        int[] neighs = getNeighOf(x).toArray();
        for (int y : neighs) {
            removeEdge(x, y);
        }
        cc.quickSet(x, -1);
        delta.add(1);
        return super.removeNode(x);
    }

    @Override
    public boolean addEdge(int x, int y) {
        if (init) {
            throw new UnsupportedOperationException("No edge must be added after initialization");
        }
        return super.addEdge(x, y);
    }

    @Override
    public boolean removeEdge(int x, int y) {
        boolean b = super.removeEdge(x, y);
        if (b) {
            // Process A : does the removal break the ccRoot ?
            UndirectedGraphDecrementalCC.ProcessA fromX = new UndirectedGraphDecrementalCC.ProcessA(this, x, y);
            UndirectedGraphDecrementalCC.ProcessA fromY = new UndirectedGraphDecrementalCC.ProcessA(this, y, x);
            boolean stopFromX = false;
            boolean stopFromY = false;
            while (!stopFromX || !stopFromY) {
                stopFromX = fromX.step();
                stopFromY = fromY.step();
            }
            if (stopFromX && !fromX.yReached) {
                for (int i = fromX.last - 1; i >= 0; i--) {
                    cc.quickSet(fromX.fifo[i], nbCC.get());
                }
                nbCC.add(1);
                return b;
            }
            if (stopFromY && !fromY.yReached) {
                cc.quickSet(nbCC.get(), y);
                for (int i = fromY.last - 1; i >= 0; i--) {
                    cc.quickSet(fromY.fifo[i], nbCC.get());
                }
                nbCC.add(1);
                return b;
            }
        }
        return b;
    }

    public int getNbCC() {
        return nbCC.get() - delta.get();
    }

    public Set<Integer> getConnectedComponentFromIndex(int ccIndex) {
        Set<Integer> connectedComponent = new HashSet<>();
        for (int i : getNodes()) {
            if (cc.quickGet(i) == ccIndex) {
                connectedComponent.add(i);
            }
        }
        assert connectedComponent.size() > 0;
        return connectedComponent;
    }

    public Set<Integer> getConnectedComponentOfNode(int node) {
        int ccIndex = cc.quickGet(node);
        return getConnectedComponentFromIndex(ccIndex);
    }

    public int getConnectedComponentIndex(int node) {
        return cc.quickGet(node);
    }

    public Set<Integer> getCCIndices() {
        Set<Integer> indices = new HashSet<>();
        for (int i : getNodes()) {
            indices.add(cc.quickGet(i));
        }
        return indices;
    }

    public class ProcessA {

        private IGraph graph;
        private int n, x, y, current, last, nbVisited;
        private int[] fifo, parent;
        private boolean yReached;

        public ProcessA(IGraph graph, int x, int y) {
            this.graph = graph;
            this.n = graph.getNbMaxNodes();
            this.x = x;
            this.y = y;
            init();
        }

        public void init() {
            this.current = 0;
            this.last = 1;
            this.fifo = new int[n];
            this.fifo[current] = x;
            this.parent = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = -1;
            }
            this.nbVisited = 0;
            this.yReached = false;
        }

        /**
         * @return True if the step led to y or if the dfs is over.
         */
        public boolean step() {
            if (current == last) {
                return true;
            }
            int i = fifo[current];
            for (int j : graph.getSuccOrNeighOf(i)) {
                if (parent[j] == -1) {
                    parent[j] = i;
                    nbVisited++;
                    if (j == y) {
                        yReached = true;
                        return true;
                    }
                    fifo[last] = j;
                    last++;
                }
            }
            current++;
            return false;
        }
    }
}
