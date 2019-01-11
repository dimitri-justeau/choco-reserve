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
import chocoreserve.grid.regular.square.HeightConnectedSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.PropNeighbors;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.SetVar;

import java.util.stream.IntStream;

/**
 *
 */
public class BufferZone extends SpatialConstraint {

    private SetVar neighCore, neighOut;

    public BufferZone(ReserveModel reserveModel) {
        super(reserveModel);
        int nbCells = reserveModel.getGrid().getNbCells();
        this.neighCore = chocoModel.setVar("neighCore", new int[] {}, IntStream.range(0, nbCells).toArray());
        this.neighOut = chocoModel.setVar("neighOut", new int[] {}, IntStream.range(0, nbCells).toArray());
    }

    @Override
    public void post() {
        Grid grid = new HeightConnectedSquareGrid(reserveModel.getNbRows(), reserveModel.getNbCols());
        int nbCells = reserveModel.getGrid().getNbCells();
        int[][] adjLists = new int[nbCells][];
        int[][] adjLists2 = new int[nbCells][];
        IntStream.range(0, nbCells).forEach(i -> adjLists[i] = grid.getNeighbors(i));
        IntStream.range(0, nbCells).forEach(i -> adjLists2[i] = grid.getNeighbors(i));
        Constraint consNeighCore = new Constraint(
                "consNeighCore",
                new PropNeighbors(reserveModel.getCore(), neighCore, adjLists)
        );
        Constraint consNeighOut = new Constraint(
                "consNeighOut",
                new PropNeighbors(reserveModel.getOut(), neighOut, adjLists)
        );
        chocoModel.post(consNeighCore);
        chocoModel.post(consNeighOut);
        chocoModel.disjoint(neighCore, reserveModel.getOut()).post();
        chocoModel.disjoint(neighOut, reserveModel.getCore()).post();
        chocoModel.intersection(new SetVar[]{neighOut, neighCore}, reserveModel.getBuffer()).post();
    }
}
