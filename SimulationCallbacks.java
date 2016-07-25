/**
 * Holds the functions to respond to simulation events. (Serve as callbacks)
 * @author Sean Godard
 */
public interface SimulationCallbacks {

    /**
     * @param cell_board Respond the the finishing of a CellBoard
     * @param fitness the fitness of the computed board
     * @param milli_compute_time the time that it took to compute the solution (in milliseconds)
     */
    void finished(CellBoard cell_board, int fitness, double milli_compute_time);

    /**
     * @param percentage Respond to progress being made on a simulation. The value should be between 0 and 1.
     */
    void progress(double percentage);
}
