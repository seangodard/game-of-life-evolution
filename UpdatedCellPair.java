/**
 * A simple class for holding the pair of the updated CellBoard and the number of new cells produced from the previous
 *  board.
 * @author Sean Godard
 */
public class UpdatedCellPair {
    private CellBoard updated_board;
    private int num_new_cells;

    public UpdatedCellPair(CellBoard updated_board, int num_new_cells) {
        this.updated_board = updated_board;
        this.num_new_cells = num_new_cells;
    }

    public CellBoard getBoard() {
        return updated_board;
    }

    public int getNumNewCells() {
        return num_new_cells;
    }
}
