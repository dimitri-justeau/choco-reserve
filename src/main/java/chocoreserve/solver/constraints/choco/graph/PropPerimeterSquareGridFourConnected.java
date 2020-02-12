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
import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Propagator maintaining an integer variable to the perimeter of a grid graph,
 * with neighborhood being the 4-connected neighborhood.
 */
public class PropPerimeterSquareGridFourConnected extends Propagator<Variable> {

    private RegularSquareGrid grid;
    private INeighborhood neigh;
    private UndirectedGraphVar g;
    private IntVar perimeter;

    public PropPerimeterSquareGridFourConnected(RegularSquareGrid grid, UndirectedGraphVar g, IntVar perimeter) {
        super(new Variable[] {g, perimeter}, PropagatorPriority.LINEAR, false);
        this.grid = grid;
        if (grid instanceof PartialRegularSquareGrid) {
            this.neigh = Neighborhoods.PARTIAL_FOUR_CONNECTED;
        } else {
            this.neigh = Neighborhoods.FOUR_CONNECTED;
        }
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
//        if (perimeterLB == perimeterUB) {
//            setPassive();
//        } else if (perimeter.isInstantiated()) {
//            int v = perimeter.getValue();
//            if (v == perimeterUB) {
//                for (int node : g.getPotentialNodes()) {
//                    g.enforceNode(node, this);
//                }
//                setPassive();
//            } else if (v == perimeterLB) {
//                for (int node : g.getPotentialNodes()) {
//                    if (!g.getMandatoryNodes().contains(node)) {
//                        g.removeNode(node, this);
//                    }
//                }
//                setPassive();
//            }
//        }
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

    int getPerimeter(UndirectedGraph graph, Set<Integer> toAdd) {
        int p = 0;
        Set<Integer> nodes = new HashSet<>();
        Arrays.stream(graph.getNodes().toArray()).forEach(i -> nodes.add(i));
        nodes.addAll(toAdd);
        for (int node : nodes) {
            int[] potNeighs = neigh.getNeighbors(grid, node);
            int frontierGrid = 4 - potNeighs.length;
            int n = 0;
            for (int i : potNeighs) {
                if (!nodes.contains(i)) {
                    n += 1;
                }
            }
            p += frontierGrid + n;
        }
        return p;
    }


    int getPerimeter(UndirectedGraph graph) {
        return getPerimeter(graph, new HashSet<>());
    }

    int getPerimeterGLB() {
        return getPerimeter(g.getLB());
    }

    int getPerimeterGUB() {
        return getPerimeter(g.getUB());
    }

    int[] getBounds() {
        int LB;
        int UB;
        // 1. Compute neutral, increasing and decreasing vertices sets
        Set<Integer> neutral = new HashSet<>();
        Set<Integer> increasing = new HashSet<>();
        Set<Integer> decreasing = new HashSet<>();
        for (int node : g.getUB().getNodes()) {
            if (!g.getLB().getNodes().contains(node)) {
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
            int decr = 0;
            Set<Integer> toAdd = new HashSet<>();
            toAdd.addAll(neutral);
            toAdd.addAll(decreasing);
            // Vertices becoming neutral of decreasing after adding neutrals and decreasing
            Set<Integer> falseIncr = new HashSet<>();
            do {
                toAdd.addAll(falseIncr);
                falseIncr.clear();
                // Detect false increasing
                for (int node : increasing) {
                    if (!g.getLB().getNodes().contains(node) && !toAdd.contains(node)) {
                        int frontierGrid = 4 - neigh.getNeighbors(grid, node).length;
                        int potDecr = 0;
                        int potIncr = frontierGrid;
                        for (int i : g.getUB().getNeighOf(node)) {
                            if (g.getLB().getNodes().contains(i) || toAdd.contains(i)) {
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
            Set<Integer> D1 = new HashSet<>();
            Set<Integer> D2 = new HashSet<>();
            boolean currentD1 = true;
            for (int col = 0; col < grid.getNbCols(); col++) {
                int c = col;
                int r = 0;
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
        return new int[] {LB, UB};
    }

    int getPotIncreasing(int node) {
        ISet neighs = g.getUB().getNeighOf(node);
        int frontierGrid = 4 - neighs.size();
        int n = 0;
        for (int i : neighs) {
            if (!g.getLB().getNodes().contains(i)) {
                n += 1;
            }
        }
        return frontierGrid + n;
    }

    int getPotDecreasing(int node) {
        int n = 0;
        for (int i : g.getUB().getNeighOf(node)) {
            if (g.getLB().getNodes().contains(i)) {
                n += 1;
            }
        }
        return n;
    }
}
