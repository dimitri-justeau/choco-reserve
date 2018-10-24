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

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 */
public class PropSmallestEnclosingCircle extends Propagator<Variable> {

    public final static double EPSILON = 1e-5;

    private BoolVar[] pointBools;
    private double[][] coordinates;
    private RealVar radius, centerX, centerY;

    public PropSmallestEnclosingCircle(BoolVar[] pointBools, double[][] pointCoordinates, RealVar radius,
                                       RealVar centerX, RealVar centerY) {
        super(
                Stream.concat(Stream.of(new Variable[] {radius, centerX, centerY}),
                              Stream.of(pointBools))
                        .toArray(Variable[]::new),
                PropagatorPriority.LINEAR,
                false
        );
        this.pointBools = pointBools;
        this.coordinates = pointCoordinates;
        this.radius = radius;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    private int[] getKernelPoints() {
        return IntStream.range(0, pointBools.length)
                .filter(i -> (pointBools[i].isInstantiatedTo(1)))
                .toArray();
    }

    private int[] getEnvelopePoints() {
        return IntStream.range(0, pointBools.length)
                .filter(i -> pointBools[i].getUB() == 1)
                .toArray();
    }

    private int[] getEnvelopeMinusKernelPoints() {
        return IntStream.range(0, pointBools.length)
                .filter(i -> !pointBools[i].isInstantiated() && pointBools[i].getUB() == 1)
                .toArray();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int[] ker = getKernelPoints();
        int[] env = getEnvelopePoints();
        if (env.length == 0) {
            fails();
        }
        if (ker.length == env.length ) {
            double[] minidisk = minidisk(ker);
            double x = minidisk[0];
            double y = minidisk[1];
            double r = minidisk[2];
            if (x < centerX.getLB() || x > centerX.getUB() || y < centerY.getLB()
                    || y > centerY.getUB() || r < radius.getLB() || r > radius.getUB()) {
                fails();
            }
            radius.updateBounds(minidisk[2], minidisk[2], this);
            centerX.updateBounds(minidisk[0], minidisk[0], this);
            centerY.updateBounds(minidisk[1], minidisk[1], this);
            return;
        }
        if (ker.length > 0){
            double[] minidisk = minidisk(ker);
            double[] cker = new double[] {minidisk[0], minidisk[1]};
            double rker = minidisk[2];
            if (rker > (radius.getUB() + radius.getPrecision())) {
                fails();
            }
            for (int i : getEnvelopeMinusKernelPoints()) {
                if (distance(cker, coordinates[i]) > rker) {
                    double[] b_disk = minidisk(IntStream.concat(IntStream.of(ker), IntStream.of(i)).toArray());
                    if (b_disk[2] > (radius.getUB() + radius.getPrecision())) {
                        pointBools[i].setToFalse(this);
                    }
                }
            }
        }
    }

    @Override
    public ESat isEntailed() {

        int[] ker = getKernelPoints();
        int[] env = getEnvelopePoints();
        if (env.length == 0){
            // We assume that by definition, the empty set of points does not satisfy the constraint.
            return ESat.FALSE;
        }
        if (ker.length == env.length) {
            double[] minidisk = minidisk(ker);
            double x = minidisk[0];
            double y = minidisk[1];
            double r = minidisk[2];
            if (x >= centerX.getLB() && x <= centerX.getUB() && y >= centerY.getLB()
                    && y <= centerY.getUB() && r >= radius.getLB() && r <= radius.getUB()) {
                return ESat.TRUE;
            }
        }
        double[] minidisk_LB = minidisk(ker);
        double[] minidisk_UB = minidisk(env);
        if (minidisk_UB.length > 0) {
            double x_ub = minidisk_UB[0];
            double y_ub = minidisk_UB[1];
            double r_ub = minidisk_UB[2];
            if (r_ub < (radius.getLB() - radius.getPrecision())
                    || x_ub < (centerX.getLB() - centerX.getPrecision())
                    || y_ub < (centerY.getLB() - centerY.getPrecision())) {
                return ESat.FALSE;
            }
            if (minidisk_LB.length > 0) {
                double x_lb = minidisk_LB[0];
                double y_lb = minidisk_LB[1];
                double r_lb = minidisk_LB[2];
                if (r_lb > (radius.getUB() + radius.getPrecision())
                        || x_lb > (centerX.getUB() + centerX.getPrecision())
                        || y_lb > (centerY.getUB() + centerY.getPrecision())) {
                    return ESat.FALSE;
                }

                return ESat.TRUE;
            }
        }

        return ESat.UNDEFINED;
    }

    // ------------------------------------------------------------------------- //
    // Welzl's O(n) minidisk algorithm                                           //
    // See: Emo Welzl, "Smallest enclosing disks (balls and ellispsoids)", 1991. //
    // ------------------------------------------------------------------------- //

    /**
     * @param points Indexes of the points.
     * @return The smallest enclosing circle as new double[] {cx, cy, radius}.
     */
    private double[] minidisk(int[] points) {
        int nbPoints = points.length;
        if (nbPoints == 0) {
            return new double[] {};
        }
        if (nbPoints == 1) {
            double[] center = coordinates[points[0]];
            return new double[] {center[0], center[1], EPSILON};
        }
        if (nbPoints == 2) {
            double[] p1 = coordinates[points[0]];
            double[] p2 = coordinates[points[1]];
            double[] center = midpoint(p1, p2);
            return new double[] {center[0], center[1], (distance(p1, p2) / 2) + EPSILON};
        }
        // Shuffle the points
        List<Integer> shuffled = IntStream.of(points).boxed().collect(Collectors.toList());
        Collections.shuffle(shuffled);
        // First circle (with the two first points)
        double[] p1 = coordinates[shuffled.get(0)];
        double[] p2 = coordinates[shuffled.get(1)];
        double[] center = midpoint(p1, p2);
        double r = (distance(p1, p2) / 2) + EPSILON;
        // Check other points
        for (int i = 2; i < shuffled.size(); i++) {
            double[] pi = coordinates[shuffled.get(i)];
            if (distance(center, pi) > r) {
                // Find circle with pi in the border
                double[] circle = b_minidisk_one(shuffled, i);
                center[0] = circle[0];
                center[1] = circle[1];
                r = circle[2];
            }
        }
        return new double[] {center[0], center[1], r};
    }

    private double[] b_minidisk_one(List<Integer> shuffled, int i) {
        double[] p1 = coordinates[shuffled.get(0)];
        double[] pi = coordinates[shuffled.get(i)];
        double[] center = midpoint(p1, pi);
        double r = (distance(p1, pi) / 2) + EPSILON;
        // Check whether previous points are included in this new circle
        for (int j = 1; j < i; j++) {
            double[] pj = coordinates[shuffled.get(j)];
            if (distance(center, pj) > r) {
                double[] circle = b_minidisk_two(shuffled, i, j);
                center[0] = circle[0];
                center[1] = circle[1];
                r = circle[2];            }
        }
        return new double[] {center[0], center[1], r};
    }

    private double[] b_minidisk_two(List<Integer> shuffled, int i, int j) {
        double[] pi = coordinates[shuffled.get(i)];
        double[] pj = coordinates[shuffled.get(j)];
        double[] center = midpoint(pi, pj);
        double r = (distance(pi, pj) / 2) + EPSILON;
        // Check whether previous points are included in this new circle
        for (int k = 0; k < j; k++) {
            double[] pk = coordinates[shuffled.get(k)];
            if (distance(center, pk) > r) {
                double[] circle = circumcircle(pi, pj, pk);
                center[0] = circle[0];
                center[1] = circle[1];
                r = circle[2];
            }
        }
        return new double[] {center[0], center[1], r};
    }

    /**
     * @return The coordinates of the vector (p1, p2)
     */
    private static double[] vector(double[] p1, double[] p2) {
        return new double[] {p1[0] + p2[0], p1[1] + p2[1]};
    }

    /**
     * @return The midpoint of the segment (p1, p2)
     */
    private static double[] midpoint(double[] p1, double[] p2) {
        double[] v = vector(p1, p2);
        return new double[] {v[0] / 2, v[1] / 2};
    }

    /**
     * @return The distance between p1 and p2
     */
    private static double distance(double[] p1, double[] p2) {
        return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
    }

    /**
     * @return The circumcircle of the triangle (a, b, c).
     */
    private static double[] circumcircle(double[] a, double[] b, double[] c) {
        double d = 2 * (a[0] * (b[1] - c[1]) + b[0] * (c[1] - a[1]) + c[0] * (a[1] - b[1]));
        double cx = ((Math.pow(a[0], 2) + Math.pow(a[1], 2)) * (b[1] - c[1])
                +  (Math.pow(b[0], 2) + Math.pow(b[1], 2)) * (c[1] - a[1])
                + (Math.pow(c[0], 2) + Math.pow(c[1], 2)) * (a[1] - b[1])) / d;
        double cy = ((Math.pow(a[0], 2) + Math.pow(a[1], 2)) * (c[0] - b[0])
                +  (Math.pow(b[0], 2) + Math.pow(b[1], 2)) * (a[0] - c[0])
                + (Math.pow(c[0], 2) + Math.pow(c[1], 2)) * (b[0] - a[0])) / d;
        double cr = distance(new double[] {cx, cy}, a) + EPSILON;
        return new double[] {cx, cy, cr};
    }

}
