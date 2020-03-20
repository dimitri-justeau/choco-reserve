/*
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package chocoreserve.solver.constraints.choco.graph;

import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

/**
 * Propagator that ensures that Nb arcs/edges belong to the final graph
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbArcsGridFourConnected extends Propagator<Variable> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected GraphVar g;
    protected IntVar k;
    protected RegularSquareGrid grid;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNbArcsGridFourConnected(GraphVar graph, IntVar k, RegularSquareGrid grid) {
        super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
        this.g = graph;
        this.k = k;
        this.grid = grid;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int nbK = 0;
        int nbE = 0;

        int cotes = 2 * (grid.getNbCols() + grid.getNbRows() - 4);
        int maxEdges = cotes * 3 + 4 * 2 + (grid.getNbRows() * grid.getNbCols() - cotes - 4) * 4;

        ISet env = g.getPotentialNodes();
        for (int i : env) {
            nbE += g.getPotSuccOrNeighOf(i).size();
            nbK += g.getMandSuccOrNeighOf(i).size();
        }
        if (!g.isDirected()) {
            nbK /= 2;
            nbE /= 2;
        }
//        System.out.println("Nb edges LB = " + nbK + " / Nb edges UB = " + nbE);
        if (k.getLB() > maxEdges) {
            fails();
        }
        filter(nbK, nbE);
    }

    private void filter(int nbK, int nbE) throws ContradictionException {
        k.updateLowerBound(nbK, this);
        k.updateUpperBound(nbE, this);
        if (k.getLB() > nbK) {
//            System.out.println("klb = " + k.getLB() + " -- nbk = " + nbK);
        }
        if (nbK != nbE && k.isInstantiated()) {
            ISet nei;
            ISet env = g.getPotentialNodes();
            if (k.getValue() == nbE) {
                for (int i : env) {
                    nei = g.getUB().getSuccOrNeighOf(i);
                    for (int j : nei) {
                        g.enforceArc(i, j, this);
                    }
                }
            }
            if (k.getValue() == nbK) {
                ISet neiKer;
                for (int i : env) {
                    nei = g.getUB().getSuccOrNeighOf(i);
                    neiKer = g.getLB().getSuccOrNeighOf(i);
                    for (int j : nei) {
                        if (!neiKer.contains(j)) {
                            g.removeArc(i, j, this);
                        }
                    }
                }
            }
        }
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vIdx == 0) {
            return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_ARC.getMask();
        } else {
            return IntEventType.boundAndInst();
        }
    }

    @Override
    public ESat isEntailed() {
        int nbK = 0;
        int nbE = 0;
        ISet env = g.getPotentialNodes();
        for (int i : env) {
            nbE += g.getUB().getSuccOrNeighOf(i).size();
            nbK += g.getLB().getSuccOrNeighOf(i).size();
        }
        if (!g.isDirected()) {
            nbK /= 2;
            nbE /= 2;
        }
        if (nbK > k.getUB() || nbE < k.getLB()) {
            return ESat.FALSE;
        }
        if (k.isInstantiated() && g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
