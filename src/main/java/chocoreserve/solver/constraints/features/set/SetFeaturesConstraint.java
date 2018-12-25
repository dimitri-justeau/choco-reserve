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

package chocoreserve.solver.constraints.features.set;

import chocoreserve.solver.SetReserveModel;
import chocoreserve.solver.constraints.SetReserveConstraint;
import chocoreserve.solver.feature.Feature;

/**
 * Abstract base class for features representation constraints.
 */
public abstract class SetFeaturesConstraint extends SetReserveConstraint {

    protected Feature[] features;

    public SetFeaturesConstraint(SetReserveModel reserveModel, Feature... features) {
        super(reserveModel);
        this.features = features;
        for (Feature feature : features) {
            if (!reserveModel.getFeatures().containsKey(feature.getName())) {
                reserveModel.addFeature(feature);
            }
        }
    }
}
