// Author: Sean Godard
// Purpose: Represent a population for the genetics algorithm with methods to select based on fitness

import java.util.Random;

public class Population {
	// Variables
	private boolean isProbUpToDate = false; // lets the population know if the probabilities are up to date
	private int populationSize = 0;
	private int maxPopSize = 0;
	private CellSet[] population;
	private int[] fitness;
	private double probabilities[]; // probabilities for fitness selection
	private int maxSimulationIterations;
	private Random rand = new Random();
	// note: index identifies each board in fitness, probabilities, and population

	// Constructor
	// @param maxPopSize: the maximum size that the population can be
	// @param maxSimulationIterations: the maximum iterations to simulate the fitness on
	public Population(int maxPopSize, int maxSimulationIterations) {
		this.maxSimulationIterations = maxSimulationIterations;
		this.maxPopSize = maxPopSize;
		population = new CellSet[maxPopSize];
		fitness = new int[maxPopSize];
	}

	// Constructor
	// @param rand: whether or not to randomly initialize the population
	// @param cellStartRadius: the radius that the cells should be generated within when randomizing
	public Population(int maxPopSize, int maxSimulationIterations, int cellStartRadius, boolean rand) {
		this(maxPopSize, maxSimulationIterations);
		// randomly initialize the population
		if (rand) {
			CellSet randomBoard;
			while (true) {
				randomBoard = new CellSet(cellStartRadius);
				add(randomBoard);
				if (isFull()) break;
			}
		}
	}

	// @return: whether or not this population is full
	public boolean isFull() {
		return populationSize == maxPopSize;
	}
	
	// @return: whether or not this population is empty
	public boolean isEmpty() {
		return populationSize == 0;
	}

	// @param: a cell set to add to the population
	// @return: whether or not the cellSet was added to the population
	// @effect: add the cell to the population along with the fitness of that board
	public boolean add(CellSet cellSet) {
		if (isFull()) return false;
		else {
			population[populationSize] = cellSet;
			fitness[populationSize] = Simulation.simulatedFitness(cellSet, maxSimulationIterations);
			populationSize++;
			isProbUpToDate = false;
			return true;
		}
	}

	// @param population: a list of boards in the population
	// @param fitness: the corresponding fitness of each board
	// @return: a parent board (higher probability given to boards with a higher fitness)
	public CellSet selectByFitness() {
		// update the probabilities if they are not up to date
		if (!isProbUpToDate) updateProbabilities(); 
		
		double randPoint = rand.nextFloat();
		
		// Add probabilities to a sum,if you add one that passes the random value then that one is selected and returned
		double tempSum = 0;
		for (int i = 0; i < populationSize;i++) {
			tempSum += probabilities[i];
			if (tempSum > randPoint) {
				return population[i];
			}
		}
		return population[populationSize-1]; // if this point is somehow reached -> return the last item
	}

	// @effect: update the probabilities array for fitness selection
	private void updateProbabilities() {
		long totalFitness = 0;
		// Increase the fitness by one (to give 0 a chance to go through) and get the total fitness
		for (int i = 0; i < populationSize; i++) {
			fitness[i] = fitness[i]+1;
			totalFitness += fitness[i];
		}
		this.probabilities = new double[populationSize];
		for (int i = 0; i < populationSize; i++) {
			probabilities[i] = fitness[i]/(double)totalFitness; 
		}
		
		//Testing
//		System.out.print("Fitnesses: ");
//		for (int i = 0; i < fitness.length;i++) {
//			System.out.print(fitness[i]+", ");			
//		}
//		System.out.println();
//		System.out.print("Probabilities: ");
//		for (int i = 0; i < fitness.length;i++) {
//			System.out.print(probabilities[i]+", ");			
//		}
//		System.out.println();	
	}
	
	// @return: the fittest CellSet in the population
	public CellSet fittest() {
		int bestIndex = 0;
		for (int i = 0; i < populationSize; i++) {
			if (fitness[i] > fitness[bestIndex]) bestIndex = i;
		}		
		return population[bestIndex];
	}
	
	// @return: the fittest CellSet in the population
	public int fittestFitness() {
		int bestIndex = 0;
		for (int i = 0; i < populationSize; i++) {
			if (fitness[i] > fitness[bestIndex]) bestIndex = i;
		}		
		return fitness[bestIndex];		
	}
	
	// @return: the string representation of the fitnesses of this population
	@Override
	public String toString() {
		String population = " ";
		for (int i = 0; i < populationSize; i++) {
			if (i < populationSize - 1) {
				population += this.fitness[i]+", ";
			}
			else {
				population += this.fitness[i];
			}			
		}
		return population;
	}
}