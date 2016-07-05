// Author: Sean Godard
// Purpose: to hold the methods used to simulate the genetics algorithm and the rules of 
//			Conway's Game of Life
// References:
// 	-http://stackoverflow.com/questions/7206442/printing-all-possible-subsets-of-a-list -> Petar Minchev

import java.awt.Point;
import java.util.HashSet;
import java.util.Random;

public final class Simulation {
	// Variables
	private static Random rand = new Random();

	// @param cellStartRadius: the area that the cells must start within
	// @param maxSimulationIterations: the maximum number of loops to test the fitness within
	// @return: the optimal placement of cells within that radius and upper simulation runs bound
	public static CellSet bruteForceBest(int cellStartRadius, int maxSimulationIterations) {
		CellSet bestCellSet = null;
		int bestFitness = 0;
		
		// Create a list of all possible points
		Point tempPoint;
		int numPossiblePoints = (int) Math.pow(cellStartRadius*2+1, 2);
		Point[] allPoints = new Point[numPossiblePoints];
		int index = 0;
		for (int x = -cellStartRadius; x <= cellStartRadius; x++) {
			for (int y = -cellStartRadius; y<= cellStartRadius;y++) {
				// translate to a point and only draw if its valid
				tempPoint = new Point(x,y);
				allPoints[index] = tempPoint;
				index++;
			}
		}
		
		// Trying all possible subsets by pulling points at the indexes determined by Petar Minchev's algorithm
		int numSubsets = (1 << numPossiblePoints);
		HashSet<Point> nextSet = new HashSet<Point>(); // the next subset of points to try
		CellSet tempCellSet;
		int tempFitness;
		for (int i = 1; i < numSubsets; i++){
			// Create the next subset
			for (int j = 0; j < numPossiblePoints; j++) {
				if ((i & (1 << j)) > 0) { //The j-th element is used
					nextSet.add(allPoints[j]);
				}
			}
			// Get its fitness
			tempCellSet = new CellSet(nextSet, cellStartRadius);
			tempFitness = simulatedFitness(tempCellSet, maxSimulationIterations);
			
			// Update the best if its better than the old one
			if (bestCellSet == null || tempFitness > bestFitness) {
				bestCellSet = tempCellSet;
				bestFitness = tempFitness;
			}		
			nextSet = new HashSet<Point>(); // reset for the next cell set
			if (i%1000 == 0) System.out.println(i); // Can delete, just to watch progress
		}
		return bestCellSet; // return the optimal
	}	

	// @param cellStartRadius: the radius around the origin that the initial cells should fit in
	// @param numInitialCells: the number of initial cells that should be placed (has to fit within the radius)
	// @param maxSimulationIterations: the maximum number of iterations the initial boards will be run through to determine fitness
	// @param populationSize: the number of initialBoards in the population
	// @param mutationRate: the rate at which children experience mutations
	// @param geneticsGenerations: the number of times the genetics algorithm runs before returning its result
	// @return bestInitialBoard: returns the best initial board according to the fitness function determined by the genetics algorithm
	public static CellSet bestByGenetics(int cellStartRadius, int maxSimulationIterations, 
			int populationSize, double mutationRate, int geneticsGenerations) {		
		// Set the population to start as a random list of solutions
		Population p = new Population(populationSize, maxSimulationIterations, cellStartRadius, true);

		Population nextGen;
		CellSet p1, p2, child;

		for (int i = 0; i < geneticsGenerations; i++) {
			nextGen = new Population(populationSize, maxSimulationIterations);	
			
			// Elitist Fix: the best automatically goes through to the next generation
			if(!p.isEmpty()) {
				nextGen.add(p.fittest());
			}
			
			while (!nextGen.isFull()) {
				
				p1 = p.selectByFitness();
				p2 = p.selectByFitness();
				child = p1.cross(p2);
				
				// Small Chance to mutate the cells
				child.mutate(mutationRate);

				nextGen.add(child);
			}
			p = nextGen;
			if (i%10 == 0) {
				System.out.println(i+"	"+p.fittestFitness()); // Can delete, just to watch progress
			}
		}
		return p.fittest();
	}

	// @purpose: update the cell data for the next step in the simulation following the rules
	//	1. Any live cell with fewer than two live neighbors dies, as if caused by under-population.
	//	2. Any live cell with two or three live neighbors live on to the next generation. 
	//	3. Any live cell with more than three live neighbors dies, as if by overcrowding. 
	//	4. Any dead cell with exactly three live neighbors becomes a live cell, as if by reproduction.
	// @return newLivingCells: a HashSet<Point> of the living cells for the next simulation step
	public static CellSet updateCells(CellSet oldLivingCells) {
		CellSet newLivingCells = oldLivingCells.copy();
		CellSet adjDeadCells = new CellSet();

		Point[] neighboringPoints;
		int livingNeighbors = 0;
		HashSet<Point> cellPoints = oldLivingCells.getCells();

		// Checking rules 1-3 for live cells
		for (Point cell : cellPoints) {
			neighboringPoints = getNeighboringPoints(cell);
			livingNeighbors = 0;

			// getting the number of living cells and tracking nearby dead cells to use later
			for (Point neighbor : neighboringPoints) {
				if (oldLivingCells.contains(neighbor)) livingNeighbors +=1;
				else adjDeadCells.addCell(neighbor);				
			}

			// Removes the cell from the next generation if it died by rule 1 or 3
			if (livingNeighbors < 2 || livingNeighbors > 3) newLivingCells.removeCell(cell);
		}

		// Checking rule 4
		for (Point deadCell : adjDeadCells.getCells()) {
			neighboringPoints = getNeighboringPoints(deadCell);
			livingNeighbors = 0;

			// get the number of adjacent living cells to this dead cell
			for (Point neighbor : neighboringPoints) {
				if (oldLivingCells.contains(neighbor)) livingNeighbors +=1;			
			}

			// Add the cell as a living cell to the next generation if it follows rule 4
			if (livingNeighbors == 3) newLivingCells.addCell(deadCell);
		}
		return newLivingCells;
	}

	// @param cellBoard: an initial cellBoard to determine the fitness of
	// @param maxSimulationIterations: the maximum number of 
	// @return fitness: the total number of new cells generated by this initial board within the limit
	public static int simulatedFitness(CellSet initialBoard, int maxSimulationIterations) {
		int fitness = 0;
		CellSet oldBoard = initialBoard.copy();
		CellSet newBoard;
		CellSet tempSet;

		for (int i = 0; i < maxSimulationIterations; i++) {
			if (oldBoard.isEmpty()) {
				break;
			}

			tempSet = updateCells(oldBoard);
			newBoard = tempSet.copy();

			// compare the old board to the new board and add the amount of new cells to the fitness
			tempSet.removeAll(oldBoard.getCells());
			fitness += tempSet.size();

			oldBoard = newBoard;
		}

		return fitness;
	}

	// @param cellPoint: the point of a cell location
	// @return neighborCells: points array of neighboring cell points (later will be judged to be living/dead)
	private static Point[] getNeighboringPoints(Point cellPoint) {
		Point neighborCells[] = new Point[8];
		int shifts[] = {-1,0,1};
		double adjX, adjY;

		int i = 0;
		for (int x : shifts) {
			for(int y : shifts) {
				if (!(x == 0 && y == 0)) {
					adjX = cellPoint.getX()+x;
					adjY = cellPoint.getY()+y;
					Point cell = new Point((int) adjX,(int) adjY);
					neighborCells[i] = cell;
					i++;
				}
			}
		}
		return neighborCells;
	}
}