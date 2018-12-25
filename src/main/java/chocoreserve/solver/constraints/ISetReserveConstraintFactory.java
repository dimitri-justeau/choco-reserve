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

import chocoreserve.solver.SetReserveModel;
import chocoreserve.solver.constraints.features.set.SetCoveredFeatures;
import chocoreserve.solver.constraints.features.set.SetMinProbability;
import chocoreserve.solver.constraints.spatial.set.NbConnectedComponents;
import chocoreserve.solver.constraints.spatial.set.SetBufferZone;
import chocoreserve.solver.constraints.spatial.set.SetRadius;
import chocoreserve.solver.feature.BinaryFeature;
import chocoreserve.solver.feature.ProbabilisticFeature;
import org.chocosolver.solver.variables.SetVar;

/**
 * Interface for constraints over the Set encoding of the Nature Reserve Problem.
 */
public interface ISetReserveConstraintFactory {

    SetReserveModel self();

    // ---- //
    // Misc //
    // ---- //

    default IReserveConstraint mandatorySites(SetVar set, int... sites) {
        return () -> {
            for (int i : sites) {
                self().getChocoModel().member(i, set).post();
            }
        };
    }

    // ---------------------------------- //
    // Feature representation constraints //
    // ---------------------------------- //

    /**
     * Creates a coveredFeatures constraint. The coveredFeatures constraint holds iff each feature involved in
     * the constraint is present in at least one planning unit of the reserve system.
     *
     * @param features An array of features.
     * @return A CoveredFeatures constraint.
     */
    default IReserveConstraint coveredFeatures(SetVar set, BinaryFeature... features) {
        return new SetCoveredFeatures(self(), set, features);
    }
//
//    /**
//     * Creates a redundantFeatures constraint. The redundantFeatures constraint holds iff each feature involved in
//     * the constraint is present in at least k distinct planning units of the reserve system.
//     *
//     * @param k A int representing the minimum number of distinct planning units on which each feature must be present.
//     * @param features An array of features.
//     * @return A RedundantFeatures constraint.
//     */
//    default IReserveConstraint redundantFeatures(int k, BinaryFeature... features) {
//        return new RedundantFeatures(self(), k, features);
//    }
//
    /**
     * Creates a minProbability constraint. The minProbability constraint holds iff each feature involved in the
     * constraint is covered with a minimum probability of alpha in the reserve system.
     *
     * @param alpha A double represent the minimum probability of presence for each feature in the reserve system.
     * @param features An array of features.
     * @return A MinProbability constraint.
     */
    default IReserveConstraint minProbability(SetVar set, double alpha, ProbabilisticFeature... features) {
        return new SetMinProbability(self(), set, alpha, features);
    }
//
//    default IReserveConstraint minProbability(double alpha, boolean postCovered, ProbabilisticFeature... features) {
//        return new MinProbability(self(), postCovered, alpha, features);
//    }
//
//
//    // ------------------- //
//    // Spatial constraints //
//    // ------------------- //
//
    /**
     * Creates a nbReserves constraint. The nbReserves constraint holds iff the reserve system has a number of
     * connected components (reserves) between nbMin and nbMax.
     *
     * @param nbMin An int representing the minimum number of reserves.
     * @param nbMax An int representing the maximum number of reserves.
     * @return A NbReserves constraint.
     */
    default IReserveConstraint nbConnectedComponents(SetVar set, int nbMin, int nbMax) {
        return new NbConnectedComponents(self(), set, nbMin, nbMax);
    }
//
//    /**
//     * Creates an areaReserves constraint. The areaReserves constraints holds iff each reserve of the reserve system
//     * has an area (in number of planning units) between minNCC and maxNCC.
//     *
//     * @param minNCC An IntVar representing the size of the smallest reserve of the reserve system.
//     * @param maxNCC An IntVar representing the size of the largest reserve of the system.
//     * @return An areaReserves constraint.
//     */
//    default IReserveConstraint areaReserves(IntVar minNCC, IntVar maxNCC) {
//        return new AreaReserves(self(), minNCC, maxNCC);
//    }
//
//    /**
//     * Creates an areaReserveSystem constraint. The areaReserveSystem constraint holds iff the total area of the
//     * reserve system (in number of planning units) is between areaMin and areaMax.
//     *
//     * @param areaMin An int representing the minimum total area of the reserve system.
//     * @param areaMax An int representing the maximum total area of the reserve system.
//     * @return An areaReserveSystem constraint.
//     */
//    default IReserveConstraint areaReserveSystem(int areaMin, int areaMax){
//        return new AreaReserveSystem(self(), areaMin, areaMax);
//    }

    /**
     * Creates a maxDiameter constraint. The maxDiameter constraint holds iff the maximum distance between the centers
     * of the sites is <= maxDiameter.
     *
     * @param maxDiameter The maximum diameter.
     * @return A maxDiameter constraint.
     */
    default IReserveConstraint maxDiameter(double maxDiameter, SetVar set) {
        return new SetRadius(self(), set, self().getChocoModel().realVar("radius", 0, 0.5 * maxDiameter, 1e-5));
    }

    default IReserveConstraint bufferZone() {
        return new SetBufferZone(self());
    }
}
