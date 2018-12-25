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

import chocoreserve.grid.regular.square.FourConnectedSquareGrid;
import chocoreserve.grid.regular.square.HeightConnectedSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.choco.PropBufferZone;
import chocoreserve.solver.constraints.choco.PropLocalBinaryPattern;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.tools.ArrayUtils;

/**
 *
 */
public class BufferZone extends SpatialConstraint {

    private int nbRows, nbCols;

    public BufferZone(ReserveModel reserveModel) {
        super(reserveModel);
        this.nbRows = reserveModel.getNbRows();
        this.nbCols = reserveModel.getNbCols();
    }

    @Override
    public void post() {
        BoolVar[][] buffer = reserveModel.getBufferSites();
        BoolVar[][] core = reserveModel.getSitesMatrix();
        RegularSquareGrid grid = new HeightConnectedSquareGrid(nbRows, nbCols);
        PropLocalBinaryPattern lbp = new PropLocalBinaryPattern(
                core,
                new int[] {
                        0b1001,
                        0b1001,
                        0b1011,
                        0b1101,
                        0b1101,
                        0b1011,
                        0b101,
                        0b101
                },
                new int[][][] {
                        {{0, 1}, {0, 1}, {0, 1}, {0, -3}},
                        {{1, 0}, {1, 0}, {1, 0}, {-3, 0}},
                        {{1, 1}, {0, 1}, {1, -1}, {-2, -1}},
                        {{1, -2}, {0, 1}, {1, 0}, {-2, 1}},
                        {{1, -1}, {0, 1}, {1, 1}, {-2, -1}},
                        {{1, 0}, {0, 1}, {1, -2}, {-2, 1}},
                        {{0, 1}, {0, 1}, {0, -2}},
                        {{1, 0}, {1, 0}, {-2, 0}}
                }
        );
        PropBufferZone bufferZone = new PropBufferZone(core, buffer, grid);
        Constraint c = new Constraint("bufferZone", lbp);
        chocoModel.post(c);
    }

    public void post_() {
        BoolVar[] buffer = ArrayUtils.flatten(reserveModel.getBufferSites());
        BoolVar[] sites = reserveModel.getSites();
        RegularSquareGrid grid = new HeightConnectedSquareGrid(nbRows, nbCols);
        for (int i = 0; i < buffer.length; i++) {
            chocoModel.not(chocoModel.and(buffer[i], sites[i])).post();
            int[] neigh = grid.getNeighbors(i);
            for (int j : neigh) {
                BoolVar b = chocoModel.and(sites[i], chocoModel.boolNotView(sites[j])).reify();
                chocoModel.ifThen(b, chocoModel.arithm(buffer[j], "=", 1));
            }
        }
    }
}
