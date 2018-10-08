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

import chocoreserve.grid.IGrid;
import chocoreserve.solver.constraints.IReserveConstraintFactory;
import chocoreserve.solver.constraints.choco.graph.PropInducedNeighborhood;
import chocoreserve.solver.feature.IFeature;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.HashMap;
import java.util.Map;

/**
 * Base model for the Nature Reserve Problem. Defines the variables and constraints that are common to every
 * instance of the problem. Specialization for specific instances is provided by the extra constraints that can
 * be 'posted' to a model.
 */
public class ReserveModel implements IReserveModel, IReserveConstraintFactory {

    /** The grid on which applies the model */
    private IGrid grid;

    /** The features referenced by the model */
    private Map<String, IFeature> features;

    /** The choco model */
    private GraphModel model;

    /** The spatial graph variable associated to the model */
    private UndirectedGraphVar g;

    /** The decision variables, one for each planning unit (each planning unit correspond to a cell of the grid */
    private BoolVar[] planningUnits;

    /** Number of connected components of g */
    private IntVar nbCC;

    /** Number of planning units */
    private IntVar nbPlanningUnits;

    public ReserveModel(IGrid grid) {
        this.grid = grid;
        this.features = new HashMap<>();
        // Init Choco model
        this.model = new GraphModel("Nature Reserve Problem");
        this.planningUnits = model.boolVarArray("planningUnits", this.grid.getNbCells());
        this.g = model.graphVar(
                "spatialGraph",
                new UndirectedGraph(model, grid.getNbCells(), SetType.BIPARTITESET, false),
                grid.getFullGraph(model, SetType.BIPARTITESET)
        );
        this.nbCC = this.model.intVar("nbCC", 0, this.grid.getNbCells());
        this.nbPlanningUnits = this.model.intVar("nbPlanningUnits", 0, this.grid.getNbCells());
        // Post necessary constraints
        this.model.nodesChanneling(this.g, this.planningUnits).post();
        this.model.post(new Constraint("inducedNeighborhood", new PropInducedNeighborhood(this.g)));
        this.model.nbConnectedComponents(this.g, this.nbCC).post();
        this.model.sum(this.planningUnits, "=", this.nbPlanningUnits).post();
    }

    @Override
    public IGrid getGrid() {
        return grid;
    }


    @Override
    public void addFeature(IFeature feature) {
        this.features.put(feature.getName(), feature);
    }

    @Override
    public Map<String, IFeature> getFeatures() {
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
    public BoolVar[] getPlanningUnits() {
        return planningUnits;
    }

    @Override
    public IntVar getNbConnectedComponents() {
        return nbCC;
    }

    @Override
    public IntVar getNbPlanningUnits() {
        return nbPlanningUnits;
    }

    @Override
    public IReserveModel _self() {
        return this;
    }
}
