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

import chocoreserve.grid.Grid;
import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.Neighborhood;
import chocoreserve.solver.Region;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.PropNeighbors;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.SetVar;

import java.util.stream.IntStream;

/**
 *
 */
public class BufferZone extends SpatialConstraint {

    private SetVar set1, set2, buffer, neighSet1, neighSet2;
    private INeighborhood neighborhood;

    public BufferZone(ReserveModel reserveModel, Region region1, Region region2, Region buffer) {
        this(reserveModel, Neighborhood.HEIGHT_CONNECTED, region1, region2, buffer);
    }

    public BufferZone(ReserveModel reserveModel, SetVar set1, SetVar set2, SetVar buffer) {
        this(reserveModel, Neighborhood.HEIGHT_CONNECTED, set1, set2, buffer);
    }

    public BufferZone(ReserveModel reserveModel, INeighborhood neighborhood, Region region1, Region region2, Region buffer) {
        this(reserveModel, neighborhood, region1.getSetVar(), region2.getSetVar(), buffer.getSetVar());
    }

    public BufferZone(ReserveModel reserveModel, INeighborhood neighborhood, SetVar set1, SetVar set2, SetVar buffer) {
        super(reserveModel);
        this.neighborhood = neighborhood;
        int nbCells = reserveModel.getGrid().getNbCells();
        this.set1 = set1;
        this.set2 = set2;
        this.buffer = buffer;
        this.neighSet1 = chocoModel.setVar("neighSet1", new int[] {}, IntStream.range(0, nbCells).toArray());
        this.neighSet2 = chocoModel.setVar("neighSet2", new int[] {}, IntStream.range(0, nbCells).toArray());
    }


    @Override
    public void post() {
        Grid grid = reserveModel.getGrid();
        int nbCells = grid.getNbCells();
        int[][] adjLists = new int[nbCells][];
        int[][] adjLists2 = new int[nbCells][];
        IntStream.range(0, nbCells).forEach(i -> adjLists[i] = neighborhood.getNeighbors(grid, i));
        IntStream.range(0, nbCells).forEach(i -> adjLists2[i] = neighborhood.getNeighbors(grid, i));
        Constraint consNeighSet1 = new Constraint(
                "consNeighSet1",
                new PropNeighbors(set1, neighSet1, adjLists)
        );
        Constraint consNeighSet2 = new Constraint(
                "consNeighSet2",
                new PropNeighbors(set2, neighSet2, adjLists)
        );
        chocoModel.post(consNeighSet1, consNeighSet2);
        chocoModel.disjoint(neighSet1, set2).post();
        chocoModel.disjoint(neighSet2, set1).post();
        chocoModel.intersection(new SetVar[]{neighSet2, neighSet1}, buffer).post();
    }
}
