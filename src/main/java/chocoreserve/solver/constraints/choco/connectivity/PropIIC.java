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

package chocoreserve.solver.constraints.choco.connectivity;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.variable.SpatialGraphVar;
import chocoreserve.util.ConnectivityFinderSpatialGraph;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Propagator maintaining a variable equals to the Effective Mesh Size (MESH), using the classical CUT procedure.
 *
 * Ref: https://link.springer.com/article/10.1023/A:1008129329289
 *
 * @author Dimitri Justeau-Allaire
 */
public class PropIIC extends Propagator<Variable> {

    protected SpatialGraphVar g;
    protected IntVar iic;
    protected int landscapeArea;
    protected int precision;
    protected RegularSquareGrid grid;
    protected INeighborhood threshold;
    public ConnectivityFinderSpatialGraph connectivityFinderGUB;
    public ConnectivityFinderSpatialGraph connectivityFinderGLB;
    public int[][] threshNeigh;
    private boolean maximize;

    /**
     *
     * @param g The graph variable associated to the region for which the propagator will maintain MESH.
     * @param iic The integer variable equals to MESH, maintained by this propagator.
     * @param landscapeArea The total landscape area.
     */
    public PropIIC(SpatialGraphVar g, IntVar iic, int landscapeArea, INeighborhood distanceThreshold, int precison, boolean maximize) {
        super(new Variable[] {g, iic}, PropagatorPriority.QUADRATIC, false);
        this.g = g;
        this.grid = (RegularSquareGrid) g.getGrid();
        this.iic = iic;
        this.landscapeArea = landscapeArea;
        this.precision = precison;
        this.threshold = distanceThreshold;
        this.connectivityFinderGUB = new ConnectivityFinderSpatialGraph(g.getGUB());
        this.connectivityFinderGLB = new ConnectivityFinderSpatialGraph(g.getGLB());
        this.threshNeigh = new int[grid.getNbCells()][];
        this.maximize = maximize;
    }

    public PropIIC(SpatialGraphVar g, IntVar iic, int landscapeArea, INeighborhood distanceThreshold, int precison) {
        this(g, iic, landscapeArea, distanceThreshold, precison, false);
    }


    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // LB
        if (!maximize || g.isInstantiated()) {
	        int iic_LB = (int) Math.round(getIICLB() * Math.pow(10, precision));
            iic.updateLowerBound(iic_LB, this);
        }        
        // UB
        int iic_UB = (int) Math.round(getIICUB() * Math.pow(10, precision));
        iic.updateUpperBound(iic_UB, this);
    }

    public float getIICLB() {
        connectivityFinderGLB.findAllCC();
        int[] sizeCC = connectivityFinderGLB.getSizeCC();
        int[][] adj = getLinkedLB();
        float iic_LB = 0;
        for (int i = 0; i < adj.length; i++) {
            int[] dists = bfs(i, adj);
            for (int j = 0; j < adj.length; j++) {
                if (dists[j] >= 0) {
                    iic_LB +=  (sizeCC[i] * sizeCC[j]) / (1 + dists[j]);
                }
            }
        }
        return iic_LB / (landscapeArea * landscapeArea);
    }

    public float getIICUB() {
        connectivityFinderGUB.findAllCC();
        int[] sizeCC = connectivityFinderGUB.getSizeCC();
        int[][] adj = getLinkedUB();
        float iic_UB = 0;
        for (int i = 0; i < adj.length; i++) {
            int[] dists = bfs(i, adj);
            for (int j = 0; j < adj.length; j++) {
                if (dists[j] >= 0) {
                    iic_UB +=  (sizeCC[i] * sizeCC[j]) / (1 + dists[j]);
                }
            }
        }
        return iic_UB  / (landscapeArea * landscapeArea);
    }

    public int[][] getLinkedUB() {
        int nbCC = connectivityFinderGUB.getNBCC();
        int[][] neigh = new int[nbCC][];
        int[] nodeCC = connectivityFinderGUB.getNodeCC();
        for (int i = 0; i < nbCC; i++) {
            boolean[] conn = new boolean[nbCC];
            int nAdj = 0;
            int[] cc = connectivityFinderGUB.getCC(i);
            for (int node : cc) {
                if (threshNeigh[node] == null) {
                    threshNeigh[node] = threshold.getNeighbors(grid, node).toArray();
                }
                for (int j : threshNeigh[node]) {
                    if (nodeCC[j] != i && g.getPotentialNodes().contains(j)) {
                        if (!conn[nodeCC[j]]) {
                            conn[nodeCC[j]] = true;
                            nAdj += 1;
                        }
                    }
                }
            }
            int[] adj = new int[nAdj];
            int k = 0;
            for (int j = 0; j < nbCC; j++) {
                if (conn[j]) {
                    adj[k++] = j;
                }
            }
            neigh[i] = adj;
        }
        return neigh;
    }

    public int[][] getLinkedLB() {
        int nbCC = connectivityFinderGLB.getNBCC();
        int[][] neigh = new int[nbCC][];
        int[] nodeCC = connectivityFinderGLB.getNodeCC();
        for (int i = 0; i < nbCC; i++) {
            boolean[] conn = new boolean[nbCC];
            int nAdj = 0;
            int[] cc = connectivityFinderGLB.getCC(i);
            for (int node : cc) {
                if (threshNeigh[node] == null) {
                    threshNeigh[node] = threshold.getNeighbors(grid, node).toArray();
                }
                for (int j : threshNeigh[node]) {
                    if (nodeCC[j] != i && g.getMandatoryNodes().contains(j)) {
                        if (!conn[nodeCC[j]]) {
                            conn[nodeCC[j]] = true;
                            nAdj += 1;
                        }
                    }
                }
            }
            int[] adj = new int[nAdj];
            int k = 0;
            for (int j = 0; j < nbCC; j++) {
                if (conn[j]) {
                    adj[k++] = j;
                }
            }
            neigh[i] = adj;
        }
        return neigh;
    }

    public int[] bfs(int source, int[][] adj) {
        int n = adj.length;
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int front = 0;
        int rear = 0;
        int[] dist = new int[n];
        for (int i = 0; i < n; i++) {
            dist[i] = - 1;
        }
        int current;
        visited[source] = true;
        queue[front] = source;
        rear++;
        dist[source] = 0;
        while (front != rear) {
            current = queue[front++];
            for (int i : adj[current]) {
                if (!visited[i]) {
                    dist[i] = dist[current] + 1;
                    queue[rear++] = i;
                    visited[i] = true;
                    int d = dist[i];
                }
            }
        }
        return dist;
    }


    @Override
    public ESat isEntailed() {
        int iic_LB = (int) Math.round(getIICLB() * Math.pow(10, precision));
        int iic_UB = (int) Math.round(getIICUB() * Math.pow(10, precision));
        if (iic_LB > iic.getUB() || iic_UB < iic.getLB()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
