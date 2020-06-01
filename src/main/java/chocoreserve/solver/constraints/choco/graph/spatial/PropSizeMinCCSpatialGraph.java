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
 * Propagator ensuring that the number of vertices of the smallest connected is equal to sizeMinCC
 * (cf. MIN_NCC graph property http://www.emn.fr/x-info/sdemasse/gccat/sec2.2.2.4.html#uid940).
 *
 * @author Dimitri Justeau-Allaire
 */
public class PropSizeMinCCSpatialGraph extends Propagator<Variable> {

    /* Variables */

    private SpatialGraphVar g;
    private IntVar sizeMinCC;
    private ConnectivityFinderSpatialGraph GLBCCFinder, GUBCCFinder;

    /* Constructor */

    public PropSizeMinCCSpatialGraph(SpatialGraphVar graph, IntVar sizeMinCC) {
        super(new Variable[]{graph, sizeMinCC}, PropagatorPriority.QUADRATIC, false);
        this.g = graph;
        this.sizeMinCC = sizeMinCC;
        this.GLBCCFinder = new ConnectivityFinderSpatialGraph(g.getGLB());
        this.GUBCCFinder = new ConnectivityFinderSpatialGraph(g.getGUB());
    }

    /* Methods */

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ALL_EVENTS;
    }

    /**
     * @param nbNodesT The number of T-vertices.
     * @param nbNodesU The number of U-vertices.
     * @return The lower bound of the graph variable MIN_NCC property.
     * Beware that this.GLBCCFinder.findAllCC() and this.GLBCCFinder.findCCSizes() must be called before.
     */
    private int getLBMinNCC(int nbNodesT, int nbNodesU) {
        if (nbNodesT == 0) {
            return 0;
        } else {
            if (nbNodesU > 0) {
                return 1;
            } else {
                return this.GLBCCFinder.getSizeMinCC();
            }
        }
    }

    /**
     * @param nbNodesT The number of T-vertices.
     * @return The upper bound of the graph variable MIN_NCC property.
     * Beware that this.GUBCCFinder.findAllCC() and this.GUBCCFinder.findCCSizes() must be called before.
     */
    private int getUBMinNCC(int nbNodesT) {
        if (nbNodesT > 0) {
            return getGUBMandatoryCCs().stream().mapToInt(cc -> GUBCCFinder.getSizeCC()[cc]).min().getAsInt();
        } else {
            return this.GUBCCFinder.getSizeMaxCC();
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // Find CCs and their sizes
        this.GLBCCFinder.findAllCC();
        this.GUBCCFinder.findAllCC();
        // Compute |V_T|, |V_U| and |V_TU|
        int nbNodesT = g.getMandatoryNodes().size();
        int nbNodesTU = g.getPotentialNodes().size();
        int nbNodesU = nbNodesTU - nbNodesT;
        // Compute MIN_NCC(g) lower and upper bounds from g
        int minNCC_LB = getLBMinNCC(nbNodesT, nbNodesU);
        int minNCC_UB = getUBMinNCC(nbNodesT);
        // 1. Trivial case
        if (sizeMinCC.getLB() > nbNodesTU) {
            fails();
        }
        // 2. Trivial case TODO: How to properly get |E_TU| ?
        // 3.
        if (minNCC_UB < sizeMinCC.getLB()) {
            fails();
        }
        // 4.
        if (minNCC_LB > sizeMinCC.getUB()) {
            fails();
        }
        // 5.
        if (sizeMinCC.getLB() < minNCC_LB) {
            sizeMinCC.updateLowerBound(minNCC_LB, this);
        }
        // 6.
        if (sizeMinCC.getUB() > minNCC_UB) {
            sizeMinCC.updateUpperBound(minNCC_UB, this);
        }
        // 7.
        for (int cc : getGUBOptionalCCs()) {
            if (GUBCCFinder.getSizeCC()[cc] < sizeMinCC.getLB()) {
                int i = GUBCCFinder.getCCFirstNode()[cc];
                while (i != -1) {
                    g.removeNode(i, this);
                    i = GUBCCFinder.getCCNextNode()[i];
                }
            }
        }
        // 8.
        boolean recomputeMinNCC_LB = false;
        Set<Integer> GUBMandatoryCCs = getGUBMandatoryCCs();
        if (minNCC_LB < sizeMinCC.getLB()) {
            // a
            for (int cc : GUBMandatoryCCs) {
                if (GUBCCFinder.getSizeCC()[cc] == sizeMinCC.getLB()) {
                    int i = GUBCCFinder.getCCFirstNode()[cc];
                    while (i != -1) {
                        g.enforceNode(i, this);
                        i = GUBCCFinder.getCCNextNode()[i];
                    }
                    sizeMinCC.instantiateTo(sizeMinCC.getLB(), this);
                }
            }
            // b.
            for (int cc = 0; cc < GLBCCFinder.getNBCC(); cc++) {
                if (GLBCCFinder.getSizeCC()[cc] < sizeMinCC.getLB()) {
                    Map<Integer, Set<Integer>> ccPotentialNeighbors = getGLBCCPotentialNeighbors(cc);
                    if (ccPotentialNeighbors.size() == 1) {
                        int i = ccPotentialNeighbors.keySet().iterator().next();
                        Set<Integer> outNeighbors = ccPotentialNeighbors.get(i);
                        if (outNeighbors.size() == 1) {
                            int j = outNeighbors.iterator().next();
                            g.enforceNode(j, this);
//							g.enforceArc(i, j, this);
                            recomputeMinNCC_LB = true;
                        }
                    }
                    if (ccPotentialNeighbors.size() > 1) {
                        Set<Integer> outNeighbors = new HashSet<>();
                        for (Set<Integer> i : ccPotentialNeighbors.values()) {
                            outNeighbors.addAll(i);
                        }
                        if (outNeighbors.size() == 1) {
                            int j = outNeighbors.iterator().next();
                            g.enforceNode(j, this);
                            recomputeMinNCC_LB = true;
                        }
                    }
                }
            }
        }
        // 9.
        if (recomputeMinNCC_LB) {
            // Recompute minNCC_LB
            this.GLBCCFinder.findAllCC();
            nbNodesT = g.getMandatoryNodes().size();
            nbNodesU = nbNodesTU - nbNodesT;
            minNCC_LB = getLBMinNCC(nbNodesT, nbNodesU);
            // Repeat 4. and 5.
            if (minNCC_LB > sizeMinCC.getUB()) {
                fails();
            }
            if (sizeMinCC.getLB() < minNCC_LB) {
                sizeMinCC.updateLowerBound(minNCC_LB, this);
            }
        }
        // 10.
        if (minNCC_UB > sizeMinCC.getUB()) {
            // a.
            if (sizeMinCC.getUB() == 0) {
                for (int i : g.getPotentialNodes()) {
                    g.removeNode(i, this);
                }
            }
            // b.
//			if (sizeMinCC.getUB() == 1 && nbNodesU == 1 && GLBCCFinder.getSizeMinCC() > 1) {
//				for (int i : g.getPotentialNodes()) {
//					if (g.getMandatoryNodes().contains(i)) {
//						g.enforceNode(i, this);
//						for (int j : g.getPotNeighOf(i)) {
//							if (i != j) {
//								g.removeArc(i, j, this);
//							}
//						}
//						break;
//					}
//				}
//			}
        }
        // 11 and 12
//        int nbCandidates = 0;
//        int candidate1 = -1;
//        int candidate2 = -1;
//        int s1 = -1;
//        int s2 = -1;
//        for (int cc = 0; cc < GLBCCFinder.getNBCC(); cc++) {
//            int size = GLBCCFinder.getSizeCC()[cc];
//            if (size <= sizeMinCC.getUB() && size >= sizeMinCC.getLB()) {
//                if (nbCandidates == 0) {
//                    candidate1 = cc;
//                    s1 = size;
//                }
//                if (nbCandidates == 1) {
//                    candidate2 = cc;
//                    s2 = size;
//                }
//                nbCandidates++;
//            }
//            if (nbCandidates > 2) {
//                break;
//            }
//        }
        // 11.
//		if (nbCandidates == 1 && nbNodesU == 0) {
//			Map<Integer, Set<Integer>> ccNeighbors = getGLBCCPotentialNeighbors(candidate1);
//			for (int i : ccNeighbors.keySet()) {
//				for (int j : ccNeighbors.get(i)) {
//					g.removeArc(i, j, this);
//				}
//			}
//		}
        // 12.
//		if (nbCandidates == 2 && nbNodesU == 0 && (s1 + s2 > sizeMinCC.getUB())) {
//			Map<Integer, Set<Integer>> cc1Neighbors = getGLBCCPotentialNeighbors(candidate1);
//			for (int i : cc1Neighbors.keySet()) {
//				for (int j : cc1Neighbors.get(i)) {
//					if (GLBCCFinder.getNodeCC()[j] == candidate2) {
//						g.removeArc(i, j, this);
//					}
//				}
//			}
//		}
    }

    /**
     * @return The indices of the mandatory GUB CCs (i.e. containing at least one node in GLB).
     */
    private Set<Integer> getGUBMandatoryCCs() {
        Set<Integer> mandatoryCCs = new HashSet<>();
        for (int cc = 0; cc < this.GUBCCFinder.getNBCC(); cc++) {
            int i = GUBCCFinder.getCCFirstNode()[cc];
            while (i != -1) {
                if (g.getMandatoryNodes().contains(i)) {
                    mandatoryCCs.add(cc);
                    break;
                }
                i = GUBCCFinder.getCCNextNode()[i];
            }
        }
        return mandatoryCCs;
    }

    /**
     * @return The indices of the optional GUB CCs (i.e. not containing any node in GLB).
     */
    private Set<Integer> getGUBOptionalCCs() {
        Set<Integer> optionalCCs = new HashSet<>();
        for (int cc = 0; cc < this.GUBCCFinder.getNBCC(); cc++) {
            int currentNode = GUBCCFinder.getCCFirstNode()[cc];
            boolean addCC = true;
            while (currentNode != -1) {
                if (g.getMandatoryNodes().contains(currentNode)) {
                    addCC = false;
                    break;
                }
                currentNode = GUBCCFinder.getCCNextNode()[currentNode];
            }
            if (addCC) {
                optionalCCs.add(cc);
            }
        }
        return optionalCCs;
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
     * Retrieve the potential CC neighbors (i.e. in GUB and not in the CC) of a GLB CC.
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
        // Compute |V_T|, |V_U| and |V_TU|
        int nbNodesT = g.getMandatoryNodes().size();
        int nbNodesTU = g.getPotentialNodes().size();
        int nbNodesU = nbNodesTU - nbNodesT;
        // Compute MIN_NCC(g) lower bound from g
        int minNCC_LB = getLBMinNCC(nbNodesT, nbNodesU);
        // Compute MIN_NCC(g) upper bound from g
        int minNCC_UB = getUBMinNCC(nbNodesT);
        // Check entailment
        if (minNCC_UB < sizeMinCC.getLB() || minNCC_LB > sizeMinCC.getUB()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            if (minNCC_LB == sizeMinCC.getValue()) {
                return ESat.TRUE;
            } else {
                return ESat.FALSE;
            }
        }
        return ESat.UNDEFINED;
    }
}
