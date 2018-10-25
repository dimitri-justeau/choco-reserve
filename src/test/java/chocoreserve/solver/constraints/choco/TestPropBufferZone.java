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

package chocoreserve.solver.constraints.choco;

import chocoreserve.grid.regular.square.HeightConnectedSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test class for buffer zone propagator.
 */
public class TestPropBufferZone {

    @Test
    public void testIsEntailed() throws ContradictionException {
        Model model = new Model();
        BoolVar[][] core = model.boolVarMatrix(4, 4);
        BoolVar[][] buffer = model.boolVarMatrix(4, 4);
        RegularSquareGrid grid = new HeightConnectedSquareGrid(4, 4);
        PropBufferZone bufferZone = new PropBufferZone(core, buffer, grid);
        // Test 1 - True
        int[][] test = new int[][] {
                {2, 2, 2, 2},
                {2, 1, 1, 2},
                {2, 1, 2, 2},
                {2, 2, 2, 0}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, true);
        Assert.assertEquals(ESat.TRUE, bufferZone.isEntailed());
        core = model.boolVarMatrix(4, 4);
        buffer = model.boolVarMatrix(4, 4);
        bufferZone = new PropBufferZone(core, buffer, grid);
        // Test 2
        test = new int[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 2},
                {0, 0, 0, 2},
                {0, 0, 0, 2}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, true);
        Assert.assertEquals(ESat.FALSE, bufferZone.isEntailed());
        core = model.boolVarMatrix(4, 4);
        buffer = model.boolVarMatrix(4, 4);
        bufferZone = new PropBufferZone(core, buffer, grid);
        // Test 3
        test = new int[][] {
                {0, 2, 2, 2},
                {2, 2, 1, 2},
                {2, 1, 2, 2},
                {2, 2, 2, 0}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, true);
        Assert.assertEquals(ESat.TRUE, bufferZone.isEntailed());
        core = model.boolVarMatrix(4, 4);
        buffer = model.boolVarMatrix(4, 4);
        bufferZone = new PropBufferZone(core, buffer, grid);
        // Test 2
        test = new int[][] {
                {0, 0, 0, 0},
                {0, 1, 2, 0},
                {0, 2, 2, 0},
                {0, 0, 0, 0}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, true);
        Assert.assertEquals(ESat.FALSE, bufferZone.isEntailed());
        core = model.boolVarMatrix(4, 4);
        buffer = model.boolVarMatrix(4, 4);
        bufferZone = new PropBufferZone(core, buffer, grid);
        // Test 2
        test = new int[][] {
                {2, 2, 2, 2},
                {2, 1, 2, 1},
                {2, 1, 2, 1},
                {2, 1, 1, 1}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, true);
        Assert.assertEquals(ESat.FALSE, bufferZone.isEntailed());
        core = model.boolVarMatrix(4, 4);
        buffer = model.boolVarMatrix(4, 4);
        bufferZone = new PropBufferZone(core, buffer, grid);
    }

    @Test
    public void testForceBuffer() throws ContradictionException {
        Model model = new Model();
        BoolVar[][] core = model.boolVarMatrix(4, 4);
        BoolVar[][] buffer = model.boolVarMatrix(4, 4);
        RegularSquareGrid grid = new HeightConnectedSquareGrid(4, 4);
        PropBufferZone bufferZone = new PropBufferZone(core, buffer, grid);
        int[][] test = new int[][] {
                {0, 0, 0, 0},
                {0, 1, 1, 0},
                {0, 1, 0, 0},
                {0, 0, 0, 0}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, false);
        Constraint c = new Constraint("buffer", bufferZone);
        model.post(c);
        if (model.getSolver().solve()) {
            Assert.assertEquals(1, buffer[0][0].getValue());
            Assert.assertEquals(1, buffer[0][1].getValue());
            Assert.assertEquals(1, buffer[0][2].getValue());
            Assert.assertEquals(1, buffer[0][3].getValue());
            Assert.assertEquals(1, buffer[1][0].getValue());
            Assert.assertEquals(1, buffer[2][0].getValue());
            Assert.assertEquals(1, buffer[3][0].getValue());
            Assert.assertEquals(1, buffer[3][1].getValue());
            Assert.assertEquals(1, buffer[3][2].getValue());
            Assert.assertEquals(1, buffer[2][2].getValue());
            Assert.assertEquals(1, buffer[2][3].getValue());
            Assert.assertEquals(1, buffer[1][3].getValue());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testFail() throws ContradictionException {
        Model model = new Model();
        BoolVar[][] core = model.boolVarMatrix(4, 4);
        BoolVar[][] buffer = model.boolVarMatrix(4, 4);
        RegularSquareGrid grid = new HeightConnectedSquareGrid(4, 4);
        PropBufferZone bufferZone = new PropBufferZone(core, buffer, grid);
        int[][] test = new int[][] {
                {2, 2, 2, 0},
                {2, 0, 2, 0},
                {2, 2, 2, 0},
                {0, 0, 0, 0}
        };
        initMatrices(core, buffer, test, grid, bufferZone, true, true);
        Constraint c = new Constraint("buffer", bufferZone);
        model.post(c);
        Assert.assertFalse(model.getSolver().solve());
    }

    @Test
    public void manyTest() throws ContradictionException {
        Model model = new Model();
        BoolVar[][] core = model.boolVarMatrix("core", 4, 4);
        BoolVar[][] buffer = model.boolVarMatrix("buffer", 4, 4);
        RegularSquareGrid grid = new HeightConnectedSquareGrid(4, 4);
        PropBufferZone bufferZone = new PropBufferZone(core, buffer, grid);
        int[][] test = new int[][] {
                {0, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 1, 1, 0},
                {0, 0, 0, 0}
        };
        initMatrices(core, buffer, test, grid, bufferZone, false, false);
        Constraint c = new Constraint("buffer", bufferZone);
        model.post(c);
        model.getSolver().findAllSolutions();
    }

    private static void initMatrices(BoolVar[][] core, BoolVar[][] buffer, int[][] values, RegularSquareGrid grid,
                                     PropBufferZone bufferZone, boolean forceCoreZero,
                                     boolean forceBufferZero) throws ContradictionException {
        for (int i = 0; i < grid.getNbRows(); i++) {
            for (int j = 0; j < grid.getNbCols(); j++) {
                if (values[i][j] == 1) {
                    core[i][j].setToTrue(bufferZone);
                    if (forceBufferZero) { buffer[i][j].setToFalse(bufferZone); }

                } else {
                    if (values[i][j] == 2) {
                        if (forceCoreZero) { core[i][j].setToFalse(bufferZone); }
                        buffer[i][j].setToTrue(bufferZone);
                    } else {
                        if(forceCoreZero) { core[i][j].setToFalse(bufferZone); }
                        if(forceBufferZero) { buffer[i][j].setToFalse(bufferZone); }
                    }
                }
            }
        }
    }
}
