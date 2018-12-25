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

package chocoreserve.solver.constraints.features.bool;

import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.Feature;
import chocoreserve.solver.feature.ProbabilisticFeature;

import java.io.IOException;
import java.util.Arrays;

/**
 * Minimum probability of presence constraint.
 */
public class MinProbabilityReal extends FeaturesConstraint {

    private double alpha;

    public MinProbabilityReal(ReserveModel reserveModel, double alpha, ProbabilisticFeature... features) {
        super(reserveModel, features);
        assert alpha >= 0 && alpha <= 1;
        this.alpha = alpha;
    }

    @Override
    public void post() {
        // Constraint
        double bound = Math.log10(1 - alpha);
        for (Feature feature : features) {
            try {
                double[] coeffs = Arrays.stream(((ProbabilisticFeature) feature).getProbabilisticData())
                        .map(v -> Math.log10(1 - v))
                        .toArray();
                chocoModel.scalar(reserveModel.getSites(), coeffs, "<=", bound).post();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
