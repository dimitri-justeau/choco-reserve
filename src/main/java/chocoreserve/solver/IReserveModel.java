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
import chocoreserve.grid.IGrid;
import chocoreserve.solver.feature.Feature;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.Map;

/**
 * Interface for the base model of the Nature Reserve Problem. Defines the variables and constraints that are common
 * to every instance of the problem. Specialization for specific instances is provided by the extra constraints that
 * can be 'posted' to a model.
 */
public interface IReserveModel {

    /**
     * @return The grid associated with the model.
     */
    IGrid getGrid();

    /**
     * Add a feature to the model.
     */
    void addFeature(Feature feature);

    /**
     * @return A map of the features referenced by the model.
     */
    Map<String, Feature> getFeatures();

    // --------------------- //
    // Choco related methods //
    // --------------------- //

    /**
     * @return The Choco (graph) model on which relies the model.
     */
    GraphModel getChocoModel();

    /**
     * @return The Choco solver associated to the model.
     */
    default Solver getChocoSolver() {
        return getChocoModel().getSolver();
    }

    /**
     * @return The graph variable representing the spatial graph.
     */
    UndirectedGraphVar getSpatialGraphVar();

    /**
     * @return The decision variables of the model, which are the sites.
     *         Each site is associated to a cell of the grid.
     */
    BoolVar[] getSites();

    BoolVar[][] getSitesMatrix();

    BoolVar[] getBufferSites();


    /**
     * @return The IntVar corresponding to the number of connected components of the spatial graph.
     */
    IntVar getNbConnectedComponents();

    /**
     * @return The IntVar corresponding to the number of selected sites.
     */
    IntVar getNbSites();

    // -------------------------- //
    // Solution retrieval methods //
    // -------------------------- //

    /**
     * @return The indices of the selected sites as an int array.
     * @throws ModelNotInstantiatedError If the solver is not at a solution state.
     */
    int[] getSelectedSites() throws ModelNotInstantiatedError;

    /**
     * @return The indices of the selected sites as an integer set.
     * @throws ModelNotInstantiatedError If the solver is not at a solution state.
     */
    ISet getSelectedSitesAsSet() throws ModelNotInstantiatedError;
}
