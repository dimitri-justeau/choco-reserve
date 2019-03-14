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

import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.BinaryFeature;
import chocoreserve.solver.feature.Feature;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;

import java.io.IOException;

/**
 *
 */
public class RedundantFeatures extends FeaturesConstraint {

    protected SetVar set;
    protected int k;
    public IntVar[] N;

    public RedundantFeatures(ReserveModel reserveModel, SetVar set, int k, Feature... features) {
        super(reserveModel, features);
        this.set = set;
        this.k = k;
        this.N = reserveModel.getChocoModel().intVarArray(features.length, k, reserveModel.getGrid().getNbCells());
    }

    @Override
    public void post() {
        for (int i = 0; i < features.length; i++) {
            try {
                int[] data = ((BinaryFeature) features[i]).getBinaryData();
                int[] coeffs = reserveModel.getGrid().getBordered(data);
                chocoModel.sumElements(set, coeffs, N[i]).post();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
