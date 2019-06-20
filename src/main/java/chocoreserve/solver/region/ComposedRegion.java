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

package chocoreserve.solver.region;

import chocoreserve.grid.Grid;
import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.solver.constraints.choco.graph.PropInducedNeighborhood;
import chocoreserve.util.objects.graphs.UndirectedGraphIncrementalCC;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.impl.SetVarImpl;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Class representing a region.
 */
public class ComposedRegion extends AbstractRegion {

    private final SetType GRAPH_SET_TYPE = SetType.BIPARTITESET;
    private final SetType SET_VAR_SET_TYPE = SetType.BIPARTITESET;

    private INeighborhood neighborhood;
    private UndirectedGraphVar graphVar;
    private Region[] regions;

    public ComposedRegion(String name, Region... regions) {
        super(name);
        this.regions = regions;
        this.neighborhood = regions[0].getNeighborhood();
    }

    @Override
    protected void buildSetVar() {
        for (Region r : regions) {
            assert reserveModel == r.getReserveModel();
        }
        SetVar[] setVars = Arrays.stream(regions).map(r -> r.getSetVar()).toArray(SetVar[]::new);
        GraphModel model = reserveModel.getChocoModel();
        Grid grid = reserveModel.getGrid();
        setVar = new SetVarImpl(
                "composedRegionSetVar['" + name + "']",
                new int[] {}, SET_VAR_SET_TYPE,
                IntStream.range(0, grid.getNbCells()).toArray(), SET_VAR_SET_TYPE,
                model
        );
        model.union(setVars, setVar).post();
    }

    public UndirectedGraphVar getGraphVar() {
        if (graphVar == null) {
            GraphModel model = reserveModel.getChocoModel();
            Grid grid = reserveModel.getGrid();
            graphVar = model.graphVar(
                    "regionGraphVar['" + name + "']",
                    new UndirectedGraphIncrementalCC(model, grid.getNbCells(), GRAPH_SET_TYPE, false),
                    neighborhood.getFullGraph(grid, model, GRAPH_SET_TYPE)
            );
            model.nodesChanneling(graphVar, getSetVar()).post();
            model.post(new Constraint("inducedNeigh['" + name + "']", new PropInducedNeighborhood(graphVar)));
        }
        return graphVar;
    }
}
