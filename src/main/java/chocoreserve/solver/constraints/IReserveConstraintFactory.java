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

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.constraints.features.AllCovered;
import chocoreserve.solver.constraints.features.CoveredFeatures;
import chocoreserve.solver.constraints.features.MinProbability;
import chocoreserve.solver.constraints.features.RedundantFeatures;
import chocoreserve.solver.constraints.spatial.*;
import chocoreserve.solver.constraints.spatial.fragmentation.AggregationIndex;
import chocoreserve.solver.constraints.spatial.fragmentation.EffectiveMeshSize;
import chocoreserve.solver.feature.BinaryFeature;
import chocoreserve.solver.feature.ProbabilisticFeature;
import chocoreserve.solver.region.AbstractRegion;
import chocoreserve.solver.region.Region;
import org.chocosolver.solver.variables.IntVar;


/**
 * Interface for constraints over the Set encoding of the Nature Reserve Problem.
 */
public interface IReserveConstraintFactory {

    ReserveModel self();

    // ---- //
    // Misc //
    // ---- //

    default IReserveConstraint mandatorySites(AbstractRegion region, int... sites) {
        return () -> {
            for (int i : sites) {
                self().getChocoModel().member(i, region.getSetVar()).post();
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
     * @param region   The region where the constraint must be posted.
     * @param features An array of features.
     * @return A CoveredFeatures constraint.
     */
    default IReserveConstraint coveredFeatures(AbstractRegion region, BinaryFeature... features) {
        return new CoveredFeatures(self(), region, features);
    }

    default IReserveConstraint allCovered(AbstractRegion region, BinaryFeature... features) {
        return new AllCovered(self(), region, features);
    }

    /**
     * Creates a redundantFeatures constraint. The redundantFeatures constraint holds iff each feature involved in
     * the constraint is present in at least k distinct planning units of the reserve system.
     *
     * @param region   The region where the constraint must be posted.
     * @param k        A int representing the minimum number of distinct planning units on which each feature must be present.
     * @param features An array of features.
     * @return A RedundantFeatures constraint.
     */
    default IReserveConstraint redundantFeatures(AbstractRegion region, int k, BinaryFeature... features) {
        return new RedundantFeatures(self(), region, k, features);
    }

    /**
     * Creates a minProbability constraint. The minProbability constraint holds iff each feature involved in the
     * constraint is covered with a minimum probability of alpha in the reserve system.
     *
     * @param region   The region where the constraint must be posted.
     * @param alpha    A double represent the minimum probability of presence for each feature in the reserve system.
     * @param features An array of features.
     * @return A MinProbability constraint.
     */
    default IReserveConstraint minProbability(AbstractRegion region, double alpha, ProbabilisticFeature... features) {
        return new MinProbability(self(), region, alpha, features);
    }

//    /**
//     * Creates a minQuantity constraint. The minQuantity constraint holds iff each feature involved in
//     * the constraint has a cumulated quantity of at least k in the region.
//     *
//     * @param region The region where the constraint must be posted.
//     * @param k min quantity.
//     * @param features An array of features.
//     * @return A MinQuantity constraint.
//     */
//    default IReserveConstraint minQuantity(AbstractRegion region, int k, BinaryFeature... features) {
//        return new RedundantFeatures(self(), region, k, features);
//    }

    // ------------------- //
    // Spatial constraints //
    // ------------------- //

    /**
     * Creates a nbReserves constraint. The nbReserves constraint holds iff the reserve system has a number of
     * connected components (reserves) between nbMin and nbMax.
     *
     * @param region The region where the constraint must be posted.
     * @param nbMin  An int representing the minimum number of reserves.
     * @param nbMax  An int representing the maximum number of reserves.
     * @return A NbReserves constraint.
     */
    default IReserveConstraint nbConnectedComponents(AbstractRegion region, int nbMin, int nbMax) {
        return new NbConnectedComponents(self(), region, nbMin, nbMax);
    }

    /**
     * Creates an areaReserves constraint. The areaReserves constraints holds iff each reserve of the reserve system
     * has an area (in number of planning units) between minNCC and maxNCC.
     *
     * @param region The region where the constraint must be posted.
     * @param minNCC An IntVar representing the size of the smallest reserve of the reserve system.
     * @param maxNCC An IntVar representing the size of the largest reserve of the system.
     * @return An areaReserves constraint.
     */
    default IReserveConstraint sizeConnectedComponents(Region region, IntVar minNCC, IntVar maxNCC) {
        return new SizeConnectedComponents(self(), region, minNCC, maxNCC);
    }

    /**
     * Creates an areaReserveSystem constraint. The areaReserveSystem constraint holds iff the total area of the
     * reserve system (in number of planning units) is between areaMin and areaMax.
     *
     * @param region  The region where the constraint must be posted.
     * @param areaMin An int representing the minimum total area of the reserve system.
     * @param areaMax An int representing the maximum total area of the reserve system.
     * @return An areaReserveSystem constraint.
     */
    default IReserveConstraint sizeRegion(AbstractRegion region, int areaMin, int areaMax) {
        return new SizeRegion(self(), region, areaMin, areaMax);
    }

    /**
     * Creates a maxDiameter constraint. The maxDiameter constraint holds iff the maximum distance between the centers
     * of the sites is <= maxDiameter.
     *
     * @param region      The region where the constraint must be posted.
     * @param maxDiameter The maximum diameter.
     * @return A maxDiameter constraint.
     */
    default IReserveConstraint maxDiameter(AbstractRegion region, double maxDiameter) {
        return new Radius(self(), region, self().getChocoModel().realVar("radius", 0, 0.5 * maxDiameter, 1e-5));
    }

    default IReserveConstraint maxDiameterSpatial(AbstractRegion region, double maxDiameter) {
        return new RadiusSpatialGraph(self(), region, self().getChocoModel().realVar("radius", 0, 0.5 * maxDiameter, 1e-5));
    }

    /**
     * Creates a bufferZone constraint. The bufferZone constraint holds iff the region 'buffer' is a buffer zone
     * between 'region1' and 'region2'.
     *
     * @param region1 The region to be buffered from region2.
     * @param region2 The region to be buffered from region1.
     * @param buffer  The buffer region between region1 and region2.
     * @return A bufferZone constraint.
     */
    default IReserveConstraint bufferZone(AbstractRegion region1, AbstractRegion region2, AbstractRegion buffer) {
        return new BufferZone(self(), region1, region2, buffer);
    }

    default IReserveConstraint bufferZone(INeighborhood neighborhood, AbstractRegion region1, AbstractRegion region2,
                                          AbstractRegion buffer) {
        return new BufferZone(self(), neighborhood, region1, region2, buffer);
    }

    default IReserveConstraint nbEdges(AbstractRegion region) {
        return new NbEdges(self(), region);
    }

    default IReserveConstraint perimeter(AbstractRegion region) {
        return new PerimeterSquareGridFourConnected(self(), region);
    }

    // ---------------- //
    // Variable Factory //
    // ---------------- //

    default IntVar nbEdgesVar(AbstractRegion region) {
        NbEdges nbEdgesConstraint = new NbEdges(self(), region);
        nbEdgesConstraint.post();
        return nbEdgesConstraint.nbEdges;
    }

    default IntVar aggregationIndex(AbstractRegion region, int precision) {
        AggregationIndex aggIndexConstraint = new AggregationIndex(self(), region, precision);
        aggIndexConstraint.post();
        return aggIndexConstraint.aggregationIndex;
    }

    default IntVar effectiveMeshSize(AbstractRegion region, int precision) {
        EffectiveMeshSize meshConstraint = new EffectiveMeshSize(self(), region, self().getGrid().getNbCells(), precision);
        meshConstraint.post();
        return meshConstraint.mesh;
    }

}
