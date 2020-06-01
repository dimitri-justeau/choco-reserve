package chocoreserve.solver.constraints.choco;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;

import java.util.HashSet;

/**
 * @author P. Vismara (2018) <a href=
 * "mailto:philippe.vismara@supagro.fr">philippe.vismara@supagro.fr</a><br/>
 * Ensures that N = Neighbors(X) <br/>
 * where Neighbors(X) = U_{x in X} adjList[x]<br/>
 * Caution: X and Neighbors(X) are generally not disjoined
 */
public class PropNeighbors extends Propagator<SetVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private ISetDeltaMonitor[] sdm;
    private IntProcedure elementForcedX, elementRemovedX, elementForcedN, elementRemovedN;

    int[][] adjLists;

    //***********************************************************************************
    // CONSTRUCTOR
    //***********************************************************************************

    /**
     * Ensures that N = Neighbors(X) <br/>
     * where Neighbors(X) = U_{x in X} adjList[x]
     *
     * @param X        a region variable
     * @param N        a region variable
     * @param adjLists = adjacency list. For example, <code>adjLists[x][0] = y</code>
     *                 means that <code>y</code> is the first neighbor of
     *                 <code>x</code> and <code>adjLists[x].length</code> is the
     *                 degree of vertex <code>x</code>.
     */
    public PropNeighbors(SetVar X, SetVar N, int[][] adjLists) {
        super(new SetVar[]{X, N}, PropagatorPriority.LINEAR, false);
        //
        this.adjLists = adjLists;

        // delta monitors
        sdm = new ISetDeltaMonitor[2];
        for (int i = 0; i < 2; i++) {
            sdm[i] = this.vars[i].monitorDelta(this);
        }
        // adding an element to X
        elementForcedX = element -> {
            // on ajoute a N les voisins de element
            // c'est idempotent puisque elementForcedN ne va rien faire dans ce cas
            for (int v : this.adjLists[element]) {
                vars[1].force(v, this);
            }
        };
        // removing an element from X
        elementRemovedX = element -> {
            // dans tous les cas, si element etait le seul support d'un élement v de UB(N) il faut le supprimer de N
            for (int v : this.adjLists[element]) {
                if (vars[1].getUB().contains(v)) { // v in UB(N)
                    // on verifie qu'il reste au moins un support pour v dans X
                    int neighborsOfVInX = 0;
                    for (int w : this.adjLists[v]) {
                        if (w != element && vars[0].getUB().contains(w)) {
                            neighborsOfVInX++;
                            break;
                        }
                    }
                    // on supprime v de N s'il n'a plus aucun voisin dans UB(X)
                    if (neighborsOfVInX == 0) {
                        vars[1].remove(v, this);
                        // pour l'idempotence il faut retirer de X tous les voisins de v
                        elementRemovedN.execute(v);
                    }
                }
            }
        };
        elementForcedN = element -> {
            // il faut au moins un voisin dans X, s'il n'y en a qu'un on le force
            int x = -1; // voisin trouve
            int nbUBX = 0;
            boolean inLBX = false;
            // on compte les voisins de element dans X
            for (int v : this.adjLists[element]) {
                if (vars[0].getUB().contains(v)) {
                    x = v;
                    nbUBX++;
                    if (nbUBX > 1) {
                        break;
                    } else if (vars[0].getLB().contains(v)) {
                        inLBX = true;
                        break;
                    }
                }
            }
            if (!inLBX) {
                if (nbUBX == 0) {
                    this.fails(); // no neighbor in X
                } else if (nbUBX == 1) { // only one neighbor in UB(X)\LB(X)
                    // x n'est pas dans vars[0]
                    vars[0].force(x, this);
                    // pour l'idempotence il faut ajouter les voisins de x à N
                    elementForcedX.execute(x);
                }
            }
        };

        elementRemovedN = element -> {
            // si element ne peut pas etre dans N, ces voisins ne peuvent etre dans X
            for (int v : this.adjLists[element]) {
                if (vars[0].getUB().contains(v)) {
                    vars[0].remove(v, this);
                    // pour l'idempotence il faut retirer de N les voisins de v n'ayant pas d'autre voisin dans X
                    elementRemovedX.execute(v);
                }
            }
        };
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return SetEventType.all();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // liste elements a supprimer
        HashSet<Integer> toRemove = new HashSet<Integer>();

        for (int x : vars[0].getUB()) {
            if (vars[0].getLB().contains(x)) {
                // on force les voisins de LB(X)
                elementForcedX.execute(x);
            } else {
                // on verifie que chaque elements de UB(X) a tous ses voisins dans UB(N)
                for (int w : this.adjLists[x]) {
                    if (!vars[1].getUB().contains(w)) {
                        toRemove.add(x);
                        break;
                    }
                }
            }
        }
        // suppression
        for (int x : toRemove) {
            vars[0].remove(x, this);
        }
        // on teste les élements de UB(N)
        // liste des elements a supprimer
        toRemove.clear();

        for (int z : vars[1].getUB()) {
            if (vars[1].getLB().contains(z))
                elementForcedN.execute(z);
            else {
                // on verifie juste l'existance d'un voisin dans UB(X)
                int neighborsOfzInX = 0;
                for (int w : this.adjLists[z]) {
                    if (vars[0].getUB().contains(w)) {
                        neighborsOfzInX++;
                        break;
                    }
                }
                // on supprime z de N s'il n'a aucun voisin dans UB(X)
                if (neighborsOfzInX == 0)
                    toRemove.add(z);
            }
        }
        // suppression
        for (int z : toRemove) {
            vars[1].remove(z, this);
            // pour l'idempotence il faut retirer de X tous les voisins de z
            elementRemovedN.execute(z);
        }
        sdm[0].unfreeze();
        sdm[1].unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (idxVarInProp == 0) {
            sdm[0].freeze();
            sdm[0].forEach(elementForcedX, SetEventType.ADD_TO_KER);
            sdm[0].forEach(elementRemovedX, SetEventType.REMOVE_FROM_ENVELOPE);
            sdm[0].unfreeze();
        } else if (idxVarInProp == 1) {
            sdm[1].freeze();
            sdm[1].forEach(elementForcedN, SetEventType.ADD_TO_KER);
            sdm[1].forEach(elementRemovedN, SetEventType.REMOVE_FROM_ENVELOPE);
            sdm[1].unfreeze();
        }
    }

    @Override
    public ESat isEntailed() {
        for (int x : vars[0].getLB()) {
            for (int v : adjLists[x]) {
                if (vars[1].getUB().contains(v)) {
                    if (!vars[1].getLB().contains(v)) {
                        return ESat.UNDEFINED;
                    }
                } else {
                    return ESat.FALSE;
                }
            }
        }
        for (int v : vars[1].getLB()) {
            int nbUBX = 0; // nombre de voisins dans UB(X)
            boolean inX = false; // au moins un voisin dans LB(X) ?
            for (int x : adjLists[v]) {
                if (vars[0].getUB().contains(x)) {
                    if (vars[0].getLB().contains(x)) {
                        inX = true;
                        break;
                    }
                    nbUBX++;
                }
            }
            if (!inX) {
                if (nbUBX == 0)
                    return ESat.FALSE;
                else
                    return ESat.UNDEFINED;
            }
        }
        if (vars[0].getLB().size() < vars[0].getUB().size())
            return ESat.UNDEFINED;
        if (vars[1].getLB().size() < vars[1].getUB().size())
            return ESat.UNDEFINED;
        return ESat.TRUE;
    }

}
