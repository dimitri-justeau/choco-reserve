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

package chocoreserve.solver;

import chocoreserve.exception.RegionAlreadyLinkedToModelError;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.constraints.IReserveConstraintFactory;
import chocoreserve.solver.feature.Feature;
import chocoreserve.solver.feature.IFeatureFactory;
import chocoreserve.solver.region.ComposedRegion;
import chocoreserve.solver.region.Region;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;

import java.util.*;

/**
 * Base model for the Nature Reserve Problem. Defines the variables and constraints that are common to every
 * instance of the problem. Specialization for specific instances is provided by the extra constraints that can
 * be 'posted' to a model.
 */
public class ReserveModel<T extends RegularSquareGrid> implements IReserveModel<T>, IReserveConstraintFactory, IFeatureFactory {

    /** The grid on which applies the model */
    private T grid;

    /** The features referenced by the model */
    private Map<String, Feature> features;

    /** The choco model */
    private GraphModel model;

    /** Decision variables */
    private IntVar[] sites;

    /** Regions */
    private Region[] regions;
    private ComposedRegion[] composedRegions;

    public ReserveModel(T grid, Region... regions) {
        this(grid, regions, new ComposedRegion[]{});
    }

    public ReserveModel(T grid, Region[] regions, ComposedRegion[] composedRegions) {
        this.grid = grid;
        this.model = new GraphModel("Nature Reserve Problem");
        this.regions = regions;
        this.composedRegions = composedRegions;
        Arrays.stream(regions).forEach(r -> {
            try {
                r.setReserveModel(this);
            } catch (RegionAlreadyLinkedToModelError regionAlreadyLinkedToModelError) {
                regionAlreadyLinkedToModelError.printStackTrace();
            }
        });
        Arrays.stream(composedRegions).forEach(r -> {
            try {
                r.setReserveModel(this);
            } catch (RegionAlreadyLinkedToModelError regionAlreadyLinkedToModelError) {
                regionAlreadyLinkedToModelError.printStackTrace();
            }
        });
        this.features = new HashMap<>();
        // Init decision variables sites[i] \in [0, region.length - 1]
        this.sites = this.model.intVarArray(
                "sites",
                this.grid.getNbCells(),
                0, regions.length - 1
        );
        // Sets <-> Decision variables channeling
        SetVar[] setVars = Arrays.stream(regions).map(r -> r.getSetVar()).toArray(SetVar[]::new);
        this.model.setsIntsChanneling(setVars, this.sites).post();

        // Set default search
        this.model.getSolver().setSearch(Search.domOverWDegSearch(sites));
    }

    public T getGrid() {
        return grid;
    }

    public void addFeature(Feature feature) {
        this.features.put(feature.getName(), feature);
    }

    public Map<String, Feature> getFeatures() {
        return features;
    }

    @Override
    public Region[] getRegions() {
        return regions;
    }

    // --------------------- //
    // Choco related methods //
    // --------------------- //

    public GraphModel getChocoModel() {
        return model;
    }

    public IntVar[] getSites() {
        return sites;
    }

    // -------------------------- //
    // Solution retrieval methods //
    // -------------------------- //

    public void printSolution() {
        printSolution(true);
    }

    public void printSolution(boolean ignoreBorder) {
        printSolution(ignoreBorder, new String[] {" ", "-", "+", "#"});
    }

    public void printSolution(boolean ignoreBorder, String[] display) {
        if (!(grid instanceof RegularSquareGrid)) {
            return;
        }
        System.out.println("\nSolution:");
        System.out.println("   " + new String(new char[grid.getNbCols(ignoreBorder) + 2]).replace("\0", "_"));
        for (int i = 0; i < grid.getNbRows(ignoreBorder); i++) {
            if (!ignoreBorder && (i == grid.getBorder() || i == grid.getNbRows() - grid.getBorder())) {
                System.out.println("  |"
                        + new String(new char[grid.getBorder()]).replace("\0", " ")
                        + new String(new char[grid.getNbRows() - grid.getBorder()]).replace("\0", "-"));
            }
            System.out.printf("  |");
            for (int j = 0; j < grid.getNbCols(ignoreBorder); j++) {
                if (!ignoreBorder && (j == grid.getBorder() || j == grid.getNbRows() - grid.getBorder())) {
                    System.out.printf("|");
                }
                boolean found = false;
                for (int r = 0; r < regions.length; r++) {
                    if (regions[r].getSetVar().getLB().contains(grid.getIndexFromCoordinates(i, j))) {
                        found = true;
                        if (r < display.length) {
                            System.out.printf(display[r]);
                        } else {
                            System.out.println(r);
                        }
                        break;
                    }
                }
                if (!found) {
                    System.out.printf("?");
                }
            }
            System.out.printf("\n");
        }
        System.out.println("\n");
        for (int r = 0; r < regions.length; r++) {
            Region region = regions[r];
            System.out.println("Region '" + region.getName() + "':");
            if (region.nbCCInit()) {
                System.out.println("  - NbCC = " + region.getNbCC().getValue());
            }
            System.out.println("  - NbSites = " + region.getNbSites().getValue());
        }
        for (int r = 0; r < composedRegions.length; r++) {
            ComposedRegion composedRegion = composedRegions[r];
            System.out.println("ComposedRegion '" + composedRegion.getName() + "':");
            System.out.println("  - NbSites = " + composedRegion.getNbSites().getValue());
        }
        System.out.printf("\n");
    }

    public int getNbCols() {
        return grid.getNbCols();
    }

    public int getNbRows() {
        return grid.getNbRows();
    }

    // For constraint factory

    @Override
    public ReserveModel self() {
        return this;
    }
}
