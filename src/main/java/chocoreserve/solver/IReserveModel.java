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

import chocoreserve.grid.IGrid;
import org.chocosolver.graphsolver.GraphModel;

/**
 * Interface for the base model of the Nature Reserve Problem. Defines the variables and constraints that are common
 * to every instance of the problem. Specialization for specific instances is provided by the extra constraints that
 * can be 'posted' to a model.
 */
public interface IReserveModel {

    /**
     * @return The grid associated with the model.
     */
    IGrid getGrid();

    /**
     * @return The Choco (graph) model on which relies the model.
     */
    GraphModel getChocoModel();
    
}
