/**
 * A class to run the genetics algorithm with the given parameters on a thread.
 * @author Sean Godard
 */

public class GeneticsSimulator implements Runnable {
    private boolean DONE = false;
    private int cell_start_radius;
    private int simulation_lifespan;
    private int population_size;
    private double mutation_rate;
    private int genetics_generations;
    private SimulationCallbacks callbacks;

    /**
     * @param cell_start_radius the radius around the origin that the initial cells should fit in
     * @param simulation_lifespan the maximum number of iterations the initial boards will be run through to determine
     *     fitness
     * @param population_size the number of initialBoards in the population
     * @param mutation_rate the rate at which children experience mutations
     * @param genetics_generations the number of times the genetics algorithm runs before returning its result
     * @param callbacks The implemented interface to respond to progress updates and the completion of the simulation.
     */
    public GeneticsSimulator(int cell_start_radius, int simulation_lifespan, int population_size,
        double mutation_rate, int genetics_generations, SimulationCallbacks callbacks) {

        this.cell_start_radius = cell_start_radius;
        this.simulation_lifespan = simulation_lifespan;
        this.population_size = population_size;
        this.mutation_rate = mutation_rate;
        this.genetics_generations = genetics_generations;
        this.callbacks = callbacks;
    }

    /**
     * Runs the simulation to determine the best initial CellBoard according to the genetics algorithm using its fitness
     *     function.
     */
    @Override
    public void run() {
        // Set the population to start as a random list of solutions
        Population p = new Population(population_size, simulation_lifespan, cell_start_radius, true);

        Population nextGen;
        CellBoard p1, p2, child;

        callbacks.progress(0);
        for (int i = 0; i < genetics_generations && !DONE; i++) {
            nextGen = new Population(population_size, simulation_lifespan);

            // Elitist Fix: the best automatically goes through to the next generation
            if(!p.isEmpty()) {
                nextGen.add(p.fittest());
            }

            while (!nextGen.isFull()) {

                p1 = p.selectByFitness();
                p2 = p.selectByFitness();
                child = p1.cross(p2);

                // Small Chance to mutate the cells
                child.mutate(mutation_rate);

                nextGen.add(child);
            }
            p = nextGen;

            if (i%10 == 0) { callbacks.progress((double) i/genetics_generations); }
        }
        // Only return the value if the simulation completed
        if (!DONE) {
            callbacks.finished(p.fittest());
            callbacks.progress(1);
        }
    }

    /**
     * Notify the thread to stop performing computations.
     */
    public synchronized void done() { this.DONE = true; }
}
