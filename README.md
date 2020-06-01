# Choco-reserve #

[![Build status](https://travis-ci.org/dimitri-justeau/choco-reserve.svg?branch=master)](https://travis-ci.org/dimitri-justeau/choco-reserve)
[![codecov](https://codecov.io/gh/dimitri-justeau/choco-reserve/branch/master/graph/badge.svg)](https://codecov.io/gh/dimitri-justeau/choco-reserve)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d3bce55285914470ab71a174ea81d258)](https://www.codacy.com/app/dimitri-justeau/choco-reserve?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dimitri-justeau/choco-reserve&amp;utm_campaign=Badge_Grade)

Choco-reserve is a systematic conservation planning (SCP) framework based on constraint programming (CP). It is built upon a generic constrained partitioning model.

It is based on the [Choco](https://github.com/chocoteam/choco-solver) CP solver, and [Choco-graph](https://github.com/chocoteam/choco-graph), its extension for handling graph variables.

Choco-reserve implements the models described in [Justeau-Allaire, Birnbaum and Lorca 2018](https://link.springer.com/chapter/10.1007/978-3-319-98334-9_33) (available for free download [here](https://www.researchgate.net/publication/327167708_Unifying_Reserve_Design_Strategies_with_Graph_Theory_and_Constraint_Programming_24th_International_Conference_CP_2018_Lille_France_August_27-31_2018_Proceedings)) and in [Justeau-Allaire, Vismara, Birnbaum and Lorca 2018](https://www.ijcai.org/Proceedings/2019/818).

## Build from sources ##

Clone the github repository and install using Maven:

```shell
mvn install
```

## Quick start ##

First, create a grid representing the discretized geographical space you are working on. For example a 20x20 regular square grid:

```java
Grid grid = new RegularSquareGrid(20, 20);
```

Then declare the regions to be delineated in this grid, and the neighborhood definition within these regions. For example a protected core area, a protected buffer zone and the out-reserve area, with the four-connected neighborhood definition:

```java
Region core = new Region("core", Neighborhoods.FOUR_CONNECTED);
Region buffer = new Region("buffer", Neighborhoods.FOUR_CONNECTED);
Region out = new Region("out", Neighborhoods.FOUR_CONNECTED);
```

Create the model:

```java
ReserveModel reserveModel = new ReserveModel(grid, out, buffer, core);
```

Load some features (e.g. species occurrences from raster files):

```java
BinaryFeature speciesA = reserveModel.binaryFeature("Species_A", "/path/to/species_A/raster.tiff");
BinaryFeature speciesB = reserveModel.binaryFeature("Species_B", "/path/to/species_B/raster.tiff");
```

Post some constraint to the model:

```java
// The protected core must contain an occurrence of speciesA and speciesB
reserveModel.coveredFeatures(core, speciesA, speciesB).post();
// The protected core must be connected
reserveModel.nbConnectedComponents(1, 1).post();
// The protected buffer must be a buffer zone between the core area and the out-reserve area
reserveModel.bufferZone(core, out, buffer).post();
// The protected buffer zone must be compact (within a smallest enclosing circle with a 4-sites maximum diameter)
reserveModel.maxDiameter(buffer, 4).post();  
```

Define an optimization objective if necessary, e.g. the number of edges:

```java
// Define the constraint
NbEdges nbEdgesConstraint = new NbEdges(reserveModel, potentialForest);
// Get the nbEdges variable - It will be defined as an optimization objective to the solver
IntVar nbEdges = nbEdgesConstraint.nbEdges;
// Post the constraint
nbEdgesConstraint.post();
```

Get the Choco solver and solve:

```java
Solver solver = reserveModel.getChocoSolver();
// Tell the solver to find the solution satisfying the constraints AND maximizing the number of edges
solver.findOptimalSolution(nbEdges, true);
```
