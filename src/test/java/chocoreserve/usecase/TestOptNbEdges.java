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

package chocoreserve.usecase;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.graph.spatial.PropNbArcsSpatialGraph;
import chocoreserve.solver.constraints.spatial.NbEdges;
import chocoreserve.solver.region.ComposedRegion;
import chocoreserve.solver.region.Region;
import chocoreserve.solver.variable.SpatialGraphVar;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TestOptNbEdges {

    @Test
    public void testThreeRegions() {

        int nbRows = 11;
        int nbCols = 11;

        RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);

        int[] forestPixels = IntStream.range(0, nbRows).map(i -> nbCols * i + (nbCols - 1) / 2).toArray();
        System.out.println(Arrays.toString(forestPixels));
        int[] reforestAUB = new int[(nbRows * (nbCols - 1)) / 2];
        for (int i = 0; i < nbRows; i++) {
            for (int j = 0; j < (nbCols - 1) / 2; j++) {
                reforestAUB[i * (nbCols - 1) / 2 + j] = i * nbCols + j;
            }
        }
        System.out.println(Arrays.toString(reforestAUB));
        int[] reforestBUB = new int[(nbRows * (nbCols - 1)) / 2];
        for (int i = 0; i < nbRows; i++) {
            for (int j = (nbCols - 1) / 2 + 1; j < nbCols; j++) {
                reforestBUB[i * (nbCols - 1) / 2 + (j - (nbCols - 1) / 2 - 1)] = i * nbCols + j;
            }
        }
        System.out.println(Arrays.toString(reforestBUB));
        int[] outUB = ArrayUtils.concat(reforestAUB, reforestBUB);

        Region forest = new Region(
                "forest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                forestPixels,
                forestPixels
        );
        Region reforestA = new Region(
                "reforestA",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                reforestAUB
        );
        Region reforestB = new Region(
                "reforestB",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                reforestBUB
        );
        Region out = new Region(
                "out",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                outUB
        );

        ComposedRegion potentialForest = new ComposedRegion("potentialForest", forest, reforestA, reforestB);

        ReserveModel reserveModel = new ReserveModel(
                grid,
                new Region[] {out, forest, reforestA, reforestB},
                new ComposedRegion[] {potentialForest}
        );

        GraphModel model = reserveModel.getChocoModel();

        reserveModel.nbConnectedComponents(reforestA, 1, 1).post();
        reserveModel.nbConnectedComponents(reforestB, 1, 1).post();

        reserveModel.maxDiameterSpatial(reforestA, 3).post();
        reserveModel.maxDiameterSpatial(reforestB, 3).post();

        SpatialGraphVar potentialForestGraphVar = potentialForest.getSetVar();

        System.out.println(potentialForestGraphVar.getGLB().getNodes().getClass());

        reserveModel.nbConnectedComponents(potentialForest, 1, 1).post();

        NbEdges cNbEdge = new NbEdges(reserveModel, potentialForest);

        IntVar nbEdges = cNbEdge.nbEdges;

        cNbEdge.post();

        Solver solver = model.getSolver();
        solver.setSearch(Search.domOverWDegSearch(reserveModel.getSites()));
        solver.showStatistics();
        solver.plugMonitor((IMonitorSolution) () -> {
            reserveModel.printSolution();
        });
        solver.findOptimalSolution(nbEdges, true);
    }

    @Test
    public void testThreeRegionsSplit() {

        int nbRows = 11;
        int nbCols = 11;

        RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);

        int[] forestPixels = IntStream.range(0, nbRows).map(i -> nbCols * i + (nbCols - 1) / 2).toArray();
        System.out.println(Arrays.toString(forestPixels));
        int[] reforestAUB = new int[(nbRows * (nbCols - 1)) / 2];
        for (int i = 0; i < nbRows; i++) {
            for (int j = 0; j < (nbCols - 1) / 2; j++) {
                reforestAUB[i * (nbCols - 1) / 2 + j] = i * nbCols + j;
            }
        }
        System.out.println(Arrays.toString(reforestAUB));
        int[] reforestBUB = new int[(nbRows * (nbCols - 1)) / 2];
        for (int i = 0; i < nbRows; i++) {
            for (int j = (nbCols - 1) / 2 + 1; j < nbCols; j++) {
                reforestBUB[i * (nbCols - 1) / 2 + (j - (nbCols - 1) / 2 - 1)] = i * nbCols + j;
            }
        }
        System.out.println(Arrays.toString(reforestBUB));
        int[] outUB = ArrayUtils.concat(reforestAUB, reforestBUB);

        Region forest = new Region(
                "forest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                forestPixels,
                forestPixels
        );
        Region reforestA = new Region(
                "reforestA",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                reforestAUB
        );
        Region reforestB = new Region(
                "reforestB",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                reforestBUB
        );
        Region out = new Region(
                "out",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                outUB
        );

        ComposedRegion potentialForestA = new ComposedRegion("potentialForest", forest, reforestA);
        ComposedRegion potentialForestB = new ComposedRegion("potentialForest", forest, reforestB);

        ReserveModel reserveModel = new ReserveModel(
                grid,
                new Region[] {out, forest, reforestA, reforestB},
                new ComposedRegion[] {potentialForestA, potentialForestB}
        );

        GraphModel model = reserveModel.getChocoModel();

        reserveModel.nbConnectedComponents(reforestA, 1, 1).post();
        reserveModel.nbConnectedComponents(reforestB, 1, 1).post();

        reserveModel.maxDiameterSpatial(reforestA, 3).post();
        reserveModel.maxDiameterSpatial(reforestB, 3).post();

        SpatialGraphVar potentialForestGraphVarA = potentialForestA.getSetVar();
        SpatialGraphVar potentialForestGraphVarB = potentialForestB.getSetVar();

        reserveModel.nbConnectedComponents(potentialForestA, 1, 1).post();
        reserveModel.nbConnectedComponents(potentialForestB, 1, 1).post();

        IntVar nbEdgesPotA = model.intVar("nbEdgesPotA", 0, potentialForestGraphVarA.getNbMaxNodes() * potentialForestGraphVarA.getNbMaxNodes());
        PropNbArcsSpatialGraph propNbEdgesPotA = new PropNbArcsSpatialGraph(potentialForestGraphVarA, nbEdgesPotA);
        model.post(new Constraint("PropNbEdgesPotA", propNbEdgesPotA));

        IntVar nbEdgesPotB = model.intVar("nbEdgesPotB", 0, potentialForestGraphVarB.getNbMaxNodes() * potentialForestGraphVarB.getNbMaxNodes());
        PropNbArcsSpatialGraph propNbEdgesPotB = new PropNbArcsSpatialGraph(potentialForestGraphVarB, nbEdgesPotB);
        model.post(new Constraint("PropNbEdgesPotB", propNbEdgesPotB));

        IntVar nbEdgesPot = model.intVar("nbEdgesPot", 0, 11 * 11);

        model.arithm(nbEdgesPotA, "+", nbEdgesPotB, "=", nbEdgesPot).post();

        Solver solver = model.getSolver();
        solver.setSearch(Search.domOverWDegSearch(reserveModel.getSites()));
        solver.showStatistics();

        solver.plugMonitor((IMonitorSolution) () -> {
            reserveModel.printSolution();
            model.arithm(nbEdgesPotA, ">=", nbEdgesPotA.getLB()).post();
            model.arithm(nbEdgesPotB, ">=", nbEdgesPotB.getLB()).post();
        });

        solver.findOptimalSolution(nbEdgesPot, true);
    }


    @Test
    public void testTwoRegions() {

        int nbRows = 5;
        int nbCols = 5;

        RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);

        int[] forestPixels = IntStream.range(0, nbRows).map(i -> nbCols * i + (nbCols - 1) / 2).toArray();
        System.out.println(Arrays.toString(forestPixels));
        int[] reforestAUB = new int[(nbRows * (nbCols - 1)) / 2];
        for (int i = 0; i < nbRows; i++) {
            for (int j = 0; j < (nbCols - 1) / 2; j++) {
                reforestAUB[i * (nbCols - 1) / 2 + j] = i * nbCols + j;
            }
        }
        System.out.println(Arrays.toString(reforestAUB));
        int[] reforestBUB = new int[(nbRows * (nbCols - 1)) / 2];
        for (int i = 0; i < nbRows; i++) {
            for (int j = (nbCols - 1) / 2 + 1; j < nbCols; j++) {
                reforestBUB[i * (nbCols - 1) / 2 + (j - (nbCols - 1) / 2 - 1)] = i * nbCols + j;
            }
        }
        System.out.println(Arrays.toString(reforestBUB));
        int[] outUB = ArrayUtils.concat(reforestAUB, reforestBUB);

        Region forest = new Region(
                "forest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                forestPixels,
                forestPixels
        );
        Region reforestA = new Region(
                "reforestA",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                reforestAUB
        );
        Region out = new Region(
                "out",
                Neighborhoods.FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                outUB
        );

        ComposedRegion potentialForest = new ComposedRegion("potentialForest", forest, reforestA);


        ReserveModel reserveModel = new ReserveModel(
                grid,
                new Region[] {out, reforestA, forest},
                new ComposedRegion[] {potentialForest}
        );

        reserveModel.nbConnectedComponents(reforestA, 1, 1).post();

        reserveModel.maxDiameterSpatial(reforestA, 3).post();

        GraphModel model = reserveModel.getChocoModel();

        SpatialGraphVar potentialForestGraphVar = potentialForest.getSetVar();

        IntVar nbEdges = model.intVar("nbEdges", 0, potentialForestGraphVar.getNbMaxNodes() * potentialForestGraphVar.getNbMaxNodes());
        PropNbArcsSpatialGraph propNbEdges = new PropNbArcsSpatialGraph(potentialForestGraphVar, nbEdges);
        model.post(new Constraint("PropNbEdges", propNbEdges));

//        model.nbConnectedComponents(potentialForestGraphVar, model.intVar(1)).post();

//        IntVar nbCC = model.intVar(0, 20*20);
//        model.nbConnectedComponents(potentialForestGraphVar, nbCC).post();

        Solver solver = model.getSolver();

        solver.setSearch(Search.domOverWDegSearch(reserveModel.getSites()));

        solver.showStatistics();

//        solver.plugMonitor(new IMonitorOpenNode() {
//            @Override
//            public void afterOpenNode() {
//                System.out.println(" --- ");
//                reserveModel.printGrid();
//                System.out.println(" --- ");
//            }
//        });

//        solver.showDecisions();
//        solver.showContradiction();

//        solver.outputSearchTreeToGraphviz("/home/djusteau/testA.dot");

        solver.plugMonitor((IMonitorSolution) () -> {
            reserveModel.printSolution();
        });

        solver.findOptimalSolution(nbEdges, true);
//        solver.findAllSolutions();

    }
}
