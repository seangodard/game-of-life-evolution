/*
 * Author: Sean Godard
 * Purpose: Implement Conway's Game of Life with Genetics Algorithm to create a board that has
 * 		the highest growth of population within an initial radius and a certain number of generations

 * How to use the program
 * -After board is decided, click to start the simulation and click again to restart
 * -Use the String and boolean variables to change which portion of the code runs or which file is used
 *  -Boards are saved in the location of the application
 * 	-To create custom boards simply places each point on its own line in a text document with each line in the
 * 		form 'x,y'

 * Requirements:
 * -Java 1.8.0_45-b14

 * References: Java Docs, JavaFX Docs, referencing in part previous personal code for GUI
 * 		-https://docs.oracle.com/javafx/2/api/javafx/scene/canvas/Canvas.html
 * 	-http://stackoverflow.com/questions/25224323/javafx-2-background-and-platform-runlater-vs-task-service
 * 	-http://docs.oracle.com/javafx/2/api/javafx/embed/swing/SwingFXUtils.html
 * 	-https://docs.oracle.com/javafx/2/events/convenience_methods.htm
 * 	-http://stackoverflow.com/questions/2885173/java-how-to-create-a-file-and-write-to-a-file
 * 	-http://stackoverflow.com/questions/15749192/how-do-i-load-a-file-from-resource-folder
 */

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Main extends Application {
	// Variables for Display:
	private static int CELL_SIZE = 4, NUM_CELLS_DISPLAY = 161; // display number is odd so that center axis fits nicely

	private static int WIDTH = CELL_SIZE * NUM_CELLS_DISPLAY, HEIGHT = CELL_SIZE * NUM_CELLS_DISPLAY;
	private static final ImageView imageViewer = new ImageView(); // for the game display
	private static UIUpdater updateThread = new UIUpdater();
	private static Desktop desktop = Desktop.getDesktop(); // for file menu
	private static Label fitnessLabel = new Label("Fitness: ");
	private static Font fitnessFont = new Font("Calibri",30);
	private static Color CELL_COLOR = new Color(0,128,0);
	private static Color BACKGROUND_COLOR = new Color(245,245,245);
	// protection with semaphores between the UI updater thread and the event click thread to change the initial board without conflicting access
	public static Semaphore UI = new Semaphore(1);
	private static boolean isPaused;

	// Other Variables for the UI: set one at a time
	private static boolean optimal = false;

	// Variables for Simulation:
	private static int INITIALIZATION_CELL_RADIUS = 3; // 2
	private static int MAX_SIMULATION_LOOPS = 100;
	private static int POPULATION_SIZE = 200; // larger seems to do better
	private static double MUTATION_RATE = .15; // doesn't seem to matter too much
	private static int GENETICS_GEN = 250; 
	private static CellSet sim; // the board modified for displaying the simulation
	private static CellSet initialBoard; // the preserved board for restoration on reset

	// @purpose: launch the application
	public static void main(String[] args) {
		launch(args);
	}

	// @purpose: initialize the cell data etc. for initial testing
	// @Guide: uncomment the desired section of code to use and comment the other sections
	@Override
	public void init() {

		// GUI Program

		// Initialize the fitness label style
		fitnessLabel.setFont(fitnessFont);

		// Initialize the board to the 5x5 board
		Point[] optimal5x5Points = {new Point(-2,-2), new Point(-2,0), new Point(-2,2), new Point(0,-2), new Point(0,2),
				new Point(2,-2), new Point(2,0), new Point(-2,-1), new Point(-2,1), new Point(0,1), new Point(2,-1),
				new Point(-1,1), new Point(1,1), new Point(-1,0), new Point(-1,2)};
		HashSet<Point> optimal5x5 = new HashSet<Point>();
		for (Point point : optimal5x5Points) {
			optimal5x5.add(point);
		}
		CellSet temp = new CellSet(optimal5x5,2);
		sim = temp;
		initialBoard = sim.copy();
		fitnessLabel.setText("Fitness: "+Simulation.simulatedFitness(sim, MAX_SIMULATION_LOOPS));

		// Non-GUI Genetics Algorithm Data Gathering Code

		//		int num_trials = 50;
		//		for (int i = 0; i<num_trials; i++) {
		//			CellSet genBest = Simulation.bestByGenetics(INITIALIZATION_CELL_RADIUS, MAX_SIMULATION_LOOPS,POPULATION_SIZE,MUTATION_RATE,GENETICS_GEN);
		//			System.out.println(i+"	"+Simulation.simulatedFitness(genBest, MAX_SIMULATION_LOOPS));
		//			saveBoard(genBest, "genetics"+i);
		//		}		
		//		System.exit(0);

		// Non-GUI code for a single run through of either the optimal algorithm or the genetics algorithm

		//		if (optimal) {
		//			sim = Simulation.bruteForceBest(INITIALIZATION_CELL_RADIUS, MAX_SIMULATION_LOOPS);
		//			System.out.println("Fitness:" + Simulation.simulatedFitness(sim, MAX_SIMULATION_LOOPS));
		//			initialBoard = sim.copy();
		//			saveBoard(initialBoard, "optimal");
		//		}
		//		else {
		//			System.out.println("Starting.");
		//			CellSet genBest = Simulation.bestByGenetics(INITIALIZATION_CELL_RADIUS, MAX_SIMULATION_LOOPS,POPULATION_SIZE,MUTATION_RATE,GENETICS_GEN);
		//			System.out.println("Fitness:" + Simulation.simulatedFitness(genBest, MAX_SIMULATION_LOOPS));
		//			sim = genBest.copy();
		//			initialBoard = genBest.copy();
		//			// save the board
		//			saveBoard(initialBoard, "genetics7x7");			
		//		}
		//		System.exit(0);	
	}

	// @effect: Creating the environment for the GUI
	@Override
	public void start(final Stage stage) throws Exception {
		// Initialize the window's settings
		stage.setTitle("Conway's Game of Life");
		imageViewer.setFitHeight(HEIGHT);
		imageViewer.setFitWidth(WIDTH);

		// Create the environment for the display 
		BorderPane border = new BorderPane();
		Scene s = new Scene(border);
		border.setCenter(imageViewer); // The imageViewer is what displays the next image of the simulation		

		// setting up the MenuBar with ability to load a new file
		final FileChooser fileChooser = new FileChooser();

		MenuBar menuBar = new MenuBar();
		Menu menuFile = new Menu("File");
		MenuItem openFile = new MenuItem("Open File...     ");
		openFile.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				setupFilechooser(fileChooser);
				File file = fileChooser.showOpenDialog(stage);
				if (file != null) {
					CellSet tempLoaded = loadBoard(file);

					// acquire the UI updater so that this is the only one running
					if (!isPaused) {
						try {UI.acquire();} catch (InterruptedException e) {e.printStackTrace();}
					}
					initialBoard = tempLoaded;
					sim = tempLoaded;
					fitnessLabel.setText("Fitness: "+Simulation.simulatedFitness(sim, MAX_SIMULATION_LOOPS));
					displayNext();
					isPaused = true;
				}
			}
		});
		menuFile.getItems().add(openFile);
		menuBar.getMenus().add(menuFile);

		// add information bar that shows the fitness of the board along with the menu bar to the top
		VBox vBox = new VBox();
		vBox.getChildren().add(menuBar);
		vBox.getChildren().add(fitnessLabel);
		border.setTop(vBox);

		// Display the initial image
		stage.setScene(s);
		stage.show();
		displayNext();

		// acquire the UI semaphore so that the UI updater can't start yet
		try {UI.acquire();} catch (InterruptedException e) {e.printStackTrace();}
		isPaused = true;

		updateThread.start();

		// This resets the display and pauses using semaphores to make sure the UI updater isn't working while the data is changed
		s.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent me) {
				if (isPaused) {
					// release the semaphore so the UI updater can continue
					isPaused = false;
					UI.release();		
				} 
				// display is running so reset image since clicked and set back to paused by not releasing the semaphore
				else {
					// acquire the semaphore so that the UI updater isn't running while the data is swapped
					try {UI.acquire();} catch (InterruptedException e) {e.printStackTrace();}
					isPaused = true;
					sim = initialBoard;
					displayNext();
				}
			}
		});
	}

	// @effect: set up the file chooser window
	private static void setupFilechooser(final FileChooser fileChooser) {
		fileChooser.setTitle("Choose Initial Board");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("TXT", "*.txt")
				);
	}

	// @effect: load in the file and change the initial board to the one loaded from the file
	private synchronized static CellSet loadBoard(File file) {
		CellSet cellSet = new CellSet();
		// Read in the file 
		try {
			Scanner scan = new Scanner(file);
			String temp; // stores the line before turning it into a point
			String[] tempArray;
			while(scan.hasNext()) {
				temp = scan.next();
				tempArray = temp.split(",");
				cellSet.addCell(new Point(Integer.parseInt(tempArray[0]), Integer.parseInt(tempArray[1])));
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return cellSet;
	}

	// @effect: save the cell board to a file within a folder where the application resides; Note: file is deleted if it already exists
	private synchronized static void saveBoard(CellSet cellSet, String name) {
		// Create the writer to save the board
		PrintWriter writer;
		try {
			writer = new PrintWriter(name+".txt", "UTF-8");
			// Save each point of the cellSet into the file as a list of points with each point on it's own line
			HashSet<Point> cellPoints = cellSet.getCells();
			for (Point p: cellPoints) {
				writer.println(p.x+","+p.y);
			}
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// @purpose: move the simulation forward one step and update the display to show the changes
	public synchronized static void displayNext() {
		// create the buffered image for drawing the next simulation image on
		BufferedImage bufImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D drawOn = bufImage.createGraphics();
		drawOn.setColor(BACKGROUND_COLOR);
		drawOn.fillRect(0, 0, WIDTH, HEIGHT);
		drawOn.setColor(CELL_COLOR);

		Point tempPoint;
		// Loop through the display-able cell region and translate any living point in there to the actual grid
		int winRadius = (NUM_CELLS_DISPLAY - 1)/2;
		for (int x = -winRadius; x <= winRadius; x++) {
			for (int y = -winRadius; y<= winRadius;y++) {
				// translate to a point and only draw if its valid
				tempPoint = new Point(x,y);
				if (sim.contains(tempPoint)) {
					// Translate to standard xy coordinates so that it's not flipped because of the way that java does coordinates
					drawOn.fillRect(x*CELL_SIZE+winRadius*CELL_SIZE, -y*CELL_SIZE+winRadius*CELL_SIZE, CELL_SIZE, CELL_SIZE);
				}
			}
		}

		// Convert the buffered image to an image and display it in the imageViewer node
		WritableImage img = new WritableImage(bufImage.getWidth(), bufImage.getHeight());
		SwingFXUtils.toFXImage(bufImage, img);	
		imageViewer.setImage(img);

		// Update the board for the next time this is called
		sim = Simulation.updateCells(sim);
	}

	// @effect: when called, close the GUI updater thread because otherwise it will run until it is manually closed, and then terminates the entire program
	@Override
	public void stop() {
		updateThread.done();
		UI.release(); // a fix to release the semaphore so that all threads will close 
		try {updateThread.join();} catch (InterruptedException e) {e.printStackTrace();}
	}
}