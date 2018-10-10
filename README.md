# Choco-reserve #

[![Build status](https://travis-ci.org/dimitri-justeau/choco-reserve.svg?branch=master)](https://travis-ci.org/dimitri-justeau/choco-reserve)
[![codecov](https://codecov.io/gh/dimitri-justeau/choco-reserve/branch/master/graph/badge.svg)](https://codecov.io/gh/dimitri-justeau/choco-reserve)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d3bce55285914470ab71a174ea81d258)](https://www.codacy.com/app/dimitri-justeau/choco-reserve?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dimitri-justeau/choco-reserve&amp;utm_campaign=Badge_Grade)

Choco-reserve is a solver for the *nature reserve problem* (also known as the *reserve design* and *reserve selection* problem).

It is based on the [Choco](https://github.com/chocoteam/choco-solver) Constraint Programming (CP) solver, and [Choco-graph](https://github.com/chocoteam/choco-graph), its extension for handling graph variables.

Choco-reserve implements the model described in [Justeau-Allaire, Birnbaum and Lorca 2018](https://link.springer.com/chapter/10.1007/978-3-319-98334-9_33) (available for free download [here](https://www.researchgate.net/publication/327167708_Unifying_Reserve_Design_Strategies_with_Graph_Theory_and_Constraint_Programming_24th_International_Conference_CP_2018_Lille_France_August_27-31_2018_Proceedings)).

## Quick start ##

First, create a grid representing the discretized geographical space you are working on:

```java
Grid grid = new FourConnectedSquareGrid(nbRows, nbCols);
```

Then instantiate a ReserveModel object:

```java
ReserveModel reserveModel = new ReserveModel(grid);
```

Create some features (e.g. from raster files):

```java
BinaryFeature speciesA = reserveModel.binaryFeature("Species_A", "/path/to/species_A/raster.tiff");
BinaryFeature speciesB = reserveModel.binaryFeature("Species_B", "/path/to/species_B/raster.tiff");
ProbabilisticFeature speciesC = reserveModel.binaryFeature("Species_C", "/path/to/species_C/raster.tiff");
```

Post some constraint to the model:

```java
reserveModel.coveredFeatures(speciesA, speciesB).post();
reserveModel.minProbability(0.9, speciesC).post();
reserveModel.nbReserves(1, 1).post();
```

Get the Choco solver and solve:

```java
Solver solver = reserveModel.getChocoSolver();
solver.solve();
```
