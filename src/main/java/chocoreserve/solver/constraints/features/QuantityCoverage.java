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
import chocoreserve.solver.feature.QuantitativeFeature;
import chocoreserve.solver.region.AbstractRegion;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class QuantityCoverage extends FeaturesConstraint {

    private AbstractRegion region;
    public IntVar N;
    private QuantitativeFeature feature;

    public QuantityCoverage(ReserveModel reserveModel, AbstractRegion region, QuantitativeFeature feature) {
        super(reserveModel, feature);
        this.region = region;
        this.feature = feature;
        try {
            this.N = chocoModel.intVar(
                    "qty_" + feature.getName(),
                    0,
                    Arrays.stream(feature.getQuantitativeData()).sum()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void post() {
        try {
            chocoModel.sumElements(region.getSetVar(), feature.getQuantitativeData(), N).post();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
