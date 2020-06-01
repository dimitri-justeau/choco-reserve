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

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.RealVar;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TestPropSmallestEnclosingCircle {

    @Test
    public void testGetKernel() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            ContradictionException {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(25);
        double[][] coordinates = new double[25][];
        for (int i = 0; i < 25; i++) {
            coordinates[i] = new double[]{Math.floorDiv(i, 5), i % 5};
        }
        RealVar radius = model.realVar(0, Math.sqrt(50));
        RealVar x = model.realVar(0, 5);
        RealVar y = model.realVar(0, 5);
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Method method = PropSmallestEnclosingCircle.class.getDeclaredMethod("getKernelPoints");
        method.setAccessible(true);
        int[] kernel = (int[]) method.invoke(smallestCircle);
        Assert.assertEquals(0, kernel.length);
        points[0].setToTrue(smallestCircle);
        kernel = (int[]) method.invoke(smallestCircle);
        Assert.assertTrue(Arrays.equals(new int[]{0}, kernel));
    }

    @Test
    public void testGetEnvelope() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(25);
        double[][] coordinates = new double[25][];
        for (int i = 0; i < 25; i++) {
            coordinates[i] = new double[]{Math.floorDiv(i, 5), i % 5};
        }
        RealVar radius = model.realVar(0, Math.sqrt(50));
        RealVar x = model.realVar(0, 5);
        RealVar y = model.realVar(0, 5);
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Method method = PropSmallestEnclosingCircle.class.getDeclaredMethod("getEnvelopePoints");
        method.setAccessible(true);
        int[] env = (int[]) method.invoke(smallestCircle);
        Assert.assertEquals(5 * 5, env.length);
    }

    @Test
    public void testMinidisk() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(25);
        double[][] coordinates = new double[25][];
        for (int i = 0; i < 25; i++) {
            coordinates[i] = new double[]{i % 5, Math.floorDiv(i, 5)};
        }
        RealVar radius = model.realVar(0, Math.sqrt(50));
        RealVar x = model.realVar(0, 5);
        RealVar y = model.realVar(0, 5);
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Method method = PropSmallestEnclosingCircle.class.getDeclaredMethod("minidisk", int[].class);
        method.setAccessible(true);
        double eps = PropSmallestEnclosingCircle.EPSILON;
        // Case 1: 0 points.
        double[] minidisk = (double[]) method.invoke(smallestCircle, new int[]{});
        Assert.assertEquals(0, minidisk.length);
        // Case 2: 1 point.
        minidisk = (double[]) method.invoke(smallestCircle, new int[]{0});
        Assert.assertTrue(Arrays.equals(new double[]{0, 0, eps}, minidisk));
        // Case 3: 2 points.
        minidisk = (double[]) method.invoke(smallestCircle, new int[]{0, 2});
        Assert.assertTrue(Arrays.equals(new double[]{1, 0, 1 + eps}, minidisk));
        // Case 4: 3 points.
        minidisk = (double[]) method.invoke(smallestCircle, new int[]{0, 2, 6});
        Assert.assertTrue(Arrays.equals(new double[]{1, 0, 1 + eps}, minidisk));
        // Case 5: 4 points
        minidisk = (double[]) method.invoke(smallestCircle, new int[]{5, 9, 17});
        Assert.assertTrue(Arrays.equals(new double[]{2, 1, 2 + eps}, minidisk));
        // Case 6: many points, many times !
        int[][] pointSets = new int[][]{
                {1, 2, 3, 8, 5, 9, 17, 20, 24},
                {4, 6, 12, 13, 11, 19, 23, 7, 8},
                {3, 6, 11, 14, 10, 17, 21, 19, 0},
                {5, 7, 13, 14, 12, 20, 24, 8, 9},
        };
        for (int[] pts : pointSets) {
            minidisk = (double[]) method.invoke(smallestCircle, pts);

            // Invoke the following code for displaying the results
//            MinidiskCanvas canvas = new MinidiskCanvas(pts, coordinates, minidisk);
//            JFrame frame = new JFrame();
//            frame.setSize(600, 600);
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.getContentPane().add(canvas);
//            frame.setVisible(true);

            double[] c = new double[]{minidisk[0], minidisk[1]};
            double r = minidisk[2];
            int nbBorder = 0;
            for (int i : pts) {
                double[] p = coordinates[i];
                double dist = Math.sqrt(Math.pow(c[0] - p[0], 2) + Math.pow(c[1] - p[1], 2));
                Assert.assertTrue(dist <= r);
                if ((dist <= r + eps) && (r - eps <= dist)) {
                    nbBorder++;
                }
            }
        }

    }

    @Test
    public void testSuccess1() {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(25);
        double[][] coordinates = new double[25][];
        for (int i = 0; i < 25; i++) {
            coordinates[i] = new double[]{i % 5, Math.floorDiv(i, 5)};
        }
        RealVar radius = model.realVar(0, Math.sqrt(50) / 2, 1e-5);
        RealVar x = model.realVar(-5, 5, 1e-5);
        RealVar y = model.realVar(-5, 5, 1e-5);
        double eps = PropSmallestEnclosingCircle.EPSILON;
        Set<Integer> setTrue = new HashSet<>();
        setTrue.add(0);
        setTrue.add(1);
        setTrue.add(2);
        for (int i = 0; i < points.length; i++) {
            if (setTrue.contains(i)) {
                model.arithm(points[i], "=", 1).post();
            } else {
                model.arithm(points[i], "=", 0).post();
            }
        }
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Constraint c = new Constraint("smallestCircle", smallestCircle);
        model.post(c);
        Solver solver = model.getSolver();
        if (solver.solve()) {
            Assert.assertTrue(x.getLB() >= (1 - x.getPrecision()) && x.getUB() <= (1 + x.getPrecision()));
            Assert.assertTrue(y.getLB() >= (0 - y.getPrecision()) && y.getUB() <= (0 + y.getPrecision()));
            Assert.assertTrue(radius.getLB() >= (1 - radius.getPrecision() - eps)
                    && radius.getUB() <= (1 + radius.getPrecision() + eps));
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testEnumerate1() {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(5);
        double[][] coordinates = new double[][]{
                {3.3, 2.5},
                {8.0, 9.5},
                {2.0, 2.0},
                {1.9, 0.5},
                {7.2, 5.6}
        };
        RealVar radius = model.realVar(0, Math.sqrt(200) / 2, 1e-5);
        RealVar x = model.realVar(-10, 10, 1e-5);
        RealVar y = model.realVar(-10, 10, 1e-5);
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Constraint c = new Constraint("smallestCircle", smallestCircle);
        model.post(c);
        Solver solver = model.getSolver();
        int nbSol = 0;
        while (solver.solve()) {
            nbSol++;
        }
        Assert.assertEquals((int) (Math.pow(2, 5)) - 1, nbSol);
    }

    @Test
    public void testEnumerate2() {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(9);
        double[][] coordinates = new double[][]{
                {0.0, 0.0},
                {1.0, 0.0},
                {-1.0, 0.0},
                {0.0, 1.0},
                {0.0, -1.0},
                {0.0, -5.5},
                {0.0, 5.5},
                {5.5, 0.0},
                {-5.5, 0.0},
        };
        RealVar radius = model.realVar(0, 2.0, 1e-5);
        RealVar x = model.realVar(-1, 1, 1e-5);
        RealVar y = model.realVar(-1, 1, 1e-5);
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Constraint c = new Constraint("smallestCircle", smallestCircle);
        model.post(c);
        Solver solver = model.getSolver();
        int nbSol = 0;
        while (solver.solve()) {
//            int[] pts = IntStream.range(0, points.length).filter(i -> points[i].isInstantiatedTo(1)).toArray();
//            double[] minidisk = new double[] {x.getUB(), y.getUB(), radius.getUB()};
//            MinidiskCanvas canvas = new MinidiskCanvas(pts, coordinates, minidisk, - 3, 3, 1, -3, 3, 1, 100);
//            JFrame frame = new JFrame();
//            frame.setSize(600, 600);
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.getContentPane().add(canvas);
//            frame.setVisible(true);
            nbSol++;
        }
        Assert.assertEquals((int) (Math.pow(2, 5)) - 1, nbSol);
    }

    @Test
    public void testFail() {
        Model model = new Model();
        BoolVar[] points = model.boolVarArray(5);
        double[][] coordinates = new double[][]{
                {0.0, 0.0},
                {1.0, 0.0},
                {-1.0, 0.0},
                {0.0, 1.0},
                {0.0, -1.0},
        };
        RealVar radius = model.realVar(3.0, 8.0, 1e-5);
        RealVar x = model.realVar(-1, 1, 1e-5);
        RealVar y = model.realVar(-1, 1, 1e-5);
        PropSmallestEnclosingCircle smallestCircle = new PropSmallestEnclosingCircle(points, coordinates, radius, x, y);
        Constraint c = new Constraint("smallestCircle", smallestCircle);
        model.post(c);
        Solver solver = model.getSolver();
        Assert.assertFalse(solver.solve());
    }

    /**
     * Utility class for displaying minidisks
     */
    public class MinidiskCanvas extends Canvas {

        int[] points;
        double[][] coordinates;
        double[] minidisk;
        int xmin, xmax, xshift, ymin, ymax, yshift, scale;

        public MinidiskCanvas(int[] points, double[][] coordinates, double[] minidisk,
                              int xmin, int xmax, int xshift, int ymin, int ymax, int yshift, int scale) {
            this.points = points;
            this.coordinates = coordinates;
            this.minidisk = minidisk;
            this.xmin = xmin;
            this.xmax = xmax;
            this.xshift = xshift * scale;
            this.ymin = ymin;
            this.ymax = ymax;
            this.yshift = yshift * scale;
            this.scale = scale;
        }

        public void paint(Graphics g) {
            g.drawLine(xshift, yshift, xshift, scale * ymax + yshift);
            g.drawLine(xshift, yshift, scale * xmax + xshift, yshift);
            for (int i : points) {
                double[] p = coordinates[i];
                g.fillOval((int) (scale * p[0] - 5 + xshift), (int) (scale * p[1] - 5 + yshift), 10, 10);
            }
            g.setColor(Color.red);
            int cx = (int) (scale * minidisk[0] + xshift);
            int cy = (int) (scale * minidisk[1] + yshift);
            int r = (int) (scale * minidisk[2]);
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
    }
}
