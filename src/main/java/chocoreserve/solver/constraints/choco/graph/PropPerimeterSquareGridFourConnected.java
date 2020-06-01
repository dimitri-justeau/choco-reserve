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

package chocoreserve.solver.constraints.choco.graph;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.regulare.square.FourConnected;
import chocoreserve.grid.neighborhood.regulare.square.PartialFourConnected;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.variable.SpatialGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

/**
 * Propagator maintaining an integer variable to the perimeter of a grid graph,
 * with neighborhood being the 4-connected neighborhood.
 */
public class PropPerimeterSquareGridFourConnected extends Propagator<Variable> {

    private RegularSquareGrid grid;
    private SpatialGraphVar g;
    private IntVar perimeter;

    public PropPerimeterSquareGridFourConnected(SpatialGraphVar g, IntVar perimeter) {
        super(new Variable[]{g, perimeter}, PropagatorPriority.LINEAR, false);
        this.grid = (RegularSquareGrid) g.getGrid();
        assert g.getGrid() instanceof RegularSquareGrid;
        INeighborhood nei = g.getNeighborhood();
        assert nei instanceof FourConnected || nei instanceof PartialFourConnected;
        this.g = g;
        this.perimeter = perimeter;
    }

    @Override
    public void propagate(int i) throws ContradictionException {
        int[] bounds = getBounds();
        int perimeterLB = bounds[0];
        int perimeterUB = bounds[1];
        perimeter.updateLowerBound(perimeterLB, this);
        perimeter.updateUpperBound(perimeterUB, this);
    }

    @Override
    public ESat isEntailed() {
        int[] bounds = getBounds();
        int perimeterLB = bounds[0];
        int perimeterUB = bounds[1];
        if (perimeterLB > perimeter.getUB()) {
            return ESat.FALSE;
        }
        if (perimeterUB < perimeter.getLB()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }

    int getPerimeter(ISet graphNodes, ISet toAdd) {
        int p = 0;
        for (ISet set : new ISet[]{graphNodes, toAdd}) {
            for (int node : set) {
                ISet potNeighs = g.getPotNeighOf(node);
                int frontierGrid = 4 - potNeighs.size();
                int n = 0;
                for (int i : potNeighs) {
                    if (!graphNodes.contains(i) && !toAdd.contains(i)) {
                        n += 1;
                    }
                }
                p += frontierGrid + n;
            }
        }
        return p;
    }


    public int getPerimeter(ISet graphNodes) {
        return getPerimeter(graphNodes, SetFactory.makeConstantSet(new int[]{}));
    }

    public int getPerimeterGLB() {
        return getPerimeter(g.getMandatoryNodes());
    }

    int getPerimeterGUB() {
        return getPerimeter(g.getPotentialNodes());
    }

    public int[] getBounds() {
        int LB;
        int UB;
        // 1. Compute neutral, increasing and decreasing vertices sets
        ISet neutral = SetFactory.makeBipartiteSet(0);
        ISet increasing = SetFactory.makeBipartiteSet(0);
        ISet decreasing = SetFactory.makeBipartiteSet(0);
        for (int node : g.getPotentialNodes()) {
            if (!g.getMandatoryNodes().contains(node)) {
                int potDecr = getPotDecreasing(node);
                int potIncr = getPotIncreasing(node);
                if (potDecr == potIncr) {
                    neutral.add(node);
                }
                if (potDecr < potIncr) {
                    increasing.add(node);
                }
                if (potDecr > potIncr) {
                    decreasing.add(node);
                }
            }
        }
        // 2.a If neutral and decrease are empty, perimeter LB is graph LB perimeter.
        if (neutral.size() == 0 && decreasing.size() == 0) {
            LB = getPerimeterGLB();
        } else {
            // 2.b Else, compute the perimeter LB by repeatedly adding decreasing and neutral vertices, until
            //     both sets are empty.
            ISet toAdd = SetFactory.makeBipartiteSet(0);
            for (int i : neutral) {
                toAdd.add(i);
            }
            for (int i : decreasing) {
                toAdd.add(i);
            }
            // Vertices becoming neutral of decreasing after adding neutrals and decreasing
            ISet falseIncr = SetFactory.makeBipartiteSet(0);
            do {
                for (int i : falseIncr) {
                    toAdd.add(i);
                }
                falseIncr.clear();
                // Detect false increasing
                for (int node : increasing) {
                    if (!g.getMandatoryNodes().contains(node) && !toAdd.contains(node)) {
                        ISet potNeigh = g.getPotNeighOf(node);
                        int frontierGrid = 4 - potNeigh.size();
                        int potDecr = 0;
                        int potIncr = frontierGrid;
                        for (int i : potNeigh) {
                            if (g.getMandatoryNodes().contains(i) || toAdd.contains(i)) {
                                potDecr += 1;
                            } else {
                                potIncr += 1;
                            }
                        }
                        if (potDecr >= potIncr) {
                            falseIncr.add(node);
                        }
                    }
                }
            } while (falseIncr.size() != 0);
            LB = getPerimeter(g.getLB(), toAdd);
        }
        // 3.a If increase is empty, perimeter UB is graph LB perimeter.
        if (neutral.size() == 0 && decreasing.size() == 0) {
            UB = getPerimeterGLB();
        } else {
            // 3.b Else, compute the perimeter UB the following way :
            //      - Partition the grid in two sets D1 and D2 by alternating diagonals.
            //      - For D1 and D2, compute the perimeter obtained by adding only increasing vertices.
            //      - The highest perimeter is the upper bound.
            ISet D1 = SetFactory.makeBipartiteSet(0);
            ISet D2 = SetFactory.makeBipartiteSet(0);
            boolean currentD1 = true;
            int nbDiags = grid.getNbCols() + grid.getNbRows() - 1;
            for (int diag = 0; diag < nbDiags; diag++) {
                int c = Math.min(diag, grid.getNbCols() - 1);
                int r = diag < grid.getNbCols() ? 0 : diag - grid.getNbCols() + 1;
                while (c >= 0 && r < grid.getNbRows()) {
                    int curr = grid.getIndexFromCoordinates(r, c);
                    if (increasing.contains(curr)) {
                        if (currentD1) {
                            D1.add(curr);
                        } else {
                            D2.add(curr);
                        }
                    }
                    c -= 1;
                    r += 1;
                }
                currentD1 = !currentD1;
            }
            int p1 = getPerimeter(g.getLB(), D1);
            int p2 = getPerimeter(g.getLB(), D2);
            UB = p1 > p2 ? p1 : p2;
        }
        return new int[]{LB, UB};
    }

    int getPotIncreasing(int node) {
        ISet neighs = g.getPotNeighOf(node);
        int frontierGrid = 4 - neighs.size();
        int n = 0;
        for (int i : neighs) {
            if (!g.getMandatoryNodes().contains(i)) {
                n += 1;
            }
        }
        return frontierGrid + n;
    }

    int getPotDecreasing(int node) {
        int n = 0;
        for (int i : g.getPotNeighOf(node)) {
            if (g.getMandatoryNodes().contains(i)) {
                n += 1;
            }
        }
        return n;
    }
}
