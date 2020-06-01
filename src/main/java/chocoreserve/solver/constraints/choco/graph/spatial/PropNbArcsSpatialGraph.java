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

package chocoreserve.solver.constraints.choco.graph.spatial;

import chocoreserve.solver.variable.SpatialGraphVar;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.procedure.IntProcedure;


/**
 * Propagator that ensures that Nb arcs/edges belong to the final graph
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbArcsSpatialGraph extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected SpatialGraphVar g;
	protected IntVar k;
	private ISetDeltaMonitor sdm;
	private IntProcedure elementForced, elementRemoved;
	private IStateInt nbK, nbE;
	private ISet removed, added;


	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNbArcsSpatialGraph(SpatialGraphVar graph, IntVar k) {
		super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.k = k;
		this.sdm = g.monitorDelta(this);
		this.nbK = getModel().getEnvironment().makeInt(0);
		this.nbE = getModel().getEnvironment().makeInt(0);
		this.removed = SetFactory.makeBitSet(0);
		this.added = SetFactory.makeBitSet(0);
		this.elementForced = element -> {
			added.add(element);
		};
		this.elementRemoved = element -> {
			removed.add(element);
		};
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		int nbKinit = 0;
		int nbEinit = 0;
		for (int i : g.getPotentialNodes()) {
			nbEinit += g.getPotNeighOf(i).size();
			nbKinit += g.getMandNeighOf(i).size();
		}
		nbKinit /= 2;
		nbEinit /= 2;
		this.nbK.set(nbKinit);
		this.nbE.set(nbEinit);
		filter(nbKinit, nbEinit);
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		sdm.freeze();

		sdm.forEach(elementRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
		sdm.forEach(elementForced, SetEventType.ADD_TO_KER);

		int removedEdges = 0;
		int edgesBetweenRemoved = 0;
		for (int i : removed) {
			ISet nei = g.getNeighborhood().getNeighbors(g.getGrid(), i);
			removedEdges += nei.size();
			for (int j : nei) {
				if (!g.getPotentialNodes().contains(j)) {
					removedEdges -= 1;
				}
				if (removed.contains(j)) {
					edgesBetweenRemoved += 1;
				}
			}
		}
		edgesBetweenRemoved /= 2;
		removedEdges += edgesBetweenRemoved;
		nbE.set(nbE.get() - removedEdges);

		int addedEdges = 0;
		int edgesBetweenAdded = 0;
		for (int i : added) {
			ISet nei = g.getMandNeighOf(i);
			addedEdges += nei.size();
			for (int j : nei) {
				if (added.contains(j)) {
					edgesBetweenAdded += 1;
				}
			}
		}
		edgesBetweenAdded /= 2;
		addedEdges -= edgesBetweenAdded;
		nbK.set(nbK.get() + addedEdges);

		added.clear();
		removed.clear();

		sdm.unfreeze();

		filter(nbK.get(), nbE.get());
	}

	private void filter(int nbK, int nbE) throws ContradictionException {
		k.updateLowerBound(nbK, this);
		k.updateUpperBound(nbE, this);
		if (nbK != nbE && k.isInstantiated()) {
			ISet nei;
			ISet env = g.getPotentialNodes();
			if (k.getValue() == nbE) {
//				sdm.freeze();
				for (int i : env) {
					nei = g.getPotNeighOf(i);
					for (int j : nei) {
						g.enforceNode(j, this);
					}
				}
				this.nbK.set(nbE);
//				sdm.unfreeze();
			}
			if (k.getValue() == nbK) {
//				sdm.freeze();
				ISet neiKer;
				for (int i : env) {
					nei = g.getPotNeighOf(i);
					neiKer = g.getMandNeighOf(i);
					for (int j : nei) {
						if (!neiKer.contains(j)) {
							g.removeNode(j, this);
						}
					}
				}
				this.nbE.set(nbK);
//				sdm.unfreeze();
			}
		}
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 0) {
			return SetEventType.REMOVE_FROM_ENVELOPE.getMask() + SetEventType.ADD_TO_KER.getMask();
		} else {
			return IntEventType.boundAndInst();
		}
	}

	@Override
	public ESat isEntailed() {
		int nbK = 0;
		int nbE = 0;
		for (int i : g.getPotentialNodes()) {
			nbE += g.getPotNeighOf(i).size();
			nbK += g.getMandNeighOf(i).size();
		}
		nbK /= 2;
		nbE /= 2;
		if (nbK > k.getUB() || nbE < k.getLB()) {
			System.out.println("nbE = " + nbE + " / " + k.getUB() + " --  nbK = " + nbK + " / " + k.getLB());
			return ESat.FALSE;
		}
		if (k.isInstantiated() && g.isInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
