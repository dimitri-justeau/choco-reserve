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

package chocoreserve.solver.search.selectors.variables;

import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.feature.Feature;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public class PoorestVarSelector implements VariableSelector<IntVar> {

    ReserveModel reserveModel;
    Feature[] features;
    double threshold;
    boolean deterministic;
    double[] scores;
    int[] ranking;

    public PoorestVarSelector(ReserveModel reserveModel, double threshold, boolean deterministic,
                              Feature... features) {
        this.reserveModel = reserveModel;
        this.features = features;
        this.threshold = threshold;
        this.deterministic = deterministic;
        this.scores = makeScores();
        this.ranking = makeRanking();
    }

    @Override
    public IntVar getVariable(IntVar[] intVars) {
        for (int i = ranking.length - 1; i >=0; i--) {
            IntVar var = reserveModel.getSites()[ranking[i]];
            if (!var.isInstantiated()) {
                return var;
            }
        }
        return null;
    }

    /**
     * Builds a diversity score based on features for each planning unit of the grid associated to a model.
     */
    private double[] makeScores() {
        int nbSites = reserveModel.getGrid().getNbCells();
        // Compute scores
        double[] scores = new double[nbSites];
        for (Feature f : features) {
            try {
                double[] data = f.getData();
                for (int i = 0; i < nbSites; i++) {
                    double v = data[i] >= threshold ? data[i] : 0;
//                    double v = data[i] >= threshold ? 1 : 0;
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
     */
    private int[] makeRanking() {
        int nbSites = reserveModel.getGrid().getNbCells();
        List<Integer> planningUnits = IntStream.range(0, nbSites)
                .boxed()
                .collect(Collectors.toList());
        if (!deterministic) {
            Collections.shuffle(planningUnits);
        }
        planningUnits.sort(Comparator.comparingInt(i -> -1 * (int) scores[i]));
        int[] diversityRanking = planningUnits.stream().mapToInt(v -> v).toArray();
        return diversityRanking;
    }
}
