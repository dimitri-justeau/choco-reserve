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
import chocoreserve.solver.variable.SpatialGraphVar;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Class representing a region.
 */
public class ComposedRegion extends AbstractRegion {

    private final SetType GRAPH_SET_TYPE = SetType.BIPARTITESET;
    private final SetType SET_VAR_SET_TYPE = SetType.BIPARTITESET;

    private INeighborhood neighborhood;
    private Region[] regions;
    private int[] LBNodes;
    private int[] UBNodes;
    private boolean ubDecr;

    public ComposedRegion(String name, Region... regions) {
        this(name, false, regions);
    }

    public ComposedRegion(String name, boolean ubDecr, Region... regions) {
        super(name);
        this.regions = regions;
        this.neighborhood = regions[0].getNeighborhood();
        this.ubDecr = ubDecr;
        Set<Integer> lb = new HashSet<>();
        Set<Integer> ub = new HashSet<>();
        boolean ubNull = false;
        for (Region r : regions) {
            for (int i : r.LBNodes) {
                lb.add(i);
            }
            if (r.UBNodes == null) {
                ubNull = true;
            } else {
                for (int i : r.UBNodes) {
                    ub.add(i);
                }
            }
        }
        this.LBNodes = lb.stream().mapToInt(i -> i).toArray();
        if (!ubNull) {
            this.UBNodes = ub.stream().mapToInt(i -> i).toArray();
        } else {
            this.UBNodes = null;
        }
    }

    @Override
    protected void buildSetVar() {
        for (Region r : regions) {
            assert reserveModel == r.getReserveModel();
        }
        SetVar[] setVars = Arrays.stream(regions).map(r -> r.getSetVar()).toArray(SetVar[]::new);
        GraphModel model = reserveModel.getChocoModel();
        Grid grid = reserveModel.getGrid();
        if (UBNodes == null) {
            UBNodes = IntStream.range(0, grid.getNbCells()).toArray();
        }
        setVar = new SpatialGraphVar(
                "composedRegionSetVar['" + name + "']",
                LBNodes, SET_VAR_SET_TYPE,
                UBNodes, SET_VAR_SET_TYPE,
                model,
                grid,
                neighborhood,
                ubDecr
        );
        model.union(setVars, setVar).post();
    }

    public INeighborhood getNeighborhood() {
        return neighborhood;
    }
}
