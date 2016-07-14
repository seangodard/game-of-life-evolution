/**
 * A class for representing worker that generates the children of the next population of the main genetic
 *  simulator. Can be run on another thread.
 * @author Sean Godard
 */
public class GeneticsSimWorker implements Runnable {
    private boolean done = false;
    private boolean paused = true;
    private double mutation_rate;
    private int simulation_lifespan;
    private GeneticSimCallbacks callbacks;
    private int current_generation = 0;

    /**
     * Create a new runnable worker
     * @param mutation_rate the mutation rate to use on the children that it produces
     * @param simulation_lifespan the number of simulation generations to compute the child's fitness on
     * @param callbacks the callbacks to use to communicate with the main GeneticSimulator
     */
    public GeneticsSimWorker(double mutation_rate, int simulation_lifespan, GeneticSimCallbacks callbacks) {
        this.mutation_rate = mutation_rate;
        this.simulation_lifespan = simulation_lifespan;
        this.callbacks = callbacks;
    }

    @Override
    public void run() {
        while (!done) {
            if (paused) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }
            }

            // Note: we catch the exception here because sometimes the thread temporarily outlives the population?
            try {
                CellBoard p1 = callbacks.selectByFitness();
                CellBoard p2 = callbacks.selectByFitness();

                CellBoard child = p1.cross(p2);
                child.mutate(mutation_rate);

                current_generation = callbacks.add(child, Simulation.simulatedFitness(child, simulation_lifespan),
                        current_generation);
            } catch (Exception e) {
                System.err.println("caught");
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify this thread to stop performing computations.
     */
    public synchronized void done() { this.done = true; }

    /**
     * Let the thread know to pause its execution.
     */
    public synchronized void pause() { this.paused = true; }

    /**
     * Let the thread know to resume its execution.
     */
    public synchronized void cont() {
        this.paused = false;
        notify();
    }

    /**
     * @return If the thread is set to paused
     */
    public boolean isPaused() { return paused; }
}
