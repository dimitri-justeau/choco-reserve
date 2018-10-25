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
import org.chocosolver.solver.constraints.PropagatorPriority;
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
 *
 * Also ensure that C and B are disjoint.
 */
public class PropBufferZone extends Propagator<BoolVar> {

    private BoolVar[][] coreArea, bufferZone;
    private RegularSquareGrid grid;

    public PropBufferZone(BoolVar[][] coreArea, BoolVar[][] bufferZone, RegularSquareGrid grid) {
        super(ArrayUtils.concat(ArrayUtils.flatten(coreArea), ArrayUtils.flatten(bufferZone)),
                PropagatorPriority.LINEAR, false);
        this.coreArea = coreArea;
        this.bufferZone = bufferZone;
        this.grid = grid;
        assert coreArea.length == grid.getNbRows();
        assert coreArea[0].length == grid.getNbCols();
        assert bufferZone.length == grid.getNbRows();
        assert bufferZone[0].length == grid.getNbCols();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        Set<Integer> toBuffer = new HashSet<>();
        for (int i = 0; i < grid.getNbRows(); i++) {
            for (int j = 0; j < grid.getNbCols(); j++) {
                if (bufferZone[i][j].isInstantiatedTo(1) && cannotHaveAdjacentCore(i, j)) {
                    fails();
                }
                if (coreArea[i][j].isInstantiatedTo(1) && bufferZone[i][j].isInstantiatedTo(1)) {
                    fails();
                }
                toBuffer.addAll(getOutFrontier(i, j));
            }
        }
        for (int index : toBuffer) {
            int[] c = grid.getCoordinatesFromIndex(index);
            bufferZone[c[0]][c[1]].setToTrue(this);
        }
        for (int i = 0; i < grid.getNbRows(); i++) {
            for (int j = 0; j < grid.getNbCols(); j++) {
                if (isBuffered(i, j) && bufferZone[i][j].isInstantiatedTo(1)) {
                    if (i != 0 && i != grid.getNbRows() - 1 && j != 0 && j != grid.getNbCols() - 1) {
                        fails();
                    }
                }
            }
        }
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < grid.getNbRows(); i++) {
            for (int j = 0; j < grid.getNbCols(); j++) {
                // Check that core and buffer are disjoint
                if (coreArea[i][j].isInstantiatedTo(1) && bufferZone[i][j].isInstantiatedTo(1)) {
                    return ESat.FALSE;
                }
                // Check for core sites without adjacent buffer
                if (isCoreFrontier(i, j) && isNotBuffered(i, j)) {
                    return ESat.FALSE;
                }
                // Check for buffer sites without adjacent core
                if (bufferZone[i][j].isInstantiatedTo(1) && cannotHaveAdjacentCore(i, j)) {
                    return ESat.FALSE;
                }
                // Check for buffer sites that should be into core
                if (isBuffered(i, j) && bufferZone[i][j].isInstantiatedTo(1)) {
                    // Check that the site is not on the limits of the grid
                    if (i != 0 && i != grid.getNbRows() - 1 && j != 0 && j != grid.getNbCols() - 1) {
                        return ESat.FALSE;
                    }
                }
            }
        }
        return ESat.TRUE;
    }

    private void showSolution() {
        for (int i = 0; i < grid.getNbRows(); i++) {
            System.out.printf("  |");
            for (int j = 0; j < grid.getNbCols(); j++) {
                boolean b = true;
                if (coreArea[i][j].isInstantiatedTo(1)) {
                    System.out.printf("#");
                    b = false;
                }
                if (bufferZone[i][j].isInstantiatedTo(1)) {
                    System.out.printf("+");
                    b =false;
                }
                if (b) {
                    System.out.printf(" ");
                }
            }
            System.out.printf("\n");
        }
    }

    private Set<Integer> getOutFrontier(int i, int j) {
        Set<Integer> outFrontier = new HashSet<>();
        if (coreArea[i][j].isInstantiatedTo(1)) {
            for (int[] neigh : grid.getMatrixNeighbors(i, j)) {
                int k = neigh[0]; int l = neigh[1];
                if (coreArea[k][l].isInstantiatedTo(0)) {
                    outFrontier.add(grid.getIndexFromCoordinates(k, l));
                }
            }
        }
        return outFrontier;
    }

    /**
     * @return True if coreArea[i][j] == 1 and is on the frontier (i.e. it has at least one adjacent site that is
     *          instantiated to 0).
     */
    private boolean isCoreFrontier(int i, int j) {
        if (!coreArea[i][j].isInstantiatedTo(1)) {
            return false;
        }
        for (int[] neigh : grid.getMatrixNeighbors(i, j)) {
            int k = neigh[0]; int l = neigh[1];
            if (coreArea[k][l].isInstantiatedTo(0)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNotBuffered(int i, int j) {
        for (int[] neigh : grid.getMatrixNeighbors(i, j)) {
            int k = neigh[0]; int l = neigh[1];
            if (coreArea[k][l].isInstantiatedTo(0) && bufferZone[k][l].isInstantiatedTo(0)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBuffered(int i, int j) {
        for (int[] neigh : grid.getMatrixNeighbors(i, j)) {
            int k = neigh[0]; int l = neigh[1];
            if (!coreArea[k][l].isInstantiatedTo(1) && !bufferZone[k][l].isInstantiatedTo(1)) {
                return false;
            }
        }
        return true;
    }

    private boolean cannotHaveAdjacentCore(int i, int j) {
        for (int[] neigh : grid.getMatrixNeighbors(i, j)) {
            int k = neigh[0]; int l = neigh[1];
            if (coreArea[k][l].getUB() == 1) {
                return false;
            }
        }
        return true;
    }
}
