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

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public class PropSetCovering extends Propagator<Variable>{

    private IntVar N;
    private SetVar T;
    private Set<Integer> U;
    private Set<Integer>[] X;
    private Map<Integer, Set<Integer>> valuesInSets;
    private int[] C;

    public PropSetCovering(IntVar N, SetVar T, Set<Integer> U, Set<Integer>[] X, int[] C) {
        super(new Variable[] {N, T}, PropagatorPriority.LINEAR, false);
        this.N = N;
        this.T = T;
        this.U = U;
        this.X = X;
        this.C = C;
        this.valuesInSets = new HashMap<>();
        for (int e : U) {
            valuesInSets.put(e, new HashSet<>());
        }
    }

    public PropSetCovering(IntVar N, SetVar T, int[] U, int[][] X, int[] C) {
        this(
                N,
                T,
                Arrays.stream(U)
                        .boxed()
                        .collect(Collectors.toSet()),
                Arrays.stream(X)
                        .map(x -> Arrays.stream(x).boxed().collect(Collectors.toSet()))
                        .toArray(Set[]::new),
                C
        );
    }

    private void refValues() {
        for (int e : U) {
            valuesInSets.get(e).clear();
        }
        for (int x : T.getUB()) {
            for (int i : X[x]) {
                valuesInSets.get(i).add(x);
            }
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        refValues();
        pruneT();
        pruneN();
    }

    private void pruneT() throws ContradictionException {
        // P1 and P2
        for (int e : U) {
            if (valuesInSets.get(e).size() == 0) {
                fails();
            }
            if (valuesInSets.get(e).size() == 1) {
                int i = valuesInSets.get(e).iterator().next();
                T.force(i, this);
            }
        }
        // P'A
        // P'B
    }

    private void pruneN() throws ContradictionException {
        N.updateUpperBound(Math.min(N.getUB(), IntStream.of(T.getUB().toArray()).map(i -> C[i]).sum()), this);
        N.updateLowerBound(Math.max(N.getLB(), IntStream.of(T.getLB().toArray()).map(i -> C[i]).sum()), this);
    }

    private void computeLbScMD() {

    }

    @Override
    public ESat isEntailed() {
        Set<Integer> cover = new HashSet<>();
        for (int i : T.getLB()) {
            cover.addAll(X[i]);
        }
        if (cover.equals(U)) {
            return ESat.TRUE;
        }
        for (int j : T.getUB()) {
            if (!T.getLB().contains(j)) {
                cover.addAll(X[j]);
            }
        }
        if (!cover.equals(U)) {
            return ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }
}
