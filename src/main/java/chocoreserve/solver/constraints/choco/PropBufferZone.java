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

    private final int[] MASKS = new int[] {
            0b100001010,
            0b001100010,
            0b010100001,
            0b010001100,
            0b000101000,
            0b010000010
    };

    public PropBufferZone(BoolVar[][] coreArea, BoolVar[][] bufferZone, RegularSquareGrid grid) {
        super(ArrayUtils.concat(ArrayUtils.flatten(coreArea), ArrayUtils.flatten(bufferZone)),
                PropagatorPriority.QUADRATIC, true);
//        super(ArrayUtils.flatten(coreArea), PropagatorPriority.QUADRATIC, true);
        this.coreArea = coreArea;
        this.bufferZone = bufferZone;
        this.grid = grid;
        assert coreArea.length == grid.getNbRows();
        assert coreArea[0].length == grid.getNbCols();
        assert bufferZone.length == grid.getNbRows();
        assert bufferZone[0].length == grid.getNbCols();
    }

    @Override
    public void propagate(int vidx, int evtmask) throws ContradictionException {

        if (vidx < grid.getNbCells()) {
//            int[] c = grid.getCoordinatesFromIndex(vidx);
//            int i = c[0]; int j = c[1];
//            if (coreArea[i][j].isInstantiatedTo(1)) {
//                bufferZone[i][j].setToFalse(this);
//                for (int k : getOutFrontier(i, j)) {
//                    int[] p = grid.getCoordinatesFromIndex(k);
//                    bufferZone[p[0]][p[1]].setToTrue(this);
//                }
//            }

            int[] c = grid.getCoordinatesFromIndex(vidx);
            int ii = c[0]; int jj = c[1];
            int start_i = Math.max(ii - 1, 0);
            int end_i = Math.min(ii + 1, grid.getNbRows() - 1);
            int start_j = Math.max(jj - 1, 0);
            int end_j = Math.min(jj + 1, grid.getNbCols());
            for (int i = start_i; i <= end_i; i++) {
                for (int j = start_j; j <= end_j; j++) {
                    int v = getBit(i, j);
                    for (int mask : MASKS) {
                        if ((mask & v) == mask) {
                            coreArea[i][j].setToTrue(this);
                        }
                    }
                }
            }
        }
//        else {
//            int[] c = grid.getCoordinatesFromIndex(vidx - grid.getNbCells());
//            int i = c[0]; int j = c[1];
//            if(bufferZone[i][j].isInstantiatedTo(1)) {
//                coreArea[i][j].setToFalse(this);
//            }
//            if (cannotHaveAdjacentCore(i, j)) {
//                bufferZone[i][j].setToFalse(this);
//            }
//            boolean toCore = true;
//            for (int[] neigh : grid.getMatrixNeighbors(i, j)) {
//                int k = neigh[0]; int l = neigh[1];
//                if (!coreArea[k][l].isInstantiatedTo(1)
//                        || !bufferZone[k][l].isInstantiatedTo(1)) {
//                    toCore = false;
//                }
//            }
//            if (toCore) {
//                coreArea[i][j].setToTrue(this);
//                bufferZone[i][j].setToFalse(this);
//            }
//        }
//        forcePropagate(PropagatorEventType.CUSTOM_PROPAGATION);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
//        Set<Integer> toBuffer = new HashSet<>();
//
//        for (int i = 0; i < grid.getNbRows(); i++) {
//            for (int j = 0; j < grid.getNbCols(); j++) {
//                if (coreArea[i][j].isInstantiatedTo(1)) {
//                    bufferZone[i][j].setToFalse(this);
//                    for (int index : toFill(i, j)) {
//                        int[] c = grid.getCoordinatesFromIndex(index);
//                        coreArea[c[0]][c[1]].setToTrue(this);
//                    }
//                    toBuffer.addAll(getOutFrontier(i, j));
//                }
//                if(bufferZone[i][j].isInstantiatedTo(1)) {
//                    coreArea[i][j].setToFalse(this);
//                }
//                if (cannotHaveAdjacentCore(i, j)) {
//                    bufferZone[i][j].setToFalse(this);
//                }
//            }
//        }
//        for (int index : toBuffer) {
//            int[] c = grid.getCoordinatesFromIndex(index);
//            bufferZone[c[0]][c[1]].setToTrue(this);
//        }
//        for (int i = 0; i < grid.getNbRows(); i++) {
//            for (int j = 0; j < grid.getNbCols(); j++) {
//                if (isBuffered(i, j) && bufferZone[i][j].isInstantiatedTo(1)) {
//                    if (i != 0 && i != grid.getNbRows() - 1 && j != 0 && j != grid.getNbCols() - 1) {
////                        coreArea[i][j].setToTrue(this);
////                        bufferZone[i][j].setToFalse(this);
//                        fails();
//                    }
//                }
//            }
//        }
    }

    @Override
    public ESat isEntailed() {
        showSolution();
        System.out.println();
//        for (int i = 0; i < grid.getNbRows(); i++) {
//            for (int j = 0; j < grid.getNbCols(); j++) {
//                // Check that core and buffer are disjoint
//                if (coreArea[i][j].isInstantiatedTo(1) && bufferZone[i][j].isInstantiatedTo(1)) {
//                    return ESat.FALSE;
//                }
//                // Check for core sites without adjacent buffer
//                if (isCoreFrontier(i, j) && isNotBuffered(i, j)) {
//                    return ESat.FALSE;
//                }
//                // Check for buffer sites without adjacent core
//                if (bufferZone[i][j].isInstantiatedTo(1) && cannotHaveAdjacentCore(i, j)) {
//                    return ESat.FALSE;
//                }
//                // Check for buffer sites that should be into core
//                if (isBuffered(i, j) && bufferZone[i][j].isInstantiatedTo(1)) {
//                    // Check that the site is not on the limits of the grid
//                    if (i != 0 && i != grid.getNbRows() - 1 && j != 0 && j != grid.getNbCols() - 1) {
//                        return ESat.FALSE;
//                    }
//                }
//            }
//        }
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

    private Set<Integer> toFill(int i, int j) {
        Set<Integer> fill = new HashSet<>();
        // Toward left
        if ( (j - 3) >= 0 && coreArea[i][j - 3].isInstantiatedTo(1) ) {
            fill.add(grid.getIndexFromCoordinates(i, j - 2));
            fill.add(grid.getIndexFromCoordinates(i, j - 1));
        } else {
            if ( (j - 2) >=0 && coreArea[i][j - 2].isInstantiatedTo(1) ) {
                fill.add(grid.getIndexFromCoordinates(i, j - 1));
            }
        }
        // Toward right
        if ( (j + 3) < grid.getNbCols() && coreArea[i][j + 3].isInstantiatedTo(1) ) {
            fill.add(grid.getIndexFromCoordinates(i, j + 2));
            fill.add(grid.getIndexFromCoordinates(i, j + 1));
        } else {
            if ( (j + 2) < grid.getNbCols() && coreArea[i][j + 2].isInstantiatedTo(1) ) {
                fill.add(grid.getIndexFromCoordinates(i, j + 1));
            }
        }
        // Toward up
        if ( (i - 3) >= 0 && coreArea[i - 3][j].isInstantiatedTo(1) ) {
            fill.add(grid.getIndexFromCoordinates(i - 2, j));
            fill.add(grid.getIndexFromCoordinates(i - 1, j));
        } else {
            if ( (i - 2) >= 0 && coreArea[i - 2][j].isInstantiatedTo(1) ) {
                fill.add(grid.getIndexFromCoordinates(i - 1, j));
            }
        }
        // Toward down
        if ( (i + 3) < grid.getNbRows() && coreArea[i + 3][j].isInstantiatedTo(1) ) {
            fill.add(grid.getIndexFromCoordinates(i + 2, j));
            fill.add(grid.getIndexFromCoordinates(i + 1, j));
        } else {
            if ( (i + 2) < grid.getNbRows() && coreArea[i + 2][j].isInstantiatedTo(1) ) {
                fill.add(grid.getIndexFromCoordinates(i + 1, j));
            }
        }
        return fill;
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

    private int getBit(int i, int j) {
        int b = 0;
        for (int ii = i - 1; ii <= i + 1; ii++) {
            for (int jj = j - 1; jj <= j + 1; jj++) {
                boolean outOfGrid = ii < 0 || ii >= grid.getNbRows() || jj < 0 || jj >= grid.getNbCols();
                int v = outOfGrid ? 0 : (coreArea[ii][jj].isInstantiatedTo(1) ? 1 : 0);
                b = (b << 1)  | v;
            }
        }
        return b;
    }
}
