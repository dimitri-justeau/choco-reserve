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

package chocoreserve.solver.constraints.choco;

import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Buffer zone propagator.
 *
 * Works on two matrices of boolean variables: the core area C and the buffer zone B.
 *
 * Proceed into two steps:
 *
 * 1. For each (i, j), if C[i][j] = 1 then for each neighbor of (k, l) such that C[k][l] = 0 set B[k][l] = 1.
 * 2. For each (i, j), if B[i][j] = 1 and every neighbor (k, l) of (i, j) is such that B[k][l] = 1
 *    then set B[i][j] = 0 and C[i][j] = 1.
 */
public class PropBufferZone extends Propagator<BoolVar> {

    private BoolVar[][] coreArea, bufferZone;
    private RegularSquareGrid grid;

    public PropBufferZone(BoolVar[][] coreArea, BoolVar[][] bufferZone, RegularSquareGrid grid) {
        super(ArrayUtils.concat(ArrayUtils.flatten(coreArea), ArrayUtils.flatten(bufferZone)));
        this.coreArea = coreArea;
        this.bufferZone = bufferZone;
        this.grid = grid;
        assert coreArea.length == grid.getNbRows();
        assert coreArea[0].length == grid.getNbCols();
        assert bufferZone.length == grid.getNbRows();
        assert bufferZone[0].length == grid.getNbCols();
    }

    @Override
    public void propagate(int i) throws ContradictionException {
        Set<Integer> toBufferZone = new HashSet<>();
        Set<Integer> toCoreArea = new HashSet<>();
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < grid.getNbRows(); i++) {
            for (int j = 0; j < grid.getNbCols(); j++) {
                // If site is in core area check that it is surrounded by core area sites or buffer zone sites.
                if (coreArea[i][j].isInstantiatedTo(1)) {
                    for (int[] k : grid.getMatrixNeighbors(i, j)) {
                        if (!coreArea[k[0]][k[1]].isInstantiatedTo(1) && !bufferZone[k[0]][k[1]].isInstantiatedTo(1)) {
                            return ESat.FALSE;
                        }
                    }
                }
                // If site is in buffer zone check that it has at least one neighbor not in core nor in buffer zone.
                if (bufferZone[i][j].isInstantiatedTo(1)) {
                    // If we are on an extremity of the grid return TRUE
                    if (i == 0 || i == grid.getNbRows() -1 || j == 0 || j == grid.getNbCols() - 1) {
                        continue;
                    }
                    boolean hasEmptyNeigh = false;
                    for (int[] k : grid.getMatrixNeighbors(i, j)) {
                        if (coreArea[k[0]][k[1]].isInstantiatedTo(0) && bufferZone[k[0]][k[1]].isInstantiatedTo(0)) {
                            hasEmptyNeigh = true;
                            break;
                        }
                    }
                    if (!hasEmptyNeigh) {
                        return ESat.FALSE;
                    }
                }
            }
        }
        return ESat.TRUE;
    }
}
