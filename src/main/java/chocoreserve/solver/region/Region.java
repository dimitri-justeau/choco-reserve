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
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.stream.IntStream;

/**
 * Class representing a region.
 */
public class Region extends AbstractRegion {

    private INeighborhood neighborhood;
    private IntVar nbCC;
    private SetType setVarSetType;
    protected int[] LBNodes, UBNodes;
    private boolean ubDecr;

    public Region(String name, INeighborhood neighborhood, SetType setVarSetType, int[] LBNodes, int[] UBNodes) {
        this(name, neighborhood, setVarSetType, LBNodes, UBNodes, false);
    }

    public Region(String name, INeighborhood neighborhood, SetType setVarSetType, int[] LBNodes, int[] UBNodes, boolean ubDecr) {
        super(name);
        this.neighborhood = neighborhood;
        this.setVarSetType = setVarSetType;
        this.LBNodes = LBNodes;
        this.UBNodes = UBNodes;
        this.ubDecr = ubDecr;
    }

    public Region(String name, INeighborhood neighborhood, SetType setVarSetType, int[] LBNodes) {
        this(name, neighborhood, setVarSetType, LBNodes, null);
    }

    public Region(String name, INeighborhood neighborhood, SetType setVarSetType) {
        this(name, neighborhood, setVarSetType, new int[]{}, null);
    }

    public Region(String name, INeighborhood neighborhood) {
        this(name, neighborhood, SetType.BIPARTITESET);
    }

    @Override
    protected void buildSetVar() {
        GraphModel model = reserveModel.getChocoModel();
        Grid grid = reserveModel.getGrid();
        if (UBNodes == null) {
            UBNodes = IntStream.range(0, grid.getNbCells()).toArray();
        }
        setVar = new SpatialGraphVar(
                "regionSetVar['" + name + "']",
                LBNodes, setVarSetType,
                UBNodes, setVarSetType,
                model,
                grid,
                neighborhood,
                ubDecr
        );
    }

    public INeighborhood getNeighborhood() {
        return neighborhood;
    }

    public boolean nbCCInit() {
        return nbCC != null;
    }
}
