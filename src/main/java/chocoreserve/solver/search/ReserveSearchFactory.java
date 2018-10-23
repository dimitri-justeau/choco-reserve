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
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Factory class for nature reserve problem search strategies.
 */
public class ReserveSearchFactory {

    //-------------------//
    // Search strategies //
    //-------------------//

    private static IntStrategy discardPoor(ReserveModel reserveModel, int[] ranking) {
        return Search.intVarSearch(
                variables -> {
                    for (int i = ranking.length - 1; i >=0; i--) {
                        IntVar var = reserveModel.getSites()[ranking[i]];
                        if (!var.isInstantiated()) {
                            return var;
                        }
                    }
                    return null;
                },
                variable -> variable.getLB(),
                reserveModel.getSites()
        );
    }

    /**
     * Discard poor search strategy (non-deterministic - planning units are shuffled before ranking):
     *      - Ranks the planning units according to the diversity of features.
     *      - Branches on the "poorest" planning units and instantiate them to 0.
     * @param reserveModel The reserve model.
     * @param probabilityThreshold A threshold for probabilistic features scoring.
     *                             The probability value is added iff it is > probabilityThreshold.
     * @return The discard poor search strategy.
     */
    public static IntStrategy discardPoor(ReserveModel reserveModel, double probabilityThreshold) {
        int[] ranking = makeRanking(reserveModel, makeScores(reserveModel, probabilityThreshold));
        return discardPoor(reserveModel, ranking);
    }

    /**
     * Discard poor search strategy (deterministic - planning units are not shuffled before ranking):
     *      - Ranks the planning units according to the diversity of features.
     *      - Branches on the "poorest" planning units and instantiate them to 0.
     * @param reserveModel The reserve model.
     * @param probabilityThreshold A threshold for probabilistic features scoring.
     *                             The probability value is added iff it is > probabilityThreshold.
     * @return The discard poor search strategy.
     */
    public static IntStrategy discardPoorDeterministic(ReserveModel reserveModel, double probabilityThreshold) {
        int[] ranking = makeRankingDeterministic(reserveModel, makeScores(reserveModel, probabilityThreshold));
        return discardPoor(reserveModel, ranking);
    }

    //-----------//
    // Utilities //
    //-----------//

    /**
     * Builds a diversity score for each planning unit of the grid associated to a model.
     * For each binary or quantitative feature 1 is added to the score. For probabilistic features,
     * the probability value is added iff it is > probabilityThreshold.
     * @param reserveModel The reserve model.
     * @param probabilityThreshold A threshold to use in case of probabilistic features.
     * @return The scores.
     */
    private static double[] makeScores(ReserveModel reserveModel, double probabilityThreshold) {
        assert probabilityThreshold >=0 && probabilityThreshold <= 1;
        int nbPlanningUnits = reserveModel.getGrid().getNbCells();
        // Compute scores
        double[] scores = new double[nbPlanningUnits];
        for (Feature f : reserveModel.getFeatures().values()) {
            try {
                double[] data = f.getData();
                for (int i = 0; i < nbPlanningUnits; i++) {
                    double v = data[i] >= probabilityThreshold ? data[i] : 0;
                    scores[i] += v;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return scores;
    }

    /**
     * Ranks the planning units according to a score.
     * This is the non-deterministic version (i.e. planning units are shuffled before ranking).
     * @param reserveModel The reserve model.
     * @param scores The scores.
     * @return The ranking.
     */
    private static int[] makeRanking(ReserveModel reserveModel, double[] scores) {
        int nbPlanningUnits = reserveModel.getGrid().getNbCells();
        List<Integer> planningUnits = IntStream.range(0, nbPlanningUnits)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(planningUnits);
        planningUnits.sort(Comparator.comparingInt(i -> -1 * (int) scores[i]));
        int[] diversityRanking = planningUnits.stream().mapToInt(v -> v).toArray();
        return diversityRanking;
    }

    /**
     * Ranks the planning units according to a score.
     * This is the deterministic version (i.e. planning units are not shuffled before ranking).
     * @param reserveModel The reserve model.
     * @param scores The scores.
     * @return The ranking.
     */
    private static int[] makeRankingDeterministic(ReserveModel reserveModel, double[] scores) {
        int nbPlanningUnits = reserveModel.getGrid().getNbCells();
        List<Integer> planningUnits = IntStream.range(0, nbPlanningUnits)
                .boxed()
                .collect(Collectors.toList());
        planningUnits.sort(Comparator.comparingInt(i -> -1 * (int) scores[i]));
        int[] diversityRanking = planningUnits.stream().mapToInt(v -> v).toArray();
        return diversityRanking;
    }
}
