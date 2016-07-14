/**
 * An interface for the callbacks for the GeneticsSimWorker class to use to communicate with the main Genetic Simulator.
 * @author Sean Godard
 */
public interface GeneticSimCallbacks {

    /**
     * @return a CellBoard selected based upon the boards fitness
     */
    CellBoard selectByFitness();

    /**
     * Attempt to add the given board to the main genetic simulation
     * @param board the CellBoard to attempt to add
     * @param fitness the fitness of the board
     * @param generation the generation that this CellBoard should go to
     * @return the current generation of the main thread
     */
    int add(CellBoard board, int fitness, int generation);
}
