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
import chocoreserve.solver.region.AbstractRegion;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * All known occurrences of each feature must be covered.
 */
public class AllCovered extends FeaturesConstraint {

    private AbstractRegion region;

    public AllCovered(ReserveModel reserveModel, AbstractRegion region, Feature... features) {
        super(reserveModel, features);
        this.region = region;
    }

    @Override
    public void post() {
        Set<Integer> mandatorySites = new HashSet<>();
        for (Feature f : features) {
            BinaryFeature bf = (BinaryFeature) f;
            try {
                for (int i = 0; i < bf.getBinaryData().length; i++) {
                    if (bf.getBinaryData()[i] >= 1) {
                        mandatorySites.add(i);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i : mandatorySites) {
            reserveModel.getChocoModel().member(i, region.getSetVar()).post();
        }
        System.out.println("Mandatory: " + mandatorySites.size());
    }
}
