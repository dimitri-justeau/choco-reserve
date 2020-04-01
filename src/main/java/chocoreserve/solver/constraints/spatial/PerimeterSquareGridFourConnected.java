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

package chocoreserve.solver.constraints.spatial;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.regulare.square.FourConnected;
import chocoreserve.grid.neighborhood.regulare.square.PartialFourConnected;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.graph.PropPerimeterSquareGridFourConnected;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

public class PerimeterSquareGridFourConnected extends SpatialConstraint {

    private AbstractRegion region;
    public IntVar perimeter;

    public PerimeterSquareGridFourConnected(ReserveModel reserveModel, AbstractRegion region) {
        super(reserveModel);
        assert reserveModel.getGrid() instanceof RegularSquareGrid;
        INeighborhood nei = region.getNeighborhood();
        assert nei instanceof FourConnected || nei instanceof PartialFourConnected;
        this.region = region;
        this.perimeter = reserveModel.getChocoModel().intVar(
                "perimeter_" + region.getName(),
                0,
                reserveModel.getGrid().getNbCells() * 4
        );
    }

    @Override
    public void post() {
        PropPerimeterSquareGridFourConnected propPerimeter = new PropPerimeterSquareGridFourConnected(
                region.getSetVar(),
                perimeter
        );
        chocoModel.post(new Constraint("PropPerimeter_" + region.getName(), propPerimeter));
    }
}
