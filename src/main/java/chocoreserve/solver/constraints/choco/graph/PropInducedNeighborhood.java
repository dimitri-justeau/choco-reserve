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

import chocoreserve.grid.IGrid;
import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.List;

/**
 * Ensures that if two nodes are enforced then any arc (or edge) linking them in the initial GUB
 * is also enforced.
 *
 * @author Dimitri Justeau-Allaire
 */
public class PropInducedNeighborhood extends Propagator<UndirectedGraphVar> {

    /* Variables */

    private UndirectedGraphVar g;
    private UndirectedGraph initialGUB;

    /* Constructors */

    public PropInducedNeighborhood(UndirectedGraphVar g) {
        super(new UndirectedGraphVar[]{g}, PropagatorPriority.LINEAR, false);
        this.g = g;
        int n = this.g.getNbMaxNodes();
        SetType t = this.g.getUB().getType();
        this.initialGUB = new UndirectedGraph(n, t, false);
        this.g.getUB().getNodes().forEach(i -> {
            this.initialGUB.addNode(i);
            this.g.getUB().getSuccOrNeighOf(i).forEach(j -> this.initialGUB.addEdge(i, j));
        });
    }

    /* Methods */

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_NODE.getMask();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i : g.getLB().getNodes()) {
            for (int j : initialGUB.getSuccOrNeighOf(i)) {
                if (g.getMandatoryNodes().contains(j)) {
                    if (!g.getMandNeighOf(i).contains(j)) {
                        g.enforceArc(i, j, this);
                    }
                } else {
                    if (!g.getPotNeighOf(i).contains(j)) {
                        g.removeNode(j, this);
                    }
                }
            }
        }
    }

    @Override
    public ESat isEntailed() {
        for (int i : g.getLB().getNodes()) {
            for (int j : initialGUB.getSuccOrNeighOf(i)) {
                if (g.getLB().getNodes().contains(j)) {
                    if (!g.getMandNeighOf(i).contains(j)) {
                        if (!g.getPotNeighOf(i).contains(j)) {
                            return ESat.FALSE;
                        } else {
                            return ESat.UNDEFINED;
                        }
                    }
                }
            }
        }
        return ESat.TRUE;
    }
}
