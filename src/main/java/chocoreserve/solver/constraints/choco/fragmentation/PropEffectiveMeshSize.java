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

import chocoreserve.solver.variable.SpatialGraphVar;
import chocoreserve.util.ConnectivityFinderSpatialGraph;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.Map;
import java.util.Set;

/**
 * Propagator maintaining a variable equals to the Effective Mesh Size (MESH), using the classical CUT procedure.
 *
 * Ref: https://link.springer.com/article/10.1023/A:1008129329289
 *
 * @author Dimitri Justeau-Allaire
 */
public class PropEffectiveMeshSize extends Propagator<Variable> {

    protected SpatialGraphVar g;
    protected IntVar mesh;
    protected int landscapeArea;
    protected int precision;
    public ConnectivityFinderSpatialGraph connectivityFinderGUB;


    /**
     *
     * @param g The graph variable associated to the region for which the propagator will maintain MESH.
     * @param mesh The integer variable equals to MESH, maintained by this propagator.
     * @param landscapeArea The total landscape area.
     */
    public PropEffectiveMeshSize(SpatialGraphVar g, IntVar mesh, int landscapeArea, int precison) {
        super(new Variable[] {g, mesh}, PropagatorPriority.VERY_SLOW, false);
        this.g = g;
        this.mesh = mesh;
        this.landscapeArea = landscapeArea;
        this.precision = precison;
        this.connectivityFinderGUB = new ConnectivityFinderSpatialGraph(g.getGUB());
    }

    public PropEffectiveMeshSize(SpatialGraphVar g, IntVar mesh, int landscapeArea) {
        this(g, mesh, landscapeArea, 2);
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // LB
        double mesh_LB = 0;
        Map<Integer, Set<Integer>> ccs = g.getGLB().getConnectedComponents();
        for (int r : ccs.keySet()) {
            int patchSize = ccs.get(r).size();
            mesh_LB += patchSize * patchSize;
        }
        mesh_LB /= 1.0 * landscapeArea;
        mesh.updateLowerBound((int) (mesh_LB * Math.pow(10, precision)), this);
        // UB
        double mesh_UB = 0;
        connectivityFinderGUB.findAllCC();
        for (int i = 0; i < connectivityFinderGUB.getNBCC(); i++) {
            int s = connectivityFinderGUB.getSizeCC()[i];
            mesh_UB += s * s;
        }
        mesh_UB /= 1.0 * landscapeArea;
        mesh.updateUpperBound((int) (mesh_UB * Math.pow(10, precision)), this);
    }

    @Override
    public ESat isEntailed() {
        if (g.isInstantiated()) {
            if (mesh.isInstantiated()) {
                double mesh_LB = 0;
                Map<Integer, Set<Integer>> ccs = g.getGLB().getConnectedComponents();
                for (int r : ccs.keySet()) {
                    int patchSize = ccs.get(r).size();
                    mesh_LB += patchSize * patchSize;
                }
                mesh_LB /= 1.0 * landscapeArea;
                if (mesh.getLB() == (int) (mesh_LB * Math.pow(10, precision))) {
                    return ESat.TRUE;
                }
            }
            return ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }
}
