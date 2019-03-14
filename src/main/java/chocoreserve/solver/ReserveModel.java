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

package chocoreserve.solver;

import chocoreserve.exception.ModelNotInstantiatedError;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.constraints.IReserveConstraintFactory;
import chocoreserve.solver.constraints.choco.graph.PropInducedNeighborhood;
import chocoreserve.solver.feature.Feature;
import chocoreserve.solver.feature.IFeatureFactory;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.cstrs.channeling.nodes.PropNodeSetChannel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.set.PropIntChannel;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.impl.SetVarImpl;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Base model for the Nature Reserve Problem. Defines the variables and constraints that are common to every
 * instance of the problem. Specialization for specific instances is provided by the extra constraints that can
 * be 'posted' to a model.
 */
public class ReserveModel implements IReserveModel, IReserveConstraintFactory, IFeatureFactory {

    /** The grid on which applies the model */
    private RegularSquareGrid grid;

    /** The features referenced by the model */
    private Map<String, Feature> features;

    /** The choco model */
    private GraphModel model;

    /** Decision variables */
    private IntVar[] sites;

    /** The spatial graph variables associated to the model */
    private UndirectedGraphVar graphCore, graphBuffer, graphOut;

    private boolean graphCoreInit, graphBufferInit, graphOutInit;

    private SetVar core, buffer, buffer2, out;

    /** Number of connected components of graphs */
    private IntVar nbCcCore, nbCcBuffer, nbCcOut;

    /** Number of sites */
    private IntVar nbSitesCore, nbSitesBuffer, nbSitesOut;

    public ReserveModel(RegularSquareGrid grid) {
        this(grid, true, false);
    }

    public ReserveModel(RegularSquareGrid grid, boolean useBuffer) {
        this(grid, useBuffer, false);
    }

    public ReserveModel(RegularSquareGrid grid, boolean useBuffer, boolean useBuffer2) {
        this(grid, useBuffer, useBuffer2, grid);
    }

    public ReserveModel(RegularSquareGrid grid, boolean useBuffer, boolean useBuffer2, RegularSquareGrid outGrid) {
        if (!useBuffer) {
            assert !useBuffer2;
        }
        this.grid = grid;
        int nbCells = this.grid.getNbCells(false);
        this.features = new HashMap<>();
        // Init Choco model
        this.model = new GraphModel("Nature Reserve Problem");

        int[] sitesDomain = useBuffer ? (useBuffer2 ? new int[] {0, 1, 2, 3} : new int[] {0, 1, 2}) : new int[] {0, 1};

        // Init decision variables
        this.sites = this.model.intVarArray(
                "sites",
                this.grid.getNbCells(),
                sitesDomain
        );

        // Init graph variables
        this.graphCore = model.graphVar(
                "graphCore",
                new UndirectedGraph(model, nbCells, SetType.BIPARTITESET, false),
                grid.getFullGraph(model, SetType.BIPARTITESET)
        );
        graphCoreInit = false;
        if (useBuffer) {
            this.graphBuffer = model.graphVar(
                    "graphBuffer",
                    new UndirectedGraph(model, nbCells, SetType.BIPARTITESET, false),
                    grid.getFullGraph(model, SetType.BIPARTITESET)
            );
        }
        graphBufferInit = false;
        this.graphOut = model.graphVar(
                "graphOut",
                new UndirectedGraph(model, nbCells, SetType.BIPARTITESET, false),
                outGrid.getFullGraph(model, SetType.BIPARTITESET)
        );
        graphOutInit = false;

        // Set vars
        SetType kerSetType = SetType.BIPARTITESET;
        SetType envSetType = SetType.BIPARTITESET;
        this.core = new SetVarImpl(
                "core",
                new int[] {}, kerSetType,
                IntStream.range(0, nbCells).filter(i -> !grid.isInBorder(i)).toArray(), envSetType,
                model
        );

        if (useBuffer) {
            this.buffer = new SetVarImpl(
                    "buffer",
                    new int[] {}, kerSetType,
                    IntStream.range(0, nbCells).toArray(), envSetType,
                    model
            );
            if (useBuffer2) {
                this.buffer2 = new SetVarImpl(
                        "buffer2",
                        new int[] {}, kerSetType,
                        IntStream.range(0, nbCells).toArray(), envSetType,
                        model
                );
            } else {
                this.buffer2 = model.setVar("buffer2", new int[]{});
            }
        } else {
            this.buffer = model.setVar("buffer", new int[]{});
        }
        this.out = new SetVarImpl(
                "out",
                new int[] {}, kerSetType,
                IntStream.range(0, nbCells).toArray(), envSetType,
                model
        );
        // Nb CC
        this.nbCcCore = this.model.intVar("nbCcCore", 0, nbCells);
        this.nbCcBuffer = this.model.intVar("nbCcBuffer", 0, nbCells);
        this.nbCcOut = this.model.intVar("nbCcOut", 0, nbCells);
        // Nb sites
        this.nbSitesCore = this.core.getCard();
        if (useBuffer) {
            this.nbSitesBuffer = this.buffer.getCard();
        }
        this.nbSitesOut = this.out.getCard();
        // Sets <-> Decision variables channeling
        if (useBuffer) {
            if (useBuffer2) {
                this.model.setsIntsChanneling(new SetVar[]{this.out, this.buffer2, this.buffer, this.core}, this.sites).post();
            } else {
                this.model.setsIntsChanneling(new SetVar[]{this.out, this.buffer, this.core}, this.sites).post();
            }
        } else {
            this.model.setsIntsChanneling(new SetVar[]{this.out, this.core}, this.sites).post();
        }
        // Set default search
        this.model.getSolver().setSearch(Search.domOverWDegSearch(sites));
    }

    public RegularSquareGrid getGrid() {
        return grid;
    }

    public void addFeature(Feature feature) {
        this.features.put(feature.getName(), feature);
    }

    public Map<String, Feature> getFeatures() {
        return features;
    }

    // --------------------- //
    // Choco related methods //
    // --------------------- //

    public GraphModel getChocoModel() {
        return model;
    }

    public IntVar[] getSites() {
        return sites;
    }

    public SetVar getCore() {
        return core;
    }

    public SetVar getBuffer() {
        return buffer;
    }

    public SetVar getBuffer2() {
        return buffer2;
    }

    public SetVar getOut() {
        return out;
    }

    public UndirectedGraphVar getGraphCore() {
        return graphCore;
    }

    public UndirectedGraphVar getGraphBuffer() {
        return graphBuffer;
    }

    public UndirectedGraphVar getGraphOut() {
        return graphOut;
    }

    public void initGraphCore() {
        if (!graphCoreInit) {
            model.nodesChanneling(graphCore, core).post();
            model.post(new Constraint("inducedNeighborhoodCore", new PropInducedNeighborhood(graphCore)));
            model.nbConnectedComponents(graphCore, nbCcCore).post();
            graphCoreInit = true;
        }
    }

    public void initGraphBuffer() {
        if (!graphBufferInit) {
            model.nodesChanneling(graphBuffer, buffer).post();
            model.post(new Constraint("inducedNeighborhoodBuffer", new PropInducedNeighborhood(graphBuffer)));
            model.nbConnectedComponents(graphBuffer, nbCcBuffer).post();
            graphBufferInit = true;
        }
    }

    public void initGraphOut() {
        if (!graphOutInit) {
            model.nodesChanneling(graphOut, out).post();
            model.post(new Constraint("inducedNeighborhoodOut", new PropInducedNeighborhood(graphOut)));
            model.nbConnectedComponents(graphOut, nbCcOut).post();
            graphCoreInit = true;
        }
    }

    public IntVar getNbCcCore() {
        return nbCcCore;
    }

    public IntVar getNbCcBuffer() {
        return nbCcBuffer;
    }

    public IntVar getNbCcOut() {
        return nbCcOut;
    }

    public IntVar getNbSitesCore() {
        return nbSitesCore;
    }

    public IntVar getNbSitesBuffer() {
        return nbSitesBuffer;
    }

    public IntVar getNbSitesOut() {
        return nbSitesOut;
    }

    // -------------------------- //
    // Solution retrieval methods //
    // -------------------------- //

    public int[] getSelectedCoreSites() throws ModelNotInstantiatedError {
        if (!core.isInstantiated()) {
            throw new ModelNotInstantiatedError();
        }
        return core.getLB().toArray();
    }

    public int[] getSelectedBufferSites() throws ModelNotInstantiatedError {
        if (!buffer.isInstantiated()) {
            throw new ModelNotInstantiatedError();
        }
        return buffer.getLB().toArray();
    }

    public int[] getSelectedOutSites() throws ModelNotInstantiatedError {
        if (!out.isInstantiated()) {
            throw new ModelNotInstantiatedError();
        }
        return out.getLB().toArray();
    }

    public void printSolution() {
        printSolution(true);
    }

    public void printSolution(boolean ignoreBorder) {
        if (!(grid instanceof RegularSquareGrid)) {
            return;
        }
        System.out.println("\nSolution:");
        System.out.println("   " + new String(new char[grid.getNbCols(ignoreBorder) + 2]).replace("\0", "_"));
        for (int i = 0; i < grid.getNbRows(ignoreBorder); i++) {
            if (!ignoreBorder && (i == grid.getBorder() || i == grid.getNbRows() - grid.getBorder())) {
                System.out.println("  |"
                        + new String(new char[grid.getBorder()]).replace("\0", " ")
                        + new String(new char[grid.getNbRows() - grid.getBorder()]).replace("\0", "-"));
            }
            System.out.printf("  |");
            for (int j = 0; j < grid.getNbCols(ignoreBorder); j++) {
                if (!ignoreBorder && (j == grid.getBorder() || j == grid.getNbRows() - grid.getBorder())) {
                    System.out.printf("|");
                }
                if (core.getLB().contains(grid.getIndexFromCoordinates(i, j))) {
                    System.out.printf("#");
                    continue;
                }
                if (buffer2.getLB().contains(grid.getIndexFromCoordinates(i, j))) {
                    System.out.printf("-");
                    continue;
                }
                if (buffer.getLB().contains(grid.getIndexFromCoordinates(i, j))) {
                    System.out.printf("+");
                    continue;
                }
                if (out.getLB().contains(grid.getIndexFromCoordinates(i, j))) {
                    System.out.printf(" ");
                    continue;
                }
                System.out.printf("?");
            }
            System.out.printf("\n");
        }
        System.out.println("\nNb CC core: " + getNbCcCore());
        System.out.println("Nb CC buffer: " + getNbCcBuffer());
        System.out.println("Nb CC out: " + getNbCcOut());
        System.out.println("Nb sites core: " + getNbSitesCore());
        System.out.println("Nb sites buffer: " + getNbSitesBuffer());
        System.out.println("Nb sites buffer2: " + buffer2.getCard().getValue());
        System.out.println("Nb sites out: " + getNbSitesOut());
        System.out.printf("\n");
    }

    public int getNbCols() {
        return grid.getNbCols();
    }

    public int getNbRows() {
        return grid.getNbRows();
    }

    // For constraint factory

    @Override
    public ReserveModel self() {
        return this;
    }
}
