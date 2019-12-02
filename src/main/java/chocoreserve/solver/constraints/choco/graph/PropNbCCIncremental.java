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

import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.graphsolver.util.UGVarConnectivityHelper;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.BitSet;


/**
 * Modified version of Choco graph's PropNbCC exploiting incremental graph data structure.
 */
public class PropNbCCIncremental extends Propagator<Variable> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final UndirectedGraphVar g;
    private final IntVar k;
    private final UGVarConnectivityHelper helper;
    private final BitSet visitedMin, visitedMax;
    private final int[] fifo, ccOf;

    public int nbProp;
    public long totalTime, totTimeIncr;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNbCCIncremental(UndirectedGraphVar graph, IntVar k) {
        super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
        this.g = graph;
        this.k = k;
        this.helper = new UGVarConnectivityHelper(g);
        this.visitedMin = new BitSet(g.getNbMaxNodes());
        this.visitedMax = new BitSet(g.getNbMaxNodes());
        this.fifo = new int[g.getNbMaxNodes()];
        this.ccOf = new int[g.getNbMaxNodes()];
        nbProp = 0; totalTime = 0; totTimeIncr = 0;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        nbProp ++;
        // trivial case
        k.updateBounds(0, g.getPotentialNodes().size(), this);
        if (k.getUB() == 0) {
            for (int i : g.getPotentialNodes()) g.removeNode(i, this);
            return;
        }

        // bound computation
        int min = minCC();
        int max = maxCC();

        k.updateLowerBound(min, this);
        k.updateUpperBound(max, this);

        // The number of CC cannot increase :
        // - remove unreachable nodes
        // - force articulation points and bridges
        if(min != max) {
            if (k.getUB() == min) {

                // 1 --- remove unreachable nodes
                int n = g.getNbMaxNodes();
                for (int o = visitedMin.nextClearBit(0); o < n; o = visitedMin.nextClearBit(o + 1)) {
                    g.removeNode(o, this);
                }

                ISet mNodes = g.getMandatoryNodes();
                if (mNodes.size() >= 2) {

                    helper.findMandatoryArticulationPointsAndBridges();

                    // 2 --- enforce articulation points that link two mandatory nodes
                    for (int ap : helper.getArticulationPoints()) {
                        g.enforceNode(ap, this);
                    }

                    // 3 --- enforce isthma that link two mandatory nodes (current version is bugged)
                    TIntArrayList brI = helper.getBridgeFrom();
                    TIntArrayList brJ = helper.getBridgeTo();
                    for (int k = 0; k < brI.size(); k++) {
                        int i = brI.get(k);
                        int j = brJ.get(k);
                        if (mNodes.contains(i) && mNodes.contains(j)) {
                            g.enforceArc(i, j, this);
                        }
                    }
                }
            }
            // a maximal number of CC is required : remaining nodes will be singleton
            else if(k.getLB() == max){
                // --- transform every potential node into a mandatory isolated node
                ISet mNodes = g.getMandatoryNodes();
                for(int i:g.getPotentialNodes()){
                    if(!mNodes.contains(i)){
                        for(int j:g.getPotNeighOf(i)){
                            g.removeArc(i,j,this);
                        }
                        g.enforceNode(i,this);
                    }
                }
                // --- remove edges between mandatory nodes that would merge 2 CC
                // note that it can happen that 2 mandatory node already belong to the same CC
                // if so the edge should not be filtered
                for(int i:g.getPotentialNodes()){
                    for(int j:g.getPotNeighOf(i)){
                        if(ccOf[i] != ccOf[j]) {
                            g.removeArc(i,j,this);
                        }
                    }
                }
            }
        }
    }

    private int minCC() {
        int min = 0;
//        int alt = 0;
//        if (g.getUB() instanceof UndirectedGraphDecrementalCC) {
//            UndirectedGraphDecrementalCC ub = (UndirectedGraphDecrementalCC) g.getUB();
//            Set<Integer> s = new HashSet<>();
//            for (int i : g.getMandatoryNodes()) {
//                int cc = ub.cc.quickGet(i);
//                if (cc != -1 && !s.contains(cc)) {
//                    s.add(cc);
//                }
//            }
//            alt = s.size();
//        }
        visitedMin.clear();
        for (int i : g.getMandatoryNodes().toArray()) {
            if (!visitedMin.get(i)) {
                helper.exploreFrom(i, visitedMin);
                min++;
            }
        }
//        if (min != alt) {
//            System.out.println("--- " + (alt - min));
//        }
        return min;
    }

    private int maxCC() {
        int nbK = 0;
        if (g.getLB() instanceof UndirectedGraphIncrementalCC) {
            UndirectedGraphIncrementalCC lb = (UndirectedGraphIncrementalCC) g.getLB();
            nbK = lb.getNbCC();
        } else {
            visitedMax.clear();
            for (int i : g.getMandatoryNodes().toArray()) {
                if (!visitedMax.get(i)) {
                    exploreLBFrom(i, visitedMax);
                    nbK++;
                }
            }
        }
        int delta = g.getPotentialNodes().size() - g.getMandatoryNodes().size();
        return nbK + delta;
    }

    private void exploreLBFrom(int root, BitSet visited) {
        int first = 0;
        int last = 0;
        int i = root;
        fifo[last++] = i;
        visited.set(i);
        ccOf[i] = root; // mark cc of explored node
        while (first < last) {
            i = fifo[first++];
            for (int j : g.getMandNeighOf(i)) { // mandatory edges only
                if (!visited.get(j)) {
                    visited.set(j);
                    ccOf[j] = root; // mark cc of explored node
                    fifo[last++] = j;
                }
            }
        }
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public ESat isEntailed() {
        if (k.getUB() < minCC() || k.getLB() > maxCC()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
