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

package chocoreserve.solver.region;

import chocoreserve.exception.RegionAlreadyLinkedToModelError;
import chocoreserve.exception.RegionNotLinkedToModelError;

import chocoreserve.grid.neighborhood.INeighborhood;
import chocoreserve.solver.ReserveModel;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;


/**
 * Class representing a region.
 */
public abstract class AbstractRegion {

    protected String name;
    protected ReserveModel reserveModel;
    protected SetVar setVar;
    protected IntVar nbSites;

    public AbstractRegion(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setReserveModel(ReserveModel reserveModel) throws RegionAlreadyLinkedToModelError {
        if (this.reserveModel != null) {
            throw new RegionAlreadyLinkedToModelError();
        }
        this.reserveModel = reserveModel;
        this.buildSetVar();
        this.nbSites = setVar.getCard();
    }

    public SetVar getSetVar() {
        if (setVar == null) {
            new RegionNotLinkedToModelError().printStackTrace();
        }
        return setVar;
    }

    protected abstract void buildSetVar();

    public IntVar getNbSites() {
        if (nbSites == null) {
            new RegionNotLinkedToModelError().printStackTrace();
        }
        return nbSites;
    }

    public ReserveModel getReserveModel() {
        return reserveModel;
    }

    public abstract  INeighborhood getNeighborhood();
}
