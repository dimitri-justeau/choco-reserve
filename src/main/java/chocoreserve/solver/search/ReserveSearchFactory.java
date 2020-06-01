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

package chocoreserve.solver.search;

import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.Feature;
import chocoreserve.solver.search.selectors.variables.PoorestVarSelector;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;


/**
 * Factory class for nature reserve problem search strategies.
 */
public class ReserveSearchFactory {

    //-------------------//
    // Search strategies //
    //-------------------//

    /**
     * Discard poor search strategy (non-deterministic - planning units are shuffled before ranking):
     * - Ranks the planning units according to the diversity of features.
     * - Branches on the "poorest" planning units and instantiate them to 0.
     *
     * @param reserveModel         The reserve model.
     * @param probabilityThreshold A threshold for probabilistic features scoring.
     *                             The probability value is added iff it is > probabilityThreshold.
     * @param features             The features for computing the score.
     * @return The discard poor search strategy.
     */
    public static IntStrategy discardPoor(ReserveModel reserveModel, double probabilityThreshold,
                                          Feature... features) {
        return Search.intVarSearch(
                new PoorestVarSelector(reserveModel, probabilityThreshold, false, features),
                variable -> variable.getLB(),
                reserveModel.getSites()
        );
    }

    /**
     * Discard poor search strategy (non-deterministic - planning units are shuffled before ranking):
     * - Ranks the planning units according to the diversity of features.
     * - Branches on the "poorest" planning units and instantiate them to 0.
     *
     * @param reserveModel         The reserve model.
     * @param probabilityThreshold A threshold for probabilistic features scoring.
     *                             The probability value is added iff it is > probabilityThreshold.
     * @return The discard poor search strategy.
     */
    public static IntStrategy discardPoor(ReserveModel reserveModel, double probabilityThreshold) {
        Feature[] features = (Feature[]) reserveModel.getFeatures().values().toArray(new Feature[0]);
        return discardPoor(reserveModel, probabilityThreshold, features);
    }

    /**
     * Discard poor search strategy (deterministic - planning units are not shuffled before ranking):
     * - Ranks the planning units according to the diversity of features.
     * - Branches on the "poorest" planning units and instantiate them to 0.
     *
     * @param reserveModel         The reserve model.
     * @param probabilityThreshold A threshold for probabilistic features scoring.
     *                             The probability value is added iff it is > probabilityThreshold.
     * @param features             The features for computing the score.
     * @return The discard poor search strategy.
     */
    public static IntStrategy discardPoorDeterministic(ReserveModel reserveModel, double probabilityThreshold,
                                                       Feature... features) {
        return Search.intVarSearch(
                new PoorestVarSelector(reserveModel, probabilityThreshold, true, features),
                variable -> variable.getLB(),
                reserveModel.getSites()
        );
    }

    /**
     * Discard poor search strategy (deterministic - planning units are not shuffled before ranking):
     * - Ranks the planning units according to the diversity of features.
     * - Branches on the "poorest" planning units and instantiate them to 0.
     *
     * @param reserveModel         The reserve model.
     * @param probabilityThreshold A threshold for probabilistic features scoring.
     *                             The probability value is added iff it is > probabilityThreshold.
     * @return The discard poor search strategy.
     */
    public static IntStrategy discardPoorDeterministic(ReserveModel reserveModel, double probabilityThreshold) {
        Feature[] features = (Feature[]) reserveModel.getFeatures().values().toArray(new Feature[0]);
        return discardPoorDeterministic(reserveModel, probabilityThreshold, features);

    }
}
