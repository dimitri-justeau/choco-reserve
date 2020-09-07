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

package chocoreserve.solver.constraints.choco.fragmentation;

import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.variable.SpatialGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

/**
 * Propagator maintaining a variable equals to the Aggregation Index (AI).
 *
 * Refs: http://www.umass.edu/landeco/research/fragstats/documents/Metrics/Contagion%20-%20Interspersion%20Metrics/Metrics/C116%20-%20AI.htm
 * https://link.springer.com/article/10.1023/A:1008102521322
 *
 * @author Dimitri Justeau-Allaire
 */
public class PropAggregationIndex extends Propagator<Variable> {

    private RegularSquareGrid grid;
    private SpatialGraphVar g;
    protected IntVar aggregationIndex;
    protected int precision;

    /**
     * @param g The graph variable associated to the region for which the propagator will maintain AI.
     * @param aggregationIndex The integer variable equals to AI, maintained by this propagator.
     * @param precision Precision of AI. If precision = 2, AI will be comprised between 0 and 100,
     *                  if precision = 3, AI will be comprised between 0 and 1000, ...
     */
    public PropAggregationIndex(SpatialGraphVar g, IntVar aggregationIndex, int precision) {
        super(new Variable[] {g, aggregationIndex}, PropagatorPriority.LINEAR, false);
        this.g = g;
        this.grid = (RegularSquareGrid) g.getGrid();
        this.aggregationIndex = aggregationIndex;
        this.precision = precision;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        double[] bounds = getBounds();
        double lb = bounds[0];
        double ub = bounds[1];
        aggregationIndex.updateLowerBound((int) lb, this);
        aggregationIndex.updateUpperBound((int) ub, this);
    }

    private int getNbEdges(UndirectedGraph graph) {
        int nbE = 0;
        for (int i : graph.getNodes()) {
            nbE += graph.getNeighOf(i).size();
        }
        nbE /= 2;
        return nbE;
    }

    private int getNbEdges(ISet graphNodes, ISet toAdd) {
        int nbE = 0;
        for (ISet set : new ISet[]{graphNodes, toAdd}) {
            for (int node : set) {
                ISet potNeighs = g.getPotNeighOf(node);
                int n = 0;
                for (int i : potNeighs) {
                    if (graphNodes.contains(i) || toAdd.contains(i)) {
                        n += 1;
                    }
                }
                nbE += n;
            }
        }
        return nbE / 2;
    }


    public double[] getBounds() {
        int LB_edges;
        int LB_nodes;
        int UB_edges;
        int UB_nodes;
        double AI_LB;
        double AI_UB;
        int nbEGLB = getNbEdges(g.getGLB());
        int nbNGLB = g.getLB().size();
        // 1. Compute neutral, increasing and decreasing vertices sets
        ISet neutral = SetFactory.makeBipartiteSet(0);
        ISet increasing = SetFactory.makeBipartiteSet(0);
        ISet decreasing = SetFactory.makeBipartiteSet(0);
        ISet isolated = SetFactory.makeBipartiteSet(0);
        for (int node : g.getPotentialNodes()) {
            if (!g.getMandatoryNodes().contains(node)) {
                int potDecr = getPotDecreasing(node);
                int potIncr = getPotIncreasing(node);
                if (potDecr == potIncr) {
                    neutral.add(node);
                }
                if (potDecr < potIncr) {
                    increasing.add(node);
                    if (potDecr == 0) {
                        isolated.add(node);
                    }
                }
                if (potDecr > potIncr) {
                    decreasing.add(node);
                }
            }
        }
        // 2.a If neutral and decrease are empty, AI UB is graph UB AI.
        if (neutral.size() == 0 && decreasing.size() == 0) {
            UB_edges = nbEGLB;
            UB_nodes = g.getGLB().getNodes().size();
            AI_UB = getAI(UB_nodes, UB_edges);
        } else {
            // 2.b Else, compute AI UB by repeatedly adding decreasing and neutral vertices, until
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
            UB_edges = getNbEdges(g.getLB(), toAdd);
            UB_nodes = nbNGLB + toAdd.size();
            AI_UB = getAI(UB_nodes, UB_edges);
        }
        // 3.a If increase is empty, AI LB is graph UB AI.
        if (neutral.size() == 0 && decreasing.size() == 0) {
            LB_edges = nbEGLB;
            LB_nodes = nbNGLB;
            AI_LB = getAI(LB_nodes, LB_edges);
        } else {
            // 3.b Else, compute AI LB the following way :
            //      - Partition the grid in two sets D1 and D2 by alternating diagonals.
            //      - For D1 and D2, compute the perimeter obtained by adding only isolated vertices.
            //      - The lowest LB is the upper bound.
            ISet D1 = SetFactory.makeBipartiteSet(0);
            ISet D2 = SetFactory.makeBipartiteSet(0);
            boolean currentD1 = true;
            int nbDiags = grid.getNbCols() + grid.getNbRows() - 1;
            for (int diag = 0; diag < nbDiags; diag++) {
                int c = Math.min(diag, grid.getNbCols() - 1);
                int r = diag < grid.getNbCols() ? 0 : diag - grid.getNbCols() + 1;
                while (c >= 0 && r < grid.getNbRows()) {
                    int curr = grid.getIndexFromCoordinates(r, c);
                    if (isolated.contains(curr)) {
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
            int nbE = nbEGLB;
            int n1 = D1.size();
            int n2 = D2.size();
            int nbN = n1 > n2 ? nbNGLB + n1 : nbNGLB + n2;
            AI_LB = getAI(nbN, nbE);
        }
        return new double[]{AI_LB, AI_UB};
    }

    private int getPotIncreasing(int node) {
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

    private int getPotDecreasing(int node) {
        int n = 0;
        for (int i : g.getPotNeighOf(node)) {
            if (g.getMandatoryNodes().contains(i)) {
                n += 1;
            }
        }
        return n;
    }

    public double getAI(int nbNodes, int nbEdges) {
        int n = (int) Math.floor(Math.sqrt(nbNodes));
        int m = nbNodes - n * n;
        int maxGi;
        if (m == 0) {
            maxGi = 2 * n * (n - 1);
        } else {
            if (m <= m) {
                maxGi = 2 * n * (n - 1) + 2 * m - 1;
            } else {
                maxGi = 2 * n * (n - 1) + 2 * m - 2;
            }
        }
        return  (1.0 * nbEdges / maxGi) * Math.pow(10, precision);
    }

    @Override
    public ESat isEntailed() {
        double[] bounds = getBounds();
        double lb = bounds[0];
        double ub = bounds[1];
        if ((int) lb > aggregationIndex.getUB() || (int) ub < aggregationIndex.getLB()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
