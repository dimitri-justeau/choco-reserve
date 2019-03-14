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

package chocoreserve.solver.constraints.choco;

import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 */
public class PropCustomChanneling extends Propagator<Variable> {

    private IntVar[] sites;
    private SetVar O, B, C;
    private GraphVar GO, GB, GC;

    public PropCustomChanneling(IntVar[] sites, SetVar O, SetVar B, SetVar C, GraphVar GO, GraphVar GB, GraphVar GC) {
        super(
                Stream.concat(Stream.of(sites), Stream.of(new Variable[] {O, B, C, GO, GB, GC}))
                        .toArray(Variable[]::new),
                PropagatorPriority.LINEAR,
                false
        );
        this.sites = sites;
        this.O = O;
        this.B = B;
        this.C = C;
        this.GO = GO;
        this.GB = GB;
        this.GC = GC;
    }

    @Override
    public void propagate(int evtMask) throws ContradictionException {
        for (int i = 0; i < sites.length; i++) {
            // 1 - Check instantiation
            int mInt = getInstInts(i);
            int mSet = getInstSets(i);
            int mGraph = getInstGraphs(i);
            // Continue if instantiated or in one LB
            if (mInt > 0 || mSet > 0 || mGraph > 0) {
                // 1.a - In several LBs
                if (mSet > 0 && mSet != 0b001 && mSet != 0b010 && mSet != 0b100) {
                    fails();
                }
                if (mGraph > 0 && mGraph != 0b001 && mGraph != 0b010 && mGraph != 0b100) {
                    fails();
                }
                // 1.b - Different LBs or instantiation
                int or = mInt | mSet | mGraph;
                if (or != 0b001 && or != 0b010 && or != 0b100) {
                    fails();
                }
            }
            if (mInt > 0) {
                int val = sites[i].getValue();
                switch (val) {
                    case 0:
                        O.force(i, this);
                        B.remove(i, this);
                        C.remove(i, this);
                        GO.enforceNode(i, this);
                        GB.removeNode(i, this);
                        GC.removeNode(i, this);
                        break;
                    case 1:
                        O.remove(i, this);
                        B.force(i, this);
                        C.remove(i, this);
                        GO.removeNode(i, this);
                        GB.enforceNode(i, this);
                        GC.removeNode(i, this);
                        break;
                    case 2:
                        O.remove(i, this);
                        B.remove(i, this);
                        C.force(i, this);
                        GO.removeNode(i, this);
                        GB.removeNode(i, this);
                        GC.enforceNode(i, this);
                        break;
                }
                continue;
            }
            if (mSet > 0) {
                continue;
            }
            if (mGraph > 0) {
                continue;
            }
            Set<Integer> domInt = getDomInt(i);
            Set<Integer> domSet = getDomSet(i);
            Set<Integer> domGraph = getDomGraph(i);
            if (domSet.size() == 0 || domGraph.size() == 0) {
                fails();
            }
            Set<Integer> toRemove = new HashSet<>();
            for (int j = 0; j <= 2; j++) {
                if (!domInt.contains(j) || !domSet.contains(j) || !domGraph.contains(j)) {
                    toRemove.add(j);
                }
            }
            if (toRemove.contains(0)) {
                sites[i].removeValue(0, this);
                O.remove(i, this);
                GO.removeNode(i, this);
            }
            if (toRemove.contains(1)) {
                sites[i].removeValue(1, this);
                B.remove(i, this);
                GB.removeNode(i, this);
            }
            if (toRemove.contains(2)) {
                sites[i].removeValue(2, this);
                C.remove(i, this);
                GC.removeNode(i, this);
            }
        }
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < sites.length; i++) {
            // 1 - Check instantiation
            int mInt = getInstInts(i);
            int mSet = getInstSets(i);
            int mGraph = getInstGraphs(i);
            // Continue if instantiated or in one LB
            if (mInt > 0 || mSet > 0 || mGraph > 0) {
                // 1.a - In several LBs
                if (mSet > 0 && mSet != 0b001 && mSet != 0b010 && mSet != 0b100) {
                    return ESat.FALSE;
                }
                if (mGraph > 0 && mGraph != 0b001 && mGraph != 0b010 && mGraph != 0b100) {
                    return ESat.FALSE;
                }
                // 1.b - Different LBs or instantiation
                int or = mInt | mSet | mGraph;
                if (or != 0b001 && or != 0b010 && or != 0b100) {
                    return ESat.FALSE;
                }
            }
            if (mInt > 0 && mSet > 0 && mGraph > 0) {
                int val = sites[i].getValue();
                switch (val) {
                    case 0:
                        if (!O.getLB().contains(i) || B.getLB().contains(i) || C.getLB().contains(i)) {
                            return ESat.FALSE;
                        }
                        if (!GO.getLB().getNodes().contains(i) || GB.getLB().getNodes().contains(i) || GC.getLB().getNodes().contains(i)) {
                            return ESat.FALSE;
                        }
                        break;
                    case 1:
                        if (O.getLB().contains(i) || !B.getLB().contains(i) || C.getLB().contains(i)) {
                            return ESat.FALSE;
                        }
                        if (GO.getLB().getNodes().contains(i) || !GB.getLB().getNodes().contains(i) || GC.getLB().getNodes().contains(i)) {
                            return ESat.FALSE;
                        }
                        break;
                    case 2:
                        if (O.getLB().contains(i) || B.getLB().contains(i) || !C.getLB().contains(i)) {
                            return ESat.FALSE;
                        }
                        if (GO.getLB().getNodes().contains(i) || GB.getLB().getNodes().contains(i) || !GC.getLB().getNodes().contains(i)) {
                            return ESat.FALSE;
                        }
                        break;
                }
            }
        }
        return ESat.UNDEFINED;
    }

    private Set<Integer> getDomInt(int i) {
        Set<Integer> dom = new HashSet<>();
        if (sites[i].contains(0))
            dom.add(0);
        if (sites[i].contains(1))
            dom.add(1);
        if (sites[i].contains(2))
            dom.add(2);
        return dom;
    }

    private Set<Integer> getDomSet(int i) {
        Set<Integer> dom = new HashSet<>();
        if (O.getUB().contains(i))
            dom.add(0);
        if (B.getUB().contains(i))
            dom.add(1);
        if (C.getUB().contains(i))
            dom.add(2);
        return dom;
    }

    private Set<Integer> getDomGraph(int i) {
        Set<Integer> dom = new HashSet<>();
        if (GO.getUB().getNodes().contains(i))
            dom.add(0);
        if (GB.getUB().getNodes().contains(i))
            dom.add(1);
        if (GC.getUB().getNodes().contains(i))
            dom.add(2);
        return dom;
    }

    private int getInstInts(int i) {
        if (sites[i].isInstantiatedTo(0))
            return 0b100;
        if (sites[i].isInstantiatedTo(1))
            return 0b010;
        if (sites[i].isInstantiatedTo(2))
            return 0b001;
        return 0b000;
    }

    private int getInstSets(int i) {
        int mask = 0b000;
        if (O.getLB().contains(i))
            mask |= 0b100;
        if (B.getLB().contains(i))
            mask |= 0b010;
        if (C.getLB().contains(i))
            mask |= 0b001;
        return mask;
    }

    private int getInstGraphs(int i) {
        int mask = 0b000;
        if (GO.getLB().getNodes().contains(i))
            mask |= 0b100;
        if (GB.getLB().getNodes().contains(i))
            mask |= 0b010;
        if (GC.getLB().getNodes().contains(i))
            mask |= 0b001;
        return mask;
    }
}
