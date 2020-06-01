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

package chocoreserve.solver.constraints.choco.graph.spatial;

import chocoreserve.solver.variable.SpatialGraphVar;
import chocoreserve.util.ConnectivityFinderSpatialGraph;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Propagator ensuring that the number of vertices of the largest connected is maxSizeCC
 * (cf. MAX_NCC graph property, http://www.emn.fr/x-info/sdemasse/gccat/sec2.2.2.4.html#uid922).
 *
 * @author Dimitri Justeau-Allaire
 */
public class PropSizeMaxCCSpatialGraph extends Propagator<Variable> {

    /* Variables */

    private SpatialGraphVar g;
    private IntVar sizeMaxCC;
    private ConnectivityFinderSpatialGraph GLBCCFinder, GUBCCFinder;

    /* Constructor */

    public PropSizeMaxCCSpatialGraph(SpatialGraphVar graph, IntVar sizeMaxCC) {
        super(new Variable[]{graph, sizeMaxCC}, PropagatorPriority.QUADRATIC, false);
        this.g = graph;
        this.sizeMaxCC = sizeMaxCC;
        this.GLBCCFinder = new ConnectivityFinderSpatialGraph(g.getGLB());
        this.GUBCCFinder = new ConnectivityFinderSpatialGraph(g.getGUB());
    }

    /* Methods */

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ALL_EVENTS;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // Find CCs and their sizes
        this.GLBCCFinder.findAllCC();
        this.GUBCCFinder.findAllCC();
        int nbCC_GLB = GLBCCFinder.getNBCC();
        // Retrieve MAX_NCC(g) lower and upper bounds from g
        int maxNCC_LB = GLBCCFinder.getSizeMaxCC();
        int maxNCC_UB = GUBCCFinder.getSizeMaxCC();
        // 1. Trivial case
        if (sizeMaxCC.getLB() > g.getPotentialNodes().size()) {
            fails();
        }
        // 2. Trivial case TODO: How to properly get |E_TU| ?
        // 3.
        if (maxNCC_UB < sizeMaxCC.getLB()) {
            fails();
        }
        // 4.
        if (maxNCC_LB > sizeMaxCC.getUB()) {
            fails();
        }
        // 5.
        if (sizeMaxCC.getLB() < maxNCC_LB) {
            sizeMaxCC.updateLowerBound(maxNCC_LB, this);
        }
        // 6.
        if (sizeMaxCC.getUB() > maxNCC_UB) {
            sizeMaxCC.updateUpperBound(maxNCC_UB, this);
        }
        // 7.
        if (maxNCC_UB > sizeMaxCC.getUB()) {
            boolean recomputeMaxNCC_UB = false;
            // a.
            if (sizeMaxCC.getUB() == 1) {
                for (int i : g.getPotentialNodes()) {
                    for (int j : g.getPotNeighOf(i)) {
                        g.removeNode(j, this);
                    }
                }
            }
            // b.
            if (sizeMaxCC.getUB() == 0) {
                for (int i : g.getPotentialNodes()) {
                    g.removeNode(i, this);
                }
            }
            for (int cc = 0; cc < nbCC_GLB; cc++) {
                int[] sizeCC = GLBCCFinder.getSizeCC();
                // c.
                if (sizeCC[cc] == sizeMaxCC.getUB()) {
                    Map<Integer, Set<Integer>> ccPotentialNeighbors = getGLBCCPotentialNeighbors(cc);
                    for (int i : ccPotentialNeighbors.keySet()) {
                        for (int j : ccPotentialNeighbors.get(i)) {
                            g.removeNode(j, this);
                        }
                    }
                } else {
                    // d.
                    for (int cc2 = cc + 1; cc2 < nbCC_GLB; cc2++) {
                        if (sizeCC[cc] + sizeCC[cc2] > sizeMaxCC.getUB()) {
                            Map<Integer, Set<Integer>> ccPotentialNeighbors = getGLBCCPotentialNeighbors(cc);
                            for (int i : ccPotentialNeighbors.keySet()) {
                                for (int j : ccPotentialNeighbors.get(i)) {
                                    if (getGLBCCNodes(cc2).contains(j)) {
                                        recomputeMaxNCC_UB = true;
                                        g.removeNode(j, this);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // e.
            if (recomputeMaxNCC_UB) {
                this.GUBCCFinder.findAllCC();
                maxNCC_UB = GUBCCFinder.getSizeMaxCC();
                if (maxNCC_UB < sizeMaxCC.getLB()) {
                    fails();
                }
                if (sizeMaxCC.getUB() > maxNCC_UB) {
                    sizeMaxCC.updateUpperBound(maxNCC_UB, this);
                }
            }
        }
        // 8.
        int nb_candidates = 0;
        int candidate = -1;
        int size = 0;
        for (int cc = 0; cc < GUBCCFinder.getNBCC(); cc++) {
            int s = GUBCCFinder.getSizeCC()[cc];
            if (s >= sizeMaxCC.getLB()) {
                nb_candidates++;
                candidate = cc;
                size = s;
            }
            if (nb_candidates > 1) {
                break;
            }
        }
        if (nb_candidates == 1 && size == sizeMaxCC.getLB()) {
            int i = GUBCCFinder.getCCFirstNode()[candidate];
            while (i != -1) {
                g.enforceNode(i, this);
                i = GUBCCFinder.getCCNextNode()[i];
            }
            sizeMaxCC.instantiateTo(sizeMaxCC.getLB(), this);
        }
    }

    /**
     * Retrieve the nodes of a GLB CC.
     *
     * @param cc The GLB CC index.
     * @return The set of nodes of the GLB CC cc.
     */
    private Set<Integer> getGLBCCNodes(int cc) {
        Set<Integer> ccNodes = new HashSet<>();
        for (int i = GLBCCFinder.getCCFirstNode()[cc]; i >= 0; i = GLBCCFinder.getCCNextNode()[i]) {
            ccNodes.add(i);
        }
        return ccNodes;
    }

    /**
     * Retrieve the potential CC neighbors (i.e. not in the CC) of a GLB CC.
     *
     * @param cc The GLB CC index.
     * @return A map with frontier nodes of the CC as keys (Integer), and their potential neighbors that are
     * outside the CC (Set<Integer>). Only the frontier nodes that have at least one potential neighbor outside the
     * CC are stored in the map.
     * {
     * frontierNode1: {out-CC potential neighbors},
     * frontierNode3: {...},
     * ...
     * }
     */
    private Map<Integer, Set<Integer>> getGLBCCPotentialNeighbors(int cc) {
        Map<Integer, Set<Integer>> ccPotentialNeighbors = new HashMap<>();
        // Retrieve all nodes of CC
        Set<Integer> ccNodes = getGLBCCNodes(cc);
        // Retrieve neighbors of the nodes of CC that are outside the CC
        for (int i : ccNodes) {
            Set<Integer> outNeighbors = new HashSet<>();
            for (int j : g.getPotNeighOf(i)) {
                if (!ccNodes.contains(j)) {
                    outNeighbors.add(j);
                }
            }
            if (outNeighbors.size() > 0) {
                ccPotentialNeighbors.put(i, outNeighbors);
            }
        }
        return ccPotentialNeighbors;
    }


    @Override
    public ESat isEntailed() {
        // Find CCs and their sizes
        this.GLBCCFinder.findAllCC();
        this.GUBCCFinder.findAllCC();
        // Retrieve MAX_NCC(g) lower and upper bounds from g
        int maxNCC_LB = GLBCCFinder.getSizeMaxCC();
        int maxNCC_UB = GUBCCFinder.getSizeMaxCC();
        // Check entailment
        if (maxNCC_UB < sizeMaxCC.getLB() || maxNCC_LB > sizeMaxCC.getUB()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            if (maxNCC_LB == sizeMaxCC.getValue()) {
                return ESat.TRUE;
            } else {
                return ESat.FALSE;
            }
        }
        return ESat.UNDEFINED;
    }
}
