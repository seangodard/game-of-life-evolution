/**
 * Holds the functions to respond to simulation events. (Serve as callbacks)
 * @author Sean Godard
 */
public interface SimulationCallbacks {

    /**
     * @param cell_board Respond the the finishing of a CellBoard
     */
    void finished(CellBoard cell_board);

    /**
     * @param percentage Respond to progress being made on a simulation. The value should be between 0 and 1.
     */
    void progress(double percentage);
}
