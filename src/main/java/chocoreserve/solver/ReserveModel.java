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
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** The spatial graph variable associated to the model */
    private UndirectedGraphVar g;

    /** The decision variables, one for each site (each site correspond to a cell of the grid */
    private BoolVar[][] sites;

    private BoolVar[][] bufferSites;

    /** Number of connected components of g */
    private IntVar nbCC;

    /** Number of sitess */
    private IntVar nbSites;

    public ReserveModel(RegularSquareGrid grid) {
        this.grid = grid;
        this.features = new HashMap<>();
        // Init Choco model
        this.model = new GraphModel("Nature Reserve Problem");
        this.sites = model.boolVarMatrix("sites", grid.getNbRows(), grid.getNbCols());
        this.g = model.graphVar(
                "spatialGraph",
                new UndirectedGraph(model, grid.getNbCells(), SetType.BIPARTITESET, false),
                grid.getFullGraph(model, SetType.BIPARTITESET)
        );
        this.nbCC = this.model.intVar("nbCC", 0, this.grid.getNbCells());
        this.nbSites = this.model.intVar("nbSites", 0, this.grid.getNbCells());
        // Post necessary constraints
        this.model.nodesChanneling(this.g, getSites()).post();
        this.model.post(new Constraint("inducedNeighborhood", new PropInducedNeighborhood(this.g)));
        this.model.nbConnectedComponents(this.g, this.nbCC).post();
        this.model.sum(getSites(), "=", this.nbSites).post();
    }

    @Override
    public RegularSquareGrid getGrid() {
        return grid;
    }


    @Override
    public void addFeature(Feature feature) {
        this.features.put(feature.getName(), feature);
    }

    @Override
    public Map<String, Feature> getFeatures() {
        return features;
    }

    // --------------------- //
    // Choco related methods //
    // --------------------- //

    @Override
    public GraphModel getChocoModel() {
        return model;
    }

    @Override
    public UndirectedGraphVar getSpatialGraphVar() {
        return g;
    }

    @Override
    public BoolVar[] getSites() {
        return ArrayUtils.flatten(sites);
    }

    public BoolVar[][] getSitesMatrix() {
        return sites;
    }

    @Override
    public BoolVar[][] getBufferSites() {
        if (bufferSites == null) {
            this.bufferSites = model.boolVarMatrix("bufferSites", grid.getNbRows(), grid.getNbCols());
        }
        return bufferSites;
    }

    @Override
    public IntVar getNbConnectedComponents() {
        return nbCC;
    }

    @Override
    public IntVar getNbSites() {
        return nbSites;
    }

    // -------------------------- //
    // Solution retrieval methods //
    // -------------------------- //

    @Override
    public int[] getSelectedSites() throws ModelNotInstantiatedError {
        return getSelectedSitesAsSet().toArray();
    }

    @Override
    public ISet getSelectedSitesAsSet() throws ModelNotInstantiatedError {
        UndirectedGraphVar g = getSpatialGraphVar();
        if (!g.isInstantiated()) {
            throw new ModelNotInstantiatedError();
        }
        return g.getMandatoryNodes();
    }

    public void printSolution(boolean showPlanningUnits) {
        if (!(grid instanceof RegularSquareGrid)) {
            return;
        }
        RegularSquareGrid rGrid = (RegularSquareGrid) grid;
        System.out.println("\nSolution:");
        System.out.println("   " + new String(new char[rGrid.getNbCols()]).replace("\0", "_"));
        ArrayList<Integer> selectedParcels = new ArrayList<>();
        for (int i = 0; i < rGrid.getNbRows(); i++) {
            System.out.printf("  |");
            for (int j = 0; j < rGrid.getNbCols(); j++) {
                if (getSitesMatrix()[i][j].getValue() == 1) {
                    System.out.printf("#");
                    selectedParcels.add(j + rGrid.getNbCols() * i);
                } else {
                    if (this.bufferSites != null && this.bufferSites[i][j].getValue() == 1){
                        System.out.printf("+");
                    } else {
                        System.out.printf(" ");
                    }
                }
            }
            System.out.printf("\n");
        }
        System.out.println("\nNumber of reserves: " + getNbConnectedComponents());
        System.out.println("Number of parcels: " + getNbSites());
        if (showPlanningUnits) {
            System.out.println("Selected parcels:");
            for (int i : selectedParcels) {
                List<String> covered = new ArrayList<>();
                for (Feature f : features.values()) {
                    try {
                        if (f.getData()[i] > 0) {
                            covered.add(String.format("%1$4s", f.getName()));
                        } else {
                            covered.add(String.format("%1$4s", ""));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(String.format(" Parcel %1$4s: ", i) + String.join(" ", covered));
            }
        }
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
    public IReserveModel self() {
        return this;
    }
}
