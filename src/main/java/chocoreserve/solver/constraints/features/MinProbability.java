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
import chocoreserve.solver.feature.ProbabilisticFeature;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.util.Arrays;

/**
 * Minimum probability of presence constraint.
 */
public class MinProbability extends FeaturesConstraint {

    private double alpha;
    private AbstractRegion region;
    private IntVar[] N;

    public MinProbability(ReserveModel reserveModel, AbstractRegion region, double alpha,
                          ProbabilisticFeature... features) {
        super(reserveModel, features);
        assert alpha > 0 && alpha < 1;
        this.region = region;
        this.alpha = alpha;
        this.N = reserveModel.getChocoModel().intVarArray(features.length, 0, reserveModel.getGrid().getNbCells() * 3000);
    }

    @Override
    public void post() {
        int scaled = (int) (-1000 * Math.log10(1 - 0.01 * Math.round(100 * alpha)));
        for (int i = 0; i < features.length; i++) {
            try {
                int[] coeffs = Arrays.stream(((ProbabilisticFeature) features[i]).getProbabilisticData())
                        .mapToInt(v -> (v == 1) ? 3000 : (int) (-1000 * Math.log10(1 - 0.01 * Math.round(100 * v))))
                        .toArray();
                chocoModel.sumElements(region.getSetVar(), coeffs, N[i]).post();
                chocoModel.arithm(N[i], ">=", scaled).post();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
