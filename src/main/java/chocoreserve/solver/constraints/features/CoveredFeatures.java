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

package chocoreserve.solver.constraints.features;

import chocoreserve.solver.IReserveModel;
import chocoreserve.solver.feature.BinaryFeature;
import chocoreserve.solver.feature.Feature;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Covered features constraint.
 */
public class CoveredFeatures extends FeaturesConstraint {

    /** IntVar representing the size of the minimum size covering */
    private IntVar N;

    private boolean atmost;

    public CoveredFeatures(IReserveModel reserveModel, boolean atmost, Feature... features) {
        super(reserveModel, features);
        this.atmost = atmost;
        this.N = chocoModel.intVar(0, reserveModel.getGrid().getNbCells());
    }

    public CoveredFeatures(IReserveModel reserveModel, Feature... features) {
        this(reserveModel, true, features);
        this.N = chocoModel.intVar(0, reserveModel.getGrid().getNbCells());
    }

    @Override
    public void post() {
        if (atmost) {
            post_atmostnvalues();
        } else {
            post_arithm();
        }
    }

    private void post_arithm() {
        for (Feature feature : features) {
            try {
                int[] coeffs = ((BinaryFeature) feature).getBinaryData();
                chocoModel.scalar(reserveModel.getPlanningUnits(), coeffs, ">=", 1).post();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void post_atmostnvalues() {
        int nbCells = reserveModel.getGrid().getNbCells();
        IntVar[] vars = new IntVar[features.length];
        for (int f = 0; f < features.length; f++) {
            Feature feature = features[f];
            try {
                double[] data = feature.getData();
                int[] sites = IntStream.range(0, nbCells)
                        .filter(i -> data[i] > 0)
                        .toArray();
                if (sites.length > 0) {
                    vars[f] = chocoModel.intVar(sites);
                } else {
                    // Forces the solver to fail -- To make more explicit.
                    chocoModel.arithm(chocoModel.intVar(1), "=", 0).post();
                    return;
                }
                // Channelling with the boolvars
                for (int i : sites) {
                    BoolVar notI = chocoModel.boolNotView(reserveModel.getPlanningUnits()[i]);
                    chocoModel.ifThen(notI, chocoModel.arithm(vars[f], "!=", i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // At most constraint
        chocoModel.atMostNValues(vars, N, true).post();
        // N is a lower bound for the number of sites
        chocoModel.arithm(reserveModel.getNbPlanningUnits(), ">=", N).post();
    }
}
