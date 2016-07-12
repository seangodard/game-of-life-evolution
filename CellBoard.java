/**
 * Represent a set of cells with helpful methods for manipulating the cell sets for the genetics algorithm.
 *   @author Sean Godard
 */

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class CellBoard {
	private static Random rand = new Random();
	private HashSet<Point> cells = new HashSet<>();
	private int cell_radius = 0;

    /**
     * Empty constructor.
     */
	public CellBoard(){}

    /**
     * Create a randomly populated Cell Board
     * @param cell_radius the radius of the board. If zero then the board is just one cell in size
     */
	public CellBoard(int cell_radius) {
		this.cell_radius = cell_radius;
		randomlyPopulate();
	}

    /**
     * Create a Cell Board with pre-existing cells. Precondition: all the cells must have been verified to fit within
     *  the radius. Where the central coordinate is 0,0.
     * @param cells the set of cell coordinates that contain living cells
     * @param cell_radius the radius of the set
     */
	public CellBoard(HashSet<Point> cells, int cell_radius) {
		this.cells = cells;
		this.cell_radius = cell_radius;
	}

    /**
     * Randomly populate the HashSet with living cell points within the radius.
     */
    private void randomlyPopulate() {
        cells = new HashSet<>();
        int num_cells = rand.nextInt((int) Math.pow((double) cell_radius*2+1, 2));

        // Make a list of empty places
        ArrayList<Point> empty_spots = new ArrayList<>();
        for (int x = -cell_radius; x <= cell_radius; x++) {
            for (int y = -cell_radius; y <= cell_radius; y++) {
                empty_spots.add(new Point(x,y));
            }
        }

        // Randomly select numCells random cell locations
        for (int i = 0; i < num_cells; i++) {
            int index = rand.nextInt(empty_spots.size());
            cells.add(empty_spots.get(index));
            empty_spots.remove(index);
        }
    }

    /**
     * @return this CellBoard's current radius
     */
	public int getCellRadius() { return this.cell_radius; }

    /**
     * @return this CellBoard's set of cell points
     */
	public HashSet<Point> getCells() { return this.cells; }

    /**
     * @return  whether or not this board is empty
     */
	public boolean isEmpty() { return cells.isEmpty(); }

    /**
     * Remove all of the given cells from this cell set.
     * @param cell_set the set of cells to remove from this set
     * @return true if the set changed as a result of this call
     */
	public boolean removeAll(HashSet<Point> cell_set) { return cells.removeAll(cell_set); }

    /**
     * Add the given cell point to this cell and expanding the radius if it is exceeded.
     * @param cell the point of a living cell to add to this board
     */
	public void addCell(Point cell) {
		if (Math.abs((int) cell.getX()) > cell_radius) cell_radius = (int) cell.getX();
		if (Math.abs((int) cell.getY()) > cell_radius) cell_radius = (int) cell.getY();
		cells.add(cell);
	}

    /**
     * Remove the given cell point if it exists.
     * @param cell the point of the cell to remove from this Cell Board
     * @return true if the cell was removed from the set successfully
     */
	public boolean removeCell(Point cell) { return cells.remove(cell); }

    /**
     * @param cell the point to check if there is a living cell there
     * @return whether or not this board contains that cell
     */
	public boolean contains(Point cell) { return cells.contains(cell); }

    /**
     * @return the number of cells in this object
     */
	public int size() { return cells.size(); }

    /**
     * @param other_parent a CellBoard to cross with this one
     *     Prerequisite: the two boards have the same radius
     * @return the child CellBoard of these two parents
     */
	public CellBoard crossRandSplit(CellBoard other_parent) {
        assert (this.cell_radius != other_parent.getCellRadius());

        CellBoard child = new CellBoard(this.cell_radius);

		boolean split_on_x = rand.nextBoolean();
		int split_index = rand.nextInt(cell_radius*2+1)-cell_radius;

		if (split_on_x) {
			for (Point cell : this.cells) {
				if (cell.getX() <= split_index) {
					child.addCell(cell);
				}
			}
			for (Point cell : other_parent.getCells()) {
				if (cell.getX() > split_index) {
					child.addCell(cell);
				}
			}
		} 
		else {
			for (Point cell : this.cells) {
				if (cell.getY() <= split_index) {
					child.addCell(cell);
				}
			}
			for (Point cell : other_parent.getCells()) {
				if (cell.getY() > split_index) {
					child.addCell(cell);
				}
			}
		}
		return child;
	}

    /**
     * @param other_parent a CellBoard to cross with this one
     *     Prerequisite: the two boards have the same radius
     * @return the child CellBoard of these two parents
     */
	public CellBoard cross(CellBoard other_parent) {
		assert (this.cell_radius != other_parent.getCellRadius());

		CellBoard child = new CellBoard(this.cell_radius);

		int xSplit = rand.nextInt(cell_radius*2+1)-cell_radius;

		for (Point cell : this.cells) {
			if (cell.getX() <= xSplit) {
				child.addCell(cell);
			}
		}
		for (Point cell : other_parent.getCells()) {
			if (cell.getX() > xSplit) {
				child.addCell(cell);
			}
		}
		return child;
	}

    /**
     * Randomly move a living cell or add in a living cell if empty.
     * @param mutation_rate the percentage of the time to flip the cell to the opposite state
     */
	public void mutate(double mutation_rate) {
		Point tempPoint;

		for (int x = -cell_radius; x <= cell_radius; x++) {
			for (int y = -cell_radius;y <= cell_radius;y++) {
				if (rand.nextFloat()<mutation_rate) {
					tempPoint = new Point(x,y);
					if (cells.contains(tempPoint)) cells.remove(tempPoint);
					else cells.add(tempPoint);
				}
			}
		}
	}

    /**
     * @return a string representation of this board
     */
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

    /**
     * @return a copy of this cell board set
     */
	public CellBoard copy() {
		HashSet<Point> copiedCellSet = new HashSet<>();
		copiedCellSet.addAll(cells);			
		return new CellBoard(copiedCellSet, cell_radius);
	}
}