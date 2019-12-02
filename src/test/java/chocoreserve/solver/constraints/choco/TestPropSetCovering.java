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

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test class for Set Covering propagator.
 */
public class TestPropSetCovering {

    /**
     * Success test, all costs are equal to one.
     * Check every found solution, and then check that every solution were found.
     */
    @Test
    public void testSuccess1() {
        Set<Integer> U_set = IntStream.range(0, 10).boxed().collect(Collectors.toSet());
        int[] U = U_set.stream().mapToInt(i -> i).toArray();
        int[][] X = new int[][] {
                {0, 1, 2},
                {2, 3, 5},
                {7, 8, 9},
                {4, 2, 3, 6},
                {1, 2},
                {7, 8, 1}
        };
        int[] C = IntStream.range(0, X.length).map(i -> 1).toArray();
        Model model = new Model();
        SetVar T = model.setVar(new int[] {}, IntStream.range(0, X.length).toArray());
        Solver solver = model.getSolver();
        IntVar N = model.intVar(0, X.length);
        Constraint SC = new Constraint("SC", new PropSetCovering(N, T, U, X, C));
        model.post(SC);
        Set<Set<Integer>> solutions = new HashSet<>();
        if (solver.solve()) {
            do {
                Set<Integer> union = new HashSet<>();
                for (int i : T.getValue()) {
                    union.addAll(IntStream.of(X[i]).boxed().collect(Collectors.toList()));
                }
                Assert.assertEquals(U_set, union);
                solutions.add(
                        IntStream.of(T.getValue().toArray())
                            .boxed()
                            .collect(Collectors.toSet())
                );
            } while (solver.solve());
        } else {
            Assert.fail();
        }
        // Now enumerate every subset of T and check that every subset that was not found as a solution by
        // the solver is actually not a solution.
        for (int i = 0; i < (1 << X.length); i++) {
            Set<Integer> current = new HashSet<>();
            for (int j = 0; j < X.length; j++) {
                if ((i & (1 << j)) > 0) {
                    current.add(j);
                }
            }
            if (!solutions.contains(current)) {
                Set<Integer> union = new HashSet<>();
                for (int v : current) {
                    union.addAll(IntStream.of(X[v]).boxed().collect(Collectors.toList()));
                }
                Assert.assertFalse(U_set.equals(union));
            }
        }
    }

    /**
     * Success test, all costs are equal to one.
     * Finds the smallest covering (= 3).
     */
    @Test
    public void testFindOptimal() {
        Set<Integer> U_set = new HashSet<>(IntStream.range(0, 10).boxed().collect(Collectors.toList()));
        int[] U = U_set.stream().mapToInt(i -> i).toArray();
        int[][] X = new int[][] {
                {0, 1, 2, 3, 4},
                {7, 6, 5},
                {8, 9},
                {4, 2, 3, 6},
                {1, 2, 9, 5},
                {7, 8, 1},
                {3, 4}
        };
        int[] C = IntStream.range(0, X.length).map(i -> 1).toArray();
        Model model = new Model();
        SetVar T = model.setVar(new int[] {}, IntStream.range(0, X.length).toArray());
        Solver solver = model.getSolver();
        IntVar N = model.intVar(0, X.length);
        Constraint SC = new Constraint("SC", new PropSetCovering(N, T, U, X, C));
        model.post(SC);
        Solution best = solver.findOptimalSolution(N, false);
        try {
            best.restore();
            Assert.assertEquals(3, N.getValue());
            Set<Integer> values = IntStream.of(T.getValue().toArray())
                    .boxed()
                    .collect(Collectors.toSet());
            Set<Integer> expected = IntStream.of(new int[] { 0, 1, 2 })
                    .boxed()
                    .collect(Collectors.toSet());
            Assert.assertEquals(expected, values);
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }
}
