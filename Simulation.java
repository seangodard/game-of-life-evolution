/**
 * Holds the methods used to simulate the genetics algorithm and the rules of Conway's Game of Life
 * @author Sean Godard
 * @references
 *  -http://stackoverflow.com/questions/7206442/printing-all-possible-subsets-of-a-list -> Petar Minchev
 */

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public final class Simulation {
	private static Random rand = new Random();

    /**
     * @param cellStartRadius the area that the cells must start within
     * @param maxSimulationIterations the maximum number of loops to test the fitness within
     * @return the optimal placement of cells within that radius and upper simulation runs bound
     */
	public static CellBoard bruteForceBest(int cellStartRadius, int maxSimulationIterations) {
		CellBoard bestCellBoard = null;
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
		HashSet<Point> nextSet = new HashSet<>(); // the next subset of points to try
		CellBoard tempCellBoard;
		int tempFitness;
		for (int i = 1; i < numSubsets; i++){
			// Create the next subset
			for (int j = 0; j < numPossiblePoints; j++) {
				if ((i & (1 << j)) > 0) { //The j-th element is used
					nextSet.add(allPoints[j]);
				}
			}
			// Get its fitness
			tempCellBoard = new CellBoard(nextSet, cellStartRadius);
			tempFitness = simulatedFitness(tempCellBoard, maxSimulationIterations);
			
			// Update the best if its better than the old one
			if (bestCellBoard == null || tempFitness > bestFitness) {
				bestCellBoard = tempCellBoard;
				bestFitness = tempFitness;
			}		
			nextSet = new HashSet<>(); // reset for the next cell set
			if (i%1000 == 0) System.out.println(i); // Can delete, just to watch progress
		}
		return bestCellBoard; // return the optimal
	}

    /**
     * Update the cell data for the next step in the simulation following the rules:
     *  1. Any live cell with fewer than two live neighbors dies, as if caused by under-population.
     *  2. Any live cell with two or three live neighbors live on to the next generation.
     *  3. Any live cell with more than three live neighbors dies, as if by overcrowding.
     *  4. Any dead cell with exactly three live neighbors becomes a live cell, as if by reproduction.
     * @param old_board the old board
     * @return a new CellBoard containing the updated cells
     */
	public static UpdatedCellPair updateCells(CellBoard old_board) {
		CellBoard new_board = old_board.copy();
		HashMap<Point, Integer> adj_dead_cells = new HashMap<>();

		Point[] neighboring_points;
		int total_new_cells = 0;

		// Checking rules 1-3 for live cells
		for (Point cell : old_board) {
			neighboring_points = getNeighboringPoints(cell);
			int livingNeighbors = 0;

			// getting the number of living cells and tracking nearby dead cells to use later
			for (Point neighbor : neighboring_points) {
				if (old_board.contains(neighbor)) livingNeighbors +=1;
				else {
                    if (adj_dead_cells.containsKey(neighbor)) {
                        adj_dead_cells.put(neighbor, adj_dead_cells.get(neighbor) + 1);
                    }
                    else {
                        adj_dead_cells.put(neighbor, 1);
                    }
                }
			}

			// Removes the cell from the next generation if it died by rule 1 or 3
			if (livingNeighbors < 2 || livingNeighbors > 3) new_board.removeCell(cell);
		}

		// Checking rule 4
		for (Point deadCell : adj_dead_cells.keySet()) {
			// Add the cell as a living cell to the next generation if it follows rule 4
			if (adj_dead_cells.get(deadCell) == 3) {
				new_board.addCell(deadCell);
				total_new_cells++;
			}
		}
		return new UpdatedCellPair(new_board, total_new_cells);
	}

    /**
     * @param initial_board an initial cellBoard to determine the fitness of
     * @param maxSimulationIterations the maximum number of
     * @return the total number of new cells generated by this initial board within the limit
     */
	public static int simulatedFitness(CellBoard initial_board, int maxSimulationIterations) {
		int fitness = 0;
		CellBoard old_board = initial_board;
		CellBoard new_board;

		for (int i = 0; i < maxSimulationIterations; i++) {
			if (old_board.isEmpty()) {
				break;
			}

			UpdatedCellPair tmp = updateCells(old_board);
			new_board = tmp.getBoard();
			fitness += tmp.getNumNewCells();
			old_board = new_board;
		}

		return fitness;
	}

    /**
     * @param cellPoint the point of a cell location
     * @return points array of neighboring cell points (later will be judged to be living/dead)
     */
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