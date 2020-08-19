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

package chocoreserve.solver.variable;

import chocoreserve.grid.Grid;
import chocoreserve.grid.neighborhood.INeighborhood;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ConstraintsName;
import org.chocosolver.solver.constraints.set.PropCardinality;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.delta.SetDelta;
import org.chocosolver.solver.variables.delta.monitor.SetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.solver.variables.impl.AbstractVariable;
import org.chocosolver.solver.variables.impl.scheduler.SetEvtScheduler;
import org.chocosolver.util.iterators.EvtScheduler;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.objects.setDataStructures.Set_ReadOnly;


/**
 * Extension of SetVar representing a spatial graph. The neighborhood relation is intentional and edges represent
 * a adjacency relation, i.e. two adjacent nodes according to the neighborhood defining cannot be disconnected.
 */
public class SpatialGraphVar extends AbstractVariable implements SetVar {

    // From set Var Impl - START //
    protected final ISet lb, ub, lbReadOnly, ubReadOnly;
    protected SetDelta delta;
    protected boolean reactOnModification;
    protected IntVar cardinality = null;
    // From set Var Impl - END //
    protected Grid grid;
    protected INeighborhood neighborhood;
    protected UndirectedGraph GLB;
    protected UndirectedGraph GUB;

    public SpatialGraphVar(String name, int[] ker, SetType kerType, int[] env, SetType envType, Model model, Grid grid, INeighborhood neighborhood, boolean ubDecr) {
        super(name, model);
        this.grid = grid;
        this.neighborhood = neighborhood;
        this.GLB = neighborhood.getPartialGraph(grid, getModel(), ker, kerType);
        this.GUB = neighborhood.getPartialGraphUB(grid, getModel(), env, envType, ubDecr);
        // Adapted from set Var Impl - START //
        lb = GLB.getNodes();
        ub = GUB.getNodes();
        lbReadOnly = new Set_ReadOnly(lb);
        ubReadOnly = new Set_ReadOnly(ub);
        // Adapted from set Var Impl - END //
    }

    public SpatialGraphVar(String name, int[] ker, SetType kerType, int[] env, SetType envType, Model model, Grid grid, INeighborhood neighborhood) {
        this(name, ker, kerType, env, envType, model, grid, neighborhood, false);
    }


    public Grid getGrid() {
        return grid;
    }

    public INeighborhood getNeighborhood() {
        return neighborhood;
    }

    public ISet getMandNeighOf(int x) {
        return GLB.getSuccOrNeighOf(x);
    }

    public ISet getPotNeighOf(int x) {
        return GUB.getSuccOrNeighOf(x);
    }

    public int getNbMaxNodes() {
        return grid.getNbCells();
    }

    public ISet getMandatoryNodes() {
        return getLB();
    }

    public ISet getPotentialNodes() {
        return getUB();
    }

    public boolean removeNode(int x, ICause cause) throws ContradictionException {
        return remove(x, cause);
    }

    public boolean enforceNode(int x, ICause cause) throws ContradictionException {
        return force(x, cause);
    }


    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public boolean isInstantiated() {
        return ub.size() == lb.size();
    }

    @Override
    public ISet getLB() {
        return lbReadOnly;
    }

    @Override
    public ISet getUB() {
        return ubReadOnly;
    }

    public UndirectedGraph getGLB() {
        return GLB;
    }

    public UndirectedGraph getGUB() {
        return GUB;
    }

    @Override
    public IntVar getCard() {
        if (!hasCard()) {
            int ubc = ub.size(), lbc = lb.size();
            if (ubc == lbc) cardinality = model.intVar(ubc);
            else {
                cardinality = model.intVar(name + ".card", lbc, ubc);
                new Constraint(ConstraintsName.SETCARD, new PropCardinality(this, cardinality)).post();
            }
        }
        return cardinality;
    }

    @Override
    public boolean hasCard() {
        return cardinality != null;
    }

    @Override
    public void setCard(IntVar card) {
        if (!hasCard()) {
            cardinality = card;
            new Constraint(ConstraintsName.SETCARD, new PropCardinality(this, card)).post();
        } else {
            model.arithm(cardinality, "=", card).post();
        }
    }

    @Override
    public boolean force(int element, ICause cause) throws ContradictionException {
        assert cause != null;
        if (!ub.contains(element)) {
            contradiction(cause, "");
            return true;
        }
        if (getGLB().addNode(element)) {
            ISet nei = GUB.getSuccOrNeighOf(element);
            for (int i : nei) {
                if (lb.contains(i)) {
                    GLB.addEdge(element, i);
                }
            }
            if (reactOnModification) {
                delta.add(element, SetDelta.LB, cause);
            }
            SetEventType e = SetEventType.ADD_TO_KER;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(int element, ICause cause) throws ContradictionException {
        assert cause != null;
        if (lb.contains(element)) {
            contradiction(cause, "");
            return true;
        }
        if (getGUB().removeNode(element)) {
            int[] nei = GUB.getSuccOrNeighOf(element).toArray();
            for (int i : nei) {
                GUB.removeEdge(i, element);
            }
            if (reactOnModification) {
                delta.add(element, SetDelta.UB, cause);
            }
            SetEventType e = SetEventType.REMOVE_FROM_ENVELOPE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public boolean instantiateTo(int[] value, ICause cause) throws ContradictionException {
        boolean changed = !isInstantiated();
        for (int i : value) {
            force(i, cause);
        }
        if (lb.size() != value.length) {
            contradiction(cause, "");
        }
        if (ub.size() != value.length) {
            for (int i : getUB()) {
                if (!getLB().contains(i)) {
                    remove(i, cause);
                }
            }
        }
        return changed;
    }

    @Override
    public SetDelta getDelta() {
        return delta;
    }

    @Override
    public int getTypeAndKind() {
        return VAR | SET;
    }

    @Override
    protected EvtScheduler createScheduler() {
        return new SetEvtScheduler();
    }

    @Override
    public String toString() {
        if (isInstantiated()) {
            return getName() + " = " + getLB().toString();
        } else {
            return getName() + " = [" + getLB() + ", " + getUB() + "]";
        }
    }

    @Override
    public void createDelta() {
        if (!reactOnModification) {
            reactOnModification = true;
            delta = new SetDelta(model.getEnvironment());
        }
    }

    @Override
    public SetDeltaMonitor monitorDelta(ICause propagator) {
        createDelta();
        return new SetDeltaMonitor(delta, propagator);
    }
}
