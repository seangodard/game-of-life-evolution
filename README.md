# Game of Life: Evolution

### The Concept

***Game of Life: Evolution*** is a fun program that takes the wonders of
[John Conway's Game of Life](1) and spices things up by implementing a
[Genetic Algorithm](2) on top. The purpose of the program is to generate
starting boards for the Game of Life that produce the most new cells within the
simulation bound. Users may easily tweak the initial parameters of the algorithm
to see what mesmerizing, and oddly natural, creations result. This serves as a
great launching point to understanding and exploring the genetic algorithm in a
fun and interesting way. 

### Basic Use Guide

The program itself is quite easy to get started playing with. When the
program first starts you will be greeted by a single starting board
with its fitness shown across to top, which is defined in this case as follows:

> ***Fitness***: the number of new cells that this board produced within the
> 	simulation limit.

The board that is shown when first loaded is the optimal solution for a 5x5
board for the simulation limit of 100 generations. This was computed with a
simple brute force method just to use for comparison.

To run the simulation use the following controls:

- Single click to play the simulation over an infinite number of generations
- Single click again to stop the simulation and return to the starting point

### Generating New Boards

To generate new boards look to the controls in the left sidebar. Here you will
see several sliders used to tune the various parameters. By default, these
sliders are set to values that I found worked well for producing near optimal
solutions. Each of the parameters has the following meaning:

> ***Cell Start Radius***: The radius of the final board that will be produced
> 	as the final result of running the genetic algorithm. The center point is 
> 	excluded from this measurement. For example, a radius of 2 will produce a
> 	board which is 5x5. (I know that this is confusing and it will hopefully
> 	be changed soon.)

> ***Simulation Lifespan***: This sets the number of iterations of the Game of 
> 	life that the fitness will be measured over.

> ***Board Population Size***: This is the number boards within each generation
> 	of the genetic algorithm.

> ***Mutation Rate***: This is the mutation rate used by the genetic algorithm.

> ***Simulation Generations***: This is the number of iterations the genetic
> 	algorithm will run for.

> ***Number of Threads***: This sets the number of worker threads the genetic
> 	algorithm will use. This is limited by the number of CPUs Java detects on
> 	your system. Beyond this limit performance may start to deteriorate.

As you will notice, each slider has a limited range that it can be
set to. If you find that that the slider limits do not suit your needs, these
can easily be modified by changing the sliders *min* and *max* attributes
within the *Main.fxml* file. 

After the parameters are set, simply click the *Computer Genetic Best!*
button. Once the computations are complete, the new board will automatically be
set to the center so if you would like to save your current creation be sure to
save it from within the file menu!


### Sharing Boards

After generating several boards you may consider saving and some of them. This
is easily done through the file Save and Open options in the file menu. Boards
are saved in a custom **.ejc** file format which saves all the parameter
settings used to create the board as well as the time it took to compute it.
Currently these stored values are not viewable within the UI but this feature
is hopefully not far around the corner.

### Tech

- [JavaFX 8](http://docs.oracle.com/javase/8/javase-clienttechnologies.htm)

### Some TODOs

- Add code so the UI can scale
- Allow viewing all the parameters stored within *.ejc* files

[1]: https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life]
[2]: https://en.wikipedia.org/wiki/Genetic_algorithm
