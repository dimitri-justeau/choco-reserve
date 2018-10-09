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

package chocoreserve.solver.constraints;

import chocoreserve.solver.IReserveModel;
import chocoreserve.solver.constraints.features.CoveredFeatures;
import chocoreserve.solver.constraints.features.MinProbability;
import chocoreserve.solver.constraints.features.RedundantFeatures;
import chocoreserve.solver.constraints.spatial.AreaReserveSystem;
import chocoreserve.solver.constraints.spatial.AreaReserves;
import chocoreserve.solver.constraints.spatial.NbReserves;
import chocoreserve.solver.feature.IBinaryFeature;
import chocoreserve.solver.feature.IProbabilisticFeature;
import org.chocosolver.solver.variables.IntVar;

/**
 * Interface for constraints over the Nature Reserve Problem.
 */
public interface IReserveConstraintFactory {

    IReserveModel self();

    // ---------------------------------- //
    // Feature representation constraints //
    // ---------------------------------- //

    default IReserveConstraint coveredFeatures(IBinaryFeature... features) {
        return new CoveredFeatures(self(), features);
    }

    default IReserveConstraint redundantFeatures(int k, IBinaryFeature... features) {
        return new RedundantFeatures(self(), k, features);
    }

    default IReserveConstraint minProbability(double alpha, IProbabilisticFeature... features) {
        return new MinProbability(self(), alpha, features);
    }

    // ------------------- //
    // Spatial constraints //
    // ------------------- //

    default IReserveConstraint nbReserves(int nbMin, int nbMax) {
        return new NbReserves(self(), nbMin, nbMax);
    }

    default IReserveConstraint areaReserves(IntVar minNCC, IntVar maxNCC) {
        return new AreaReserves(self(), minNCC, maxNCC);
    }

    default IReserveConstraint areaReserveSystem(int areaMin, int areaMax){
        return new AreaReserveSystem(self(), areaMin, areaMax);
    }
}
