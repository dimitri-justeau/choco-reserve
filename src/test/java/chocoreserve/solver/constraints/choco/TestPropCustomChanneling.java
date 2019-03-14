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

import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.grid.regular.square.HeightConnectedSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.impl.SetVarImpl;
import org.chocosolver.util.criteria.Criterion;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 *
 */
public class TestPropCustomChanneling {

    @Test
    public void test() {
        RegularSquareGrid grid4 = new FourConnectedSquareGrid(45, 76);
        int[] universe = IntStream.range(0, grid4.getNbCells()).toArray();

        GraphModel model = new GraphModel();

        IntVar[] sites = model.intVarArray(universe.length, 0, 2); // Sites
        SetType kerSetType = SetType.BIPARTITESET;
        SetType envSetType = SetType.BIPARTITESET;
        SetVar O = new SetVarImpl(
                "core",
                new int[] {}, kerSetType,
                universe, envSetType,
                model
        );
        SetVar B = new SetVarImpl(
                "core",
                new int[] {}, kerSetType,
                universe, envSetType,
                model
        );
        SetVar C = new SetVarImpl(
                "core",
                new int[] {}, kerSetType,
                universe, envSetType,
                model
        );

        UndirectedGraphVar GO = model.graphVar(
                "GO",
                new UndirectedGraph(model, universe.length, SetType.BIPARTITESET, false), // Graph Out
                grid4.getFullGraph(model, SetType.BIPARTITESET)
        );
        UndirectedGraphVar GB = model.graphVar(
                "GB",
                new UndirectedGraph(model, universe.length, SetType.BIPARTITESET, false), // Graph Buffer
                grid4.getFullGraph(model, SetType.BIPARTITESET)
        );
        UndirectedGraphVar GC = model.graphVar(
                "GC",
                new UndirectedGraph(model, universe.length, SetType.BIPARTITESET, false), // Graph Core
                grid4.getFullGraph(model, SetType.BIPARTITESET)
        );

        PropCustomChanneling channeling = new PropCustomChanneling(sites, O, B, C, GO, GB, GC);
        model.post(new Constraint("channeling", channeling));

        // Solve
        Solver solver = model.getSolver();
        solver.setSearch(Search.domOverWDegSearch(sites));
//        solver.plugMonitor((IMonitorSolution) () -> {
//            System.out.println("   " + new String(new char[grid4.getNbCols()]).replace("\0", "_"));
//            for (int i = 0; i < grid4.getNbRows(); i++) {
//                System.out.printf("  |");
//                for (int j = 0; j < grid4.getNbCols(); j++) {
//                    if (C.getLB().contains(grid4.getIndexFromCoordinates(i, j))) {
//                        System.out.printf("#");
//                        continue;
//                    } else {
//                        if (B.getLB().contains(grid4.getIndexFromCoordinates(i, j))) {
//                            System.out.printf("+");
//                            continue;
//                        } else {
//                            if (O.getLB().contains(grid4.getIndexFromCoordinates(i, j)))
//                                System.out.printf(" ");
//                            else
//                                System.out.printf("@");
//                        }
//                    }
//                }
//                System.out.printf("\n");
//            }
//        });
        Criterion limit = () -> solver.getTimeCount() >= 10;
        solver.findAllSolutions(limit);
//        solver.solve();
    }
}
