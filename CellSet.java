// Author: Sean Godard
// Purpose: Represent a set of cells with helpful methods for manipulating the cell sets for the genetics algorithm 

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class CellSet {
	// Variables
	private Random rand = new Random();
	private HashSet<Point> cells = new HashSet<Point>();
	private int cellRadius = 0;

	// Constructor: empty constructor
	public CellSet(){};

	// Constructor: if no cells given then randomly generate the board
	public CellSet(int cellRadius) {
		this.cellRadius = cellRadius;
		randomlyPopulate();
	}

	// Constructor: if cells given then populate accordingly
	public CellSet(HashSet<Point> cells, int cellRadius) {
		this.cells = cells;
		this.cellRadius = cellRadius;		
	}

	// @return: this CellSets current radius
	public int getCellRadius() {
		return this.cellRadius;
	}

	// @return: this cells set of cell points
	public HashSet<Point> getCells() {
		return this.cells;
	}

	// @return: whether or not this board is empty
	public boolean isEmpty() {
		return cells.isEmpty();
	}

	// @return: true if the set changed as a result of this call
	// @effect: remove all of the given cells
	public boolean removeAll(HashSet<Point> cellset) {
		return cells.removeAll(cellset);
	}	

	// @param cell: a cell to add to this board
	// @effect: add the cell to the board and update the radius if it has changed
	public void addCell(Point cell) {
		if (Math.abs((int) cell.getX()) > cellRadius) cellRadius = (int) cell.getX();
		else if (Math.abs((int) cell.getY()) > cellRadius) cellRadius = (int) cell.getY();
		cells.add(cell);
	}

	// @param cell: a cell to remove from the board
	// @effect: remove a cell from the board
	public boolean removeCell(Point cell) {
		return cells.remove(cell);
	}

	// @param cell: a cell to see if it is in the cell set
	// @return: whether or not this board contains that cell
	public boolean contains(Point cell) {
		return cells.contains(cell);
	}

	// @return: the number of cells in this object
	public int size() {
		return cells.size();		
	}

	// @param otherParent: a CellSet to cross with this one
	// @return: the child CellSet of these two parents; return null if their radi aren't the same
	public CellSet crossRandSplit(CellSet otherParent) {
		if (this.cellRadius != otherParent.getCellRadius()) {
			System.err.println("Error: Parents must have the same initial radius.");
			return null;
		}
		HashSet<Point> child = new HashSet<Point>();
		boolean x = rand.nextBoolean(); // determines which direction to split x = true y = false

		// decide where to divide the cell boards
		int split = rand.nextInt(cellRadius*2+1)-cellRadius;

		// if it's an X split
		if (x) {
			// Copy all points less than that x from the first parent into the child
			for (Point cell : this.cells) {
				if (cell.getX() <= split) {
					child.add(cell);
				}
			}
			// Copy all points greater than that x from the second parent into the child
			for (Point cell : otherParent.getCells()) {
				if (cell.getX() > split) {
					child.add(cell);
				}
			}
		} 
		// if it's a Y split
		else {
			// Copy all points less than that y from the first parent into the child
			for (Point cell : this.cells) {
				if (cell.getY() <= split) {
					child.add(cell);
				}
			}
			// Copy all points greater than that y from the second parent into the child
			for (Point cell : otherParent.getCells()) {
				if (cell.getY() > split) {
					child.add(cell);
				}
			}
		}
		return new CellSet(child, this.getCellRadius());
	}

	// @param otherParent: a CellSet to cross with this one
	// @return: the child CellSet of these two parents; return null if their radi aren't the same
	public CellSet cross(CellSet otherParent) {
		if (this.cellRadius != otherParent.getCellRadius()) {
			System.out.println("Error: Parents must have the same initial radius.");
			return null;
		}
		HashSet<Point> child = new HashSet<Point>();

		// cut the first array at a random x point and add all those to the child from the first parent;
		int xSplit = rand.nextInt(cellRadius*2+1)-cellRadius;

		for (Point cell : this.cells) {
			if (cell.getX() <= xSplit) {
				child.add(cell);
			}
		}
		// Copy all points greater than that x in the second parent into the child
		for (Point cell : otherParent.getCells()) {
			if (cell.getX() > xSplit) {
				child.add(cell);
			}
		}
		return new CellSet(child, this.getCellRadius());
	}

	// @effect: randomly move a living cell or add in a living cell if empty
	public void mutate(double mutationRate) {
		Point tempPoint;
		// For each spot on the board
		for (int x = -cellRadius; x <= cellRadius; x++) {
			for (int y = -cellRadius;y <= cellRadius;y++) {
				if (rand.nextFloat()<mutationRate) {
					tempPoint = new Point(x,y);
					// Remove it if it is in the cell set
					if (cells.contains(tempPoint)) cells.remove(tempPoint);					
					// Add it if it is not in the cell set
					else cells.add(tempPoint);
				}
			}
		}
	}

	// @return: a string representation of this board
	@Override
	public String toString() {
		String cellsString = "";
		cellsString+= "Number Cells = "+cells.size()+": {";
		Iterator<Point> cellIterator = cells.iterator();
		Point nextPoint;

		while (cellIterator.hasNext()) {
			nextPoint = cellIterator.next();
			cellsString +="("+nextPoint.getX()+", "+nextPoint.getY()+")";
			if (cellIterator.hasNext()) cellsString += ", ";
		}
		cellsString += "}\n";
		return cellsString;	
	}

	// @return: a copy of this cell board set
	public CellSet copy() {
		HashSet<Point> copiedCellSet = new HashSet<Point>();		
		copiedCellSet.addAll(cells);			
		return new CellSet(copiedCellSet, cellRadius);
	}

	// @effect: randomly populate the HashSet of living cell points within the radius
	private void randomlyPopulate() {
		cells = new HashSet<Point>();
		// ensure this value is less than or equal to the area it covers 
		int numCells = rand.nextInt((int) Math.pow((double) cellRadius*2+1, 2)); 

		// Make a list of empty places
		ArrayList<Point> emptySpots = new ArrayList<Point>();
		for (int x = -cellRadius; x <= cellRadius; x++) {
			for (int y = -cellRadius;y <= cellRadius;y++) {
				emptySpots.add(new Point(x,y));
			}
		}

		// Randomly select numCells random cell locations
		for (int i = 0; i < numCells; i++) {
			int index = rand.nextInt(emptySpots.size());
			cells.add(emptySpots.get(index));
			emptySpots.remove(index);
		}
	}
}