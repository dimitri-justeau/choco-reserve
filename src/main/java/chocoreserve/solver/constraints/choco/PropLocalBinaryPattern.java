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

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 *
 */
public class PropLocalBinaryPattern extends Propagator<BoolVar> {

    private BoolVar[][] vars;
    private int[] masks;
    private int[][][] neighborhoods;
    private int nbRows, nbCols;

    public PropLocalBinaryPattern(BoolVar[][] vars, int[] masks, int[][][] neighborhoods) {
        super(ArrayUtils.flatten(vars), PropagatorPriority.LINEAR, true);
        this.vars = vars;
        this.masks = masks;
        assert vars.length > 0;
        assert vars[0].length > 0;
        assert masks.length == neighborhoods.length;
        this.nbRows = vars.length;
        this.nbCols = vars[0].length;
        this.neighborhoods = neighborhoods;
    }

    @Override
    public void propagate(int evtMask) throws ContradictionException {
        for (int i = 0; i < nbRows; i++) {
            for (int j = 0; j < nbCols; j++) {
                propagate(getIndex(i, j), evtMask);
            }
        }
    }

    @Override
    public void propagate(int vidx, int evtMask) throws ContradictionException {
        Set<Integer> backPropagate = new HashSet<>();
        int i = getRow(vidx);
        int j = getCol(vidx);
        for (int m = 0; m < masks.length; m++) {
            int mask = masks[m];
            List<BoolVar[]> neighs = getNeighborhoods(m, i, j);
            for (BoolVar[] neigh : neighs) {
                int[] notInstantiated = notInstantiatedVars(neigh);
                switch (notInstantiated.length) {
                    case 0:
                        if (mask == getInstantiatedNeighBinaryPattern(neigh)) {
                            fails();
                        }
                        break;
                    case 1:
                        if (mask == getUBNeighBinaryPattern(neigh)) {
                            neigh[notInstantiated[0]].setToFalse(this);
                            backPropagate.add(vidx);
                            break;
                        }
                        if (mask == getLBNeighBinaryPattern(neigh)) {
                            neigh[notInstantiated[0]].setToTrue(this);
                            backPropagate.add(vidx);
                            break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        for (int varId : backPropagate) {
            propagate(varId, evtMask);
        }
    }

    @Override
    public ESat isEntailed() {
        return ESat.UNDEFINED;
    }

    private List<BoolVar[]> getNeighborhoods(int maskId, int i, int j) {
        int sizeNeigh = neighborhoods[maskId].length;
        List<BoolVar[]> neighs = new ArrayList<>();
        for (int start = 0; start < sizeNeigh; start++) {
            BoolVar[] neigh = new BoolVar[sizeNeigh];
            int pos = start;
            int curr_i = i;
            int curr_j = j;
            do {
                if (curr_i < 0 || curr_i >= nbRows || curr_j < 0 || curr_j >= nbCols) {
                    break;
                }
                neigh[pos] = vars[curr_i][curr_j];
                int di = neighborhoods[maskId][pos][0];
                int dj = neighborhoods[maskId][pos][1];
                curr_i += di;
                curr_j += dj;
                if (pos == sizeNeigh - 1) {
                    pos = 0;
                } else {
                    pos++;
                }
            } while (pos != start);
            if (pos == start) {
                neighs.add(neigh);
            }
        }
        return neighs;
    }

    private int[] notInstantiatedVars(BoolVar[] neigh) {
        return IntStream.range(0, neigh.length).filter(i -> !neigh[i].isInstantiated()).toArray();
    }

    private int getRow(int index) {
        return Math.floorDiv(index, nbCols);
    }

    private int getCol(int index) {
        return index % nbCols;
    }

    private int getIndex(int row, int col) {
        return row * nbCols + col;
    }

    private int getInstantiatedNeighBinaryPattern(BoolVar[] neigh) {
        int b = 0;
        for (BoolVar v : neigh) {
            b = (b << 1) | v.getValue();
        }
        return b;
    }

    private int getUBNeighBinaryPattern(BoolVar[] neigh) {
        int b = 0;
        for (BoolVar v : neigh) {
            b = (b << 1) | v.getUB();
        }
        return b;
    }

    private int getLBNeighBinaryPattern(BoolVar[] neigh) {
        int b = 0;
        for (BoolVar v : neigh) {
            b = (b << 1) | v.getLB();
        }
        return b;
    }
}

