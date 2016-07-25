import java.util.concurrent.Semaphore;

/**
 * A class to run the genetics algorithm with the given parameters. Can be run on a new thread.
 * @author Sean Godard
 */

public class GeneticsSimulator implements Runnable {
    private boolean DONE = false;
    private boolean paused = false;
    private int cell_start_radius;
    private int simulation_lifespan;
    private int population_size;
    private double mutation_rate;
    private int genetics_generations;
    private int num_workers;
    private SimulationCallbacks callbacks;
    private int current_generation = 0;
    private Population population;
    private Population next_gen;
    private Semaphore semaphore = new Semaphore(1);

    /**
     * @param cell_start_radius the radius around the origin that the initial cells should fit in
     * @param simulation_lifespan the maximum number of iterations the initial boards will be run through to determine
     *     fitness
     * @param population_size the number of initialBoards in the population
     * @param mutation_rate the rate at which children experience mutations
     * @param genetics_generations the number of times the genetics algorithm runs before returning its result
     * @param num_workers the number of worker threads to create to assist in the genetic algorithm simulation by
     *  working on producing the CellBoards and their fitness for the population
     * @param callbacks The implemented interface to respond to progress updates and the completion of the simulation.
     */
    public GeneticsSimulator(int cell_start_radius, int simulation_lifespan, int population_size,
        double mutation_rate, int genetics_generations, int num_workers, SimulationCallbacks callbacks) {

        this.cell_start_radius = cell_start_radius;
        this.simulation_lifespan = simulation_lifespan;
        this.population_size = population_size;
        this.mutation_rate = mutation_rate;
        this.genetics_generations = genetics_generations;
        this.num_workers = num_workers;
        this.callbacks = callbacks;

        // Set the population to start as a random list of solutions
        this.population = new Population(population_size, simulation_lifespan, cell_start_radius, true);
    }

    /**
     * Runs the simulation to determine the best initial CellBoard according to the genetics algorithm using its fitness
     *     function.
     */
    @Override
    public void run() {
        callbacks.progress(0);
        double start_time = System.currentTimeMillis();

        // Initialize and start the worker threads
        GeneticsSimWorker workers[] = new GeneticsSimWorker[num_workers];
        Thread[] worker_threads = new Thread[num_workers];
        for (int i = 0; i < num_workers; ++i) {
            workers[i] = new GeneticsSimWorker(mutation_rate, simulation_lifespan,
                new GeneticSimCallbacks() {
                    @Override
                    public CellBoard selectByFitness() {
                        return population.selectByFitness();
                    }

                    @Override
                    public int add(CellBoard board, int fitness, int generation) {
                        return addBoard(board, fitness, generation);
                    }
            });
            worker_threads[i] = new Thread(workers[i]);
            worker_threads[i].setName("Genetic Worker "+i);
            worker_threads[i].start();
        }

        try { semaphore.acquire(); } catch (InterruptedException e) { if (Main.IS_DEBUG) { e.printStackTrace(); } }

        for (current_generation = 0; current_generation < genetics_generations && !DONE; ++current_generation) {
            population.updateFitnessProbabilities();
            next_gen = new Population(population_size, simulation_lifespan);

            // Resume any paused worker threads
            for (GeneticsSimWorker worker : workers) {
                synchronized (worker) {
                    if (worker.isPaused()) {
                        worker.cont();
                    }
                }
            }

            // Elitist Fix: the best automatically goes through to the next generation
            if (!population.isEmpty()) {
                next_gen.add(population.fittest(), population.fittestFitness());
            }

            // Wait for the next generation population to fill up then move the population forward
            paused = true;
            semaphore.release();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) { if (Main.IS_DEBUG) { e.printStackTrace(); } }
            try { semaphore.acquire(); } catch (InterruptedException e) { if (Main.IS_DEBUG) { e.printStackTrace(); } }

            // Pause any running workers
            for (GeneticsSimWorker worker : workers) {
                synchronized (worker) {
                    if (!worker.isPaused()) {
                        worker.pause();
                    }
                }
            }
            population = next_gen;

            if (current_generation%10 == 0) { callbacks.progress((double) current_generation/genetics_generations); }
        }
        semaphore.release();

        // Only return the value if the simulation completed
        if (!DONE) {
            double end_time = System.currentTimeMillis();
            callbacks.finished(population.fittest(), population.fittestFitness(), end_time - start_time);
            callbacks.progress(1);
        }

        // Close the worker threads
        for (GeneticsSimWorker worker : workers) {
            synchronized (worker) {
                if (worker.isPaused()) {
                    worker.cont();
                    worker.done();
                }
            }
        }
        for (Thread worker : worker_threads) {
            try { worker.join(); } catch (InterruptedException e) { if (Main.IS_DEBUG) { e.printStackTrace(); } }
        }
    }

    /**
     * Add a CellBoard to the current population and notify the main GeneticsSimulator when the population becomes full.
     * @param board the board to add to this population
     * @param fitness the fitness of the produced board
     * @param generation the population generation that this child is suppose to belong to
     * @return the current generation that the children should be added to
     */
    public int addBoard(CellBoard board, int fitness, int generation) {
        try {
            // Make sure that this thread is not trying to move the population forward a generation
            semaphore.acquire();
            if (this.current_generation == generation) {
                next_gen.add(board, fitness);

                if (next_gen.isFull() && paused) {
                    synchronized (this) {
                        paused = false;
                        this.notify();
                    }
                }
            }
            semaphore.release();
        } catch (InterruptedException e) {
            if (Main.IS_DEBUG) { e.printStackTrace(); }
            return -1;
        } finally {
            semaphore.release();
        }
        return current_generation;
    }

    /**
     * Notify this thread to stop performing computations.
     */
    public synchronized void done() { this.DONE = true; }
}
