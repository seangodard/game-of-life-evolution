/**
 * Represent a population for the genetics algorithm with methods to select based on fitness.
 * @author Sean Godard
 */

import java.util.ArrayList;
import java.util.Random;

/**
 * Represent a population of CellBoards for use with the genetics algorithm.
 * @author Sean Godard
 */
public class Population {
    private int sim_lifespan, max_size, size = 0;
    private static int cell_start_radius = 0;
    private static final int PROB_RESOLUTION = 1000; // The resolution of the probability (higher is better)

	private Random rand = new Random();
    private boolean is_updated = false; /* if the population probabilities for selection are up to date. Allows deferring
                                         *  the computations until all cell boards are added */

    // note: index identifies each board in fitness, probabilities, and population
    private CellBoard[] population;
    private int[] fitness;
    private ArrayList<CellBoard> prob_selection_array; /* Stores duplicate CellBoards based on their fitness
                                                        *  probabilities */

    /**
     * Constructor
     * @param max_size the maximum size that the population can be
     * @param sim_lifespan the maximum iterations to simulate the fitness on per CellBoard
     */
	public Population(int max_size, int sim_lifespan) {
		this.sim_lifespan = sim_lifespan;
		this.max_size = max_size;
		population = new CellBoard[max_size];
		fitness = new int[max_size];
	}

    /**
     * Constructor
     * @param max_size the maximum size that the population can be
     * @param sim_lifespan the maximum iterations to simulate the fitness on per CellBoard
     * @param cell_start_radius the radius that the cells should be generated within when randomizing
     * @param rand whether or not to initialize the population with random CellBoards
     */
	public Population(int max_size, int sim_lifespan, int cell_start_radius, boolean rand) {
		this(max_size, sim_lifespan);
        this.cell_start_radius = cell_start_radius;

		if (rand) {
			CellBoard tmp;
			while (!isFull()) {
				tmp = new CellBoard(cell_start_radius);
				add(tmp);
			}
		}
	}

    /**
     * @return whether or not this population is full
     */
	public boolean isFull() { return size == max_size; }

    /**
     * @return whether or not this population is empty
     */
	public boolean isEmpty() { return size == 0; }

    /**
     * Add the cell to the population along with the fitness of that board
     * @param cell_board a cell set to add to the population.
     *     Precondition: must have the same size as the rest of the population
     * @return whether or not the cell_board was added to the population
     */
	public boolean add(CellBoard cell_board) {
		if (isFull()) return false;
		else {
			population[size] = cell_board;
			fitness[size] = Simulation.simulatedFitness(cell_board, sim_lifespan);
			size++;
			is_updated = false;
			return true;
		}
	}

    /**
     * @return a parent board (higher probability given to boards with a higher fitness)
     */
	public CellBoard selectByFitness() {
		// update the probabilities if they are not up to date
		if (!is_updated) updateProbArray();

		return prob_selection_array.get(rand.nextInt(prob_selection_array.size()));
	}

    /**
     * Updates the fitness-based CellBoard selection array so that random selection from this array results in a greater
     *  likelihood of a higher fitness board being selected.
     */
	private void updateProbArray() {
		long totalFitness = 0;

		// Increase the fitness by one (to give 0 a chance to go through) and get the total fitness
		for (int i = 0; i < size; i++) {
			fitness[i] = fitness[i]+1;
			totalFitness += fitness[i];
		}

        prob_selection_array = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // Adds one so that 0 fitness has a selection chance
            int reps = (int) (fitness[i] / (double)totalFitness) * PROB_RESOLUTION + 1;

            for (int j = 0; j < reps; ++j) { prob_selection_array.add(population[i]); }
		}
	}

    // TODO: 7/6/16 SpeedUp Algorithm
    /**
     * @return the fittest CellBoard in the population
     */
	public CellBoard fittest() {
		int bestIndex = 0;
		for (int i = 0; i < size; i++) {
			if (fitness[i] > fitness[bestIndex]) bestIndex = i;
		}
		return population[bestIndex];
	}

    // TODO: 7/6/16 SpeedUp Algorithm
    /**
     * @return the fittest CellBoard in the population
     */
	public int fittestFitness() {
		int bestIndex = 0;
		for (int i = 0; i < size; i++) {
			if (fitness[i] > fitness[bestIndex]) bestIndex = i;
		}
		return fitness[bestIndex];
	}

    /**
     * @return the string representation of the finesses of this population
     */
	@Override
	public String toString() {
		String population = " ";
		for (int i = 0; i < size; i++) {
			if (i < size - 1) {
				population += this.fitness[i]+", ";
			}
			else {
				population += this.fitness[i];
			}			
		}
		return population;
	}
}