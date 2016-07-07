/**
 * Implement Conway's Game of Life with Genetics Algorithm to create a board that has
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
 * @author Sean Godard
 */

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Main extends Application {
    // Slider Limits
    private static final int MIN_CELL_RADIUS = 1;
    private static final int MAX_CELL_RADIUS = 30;
    private static final int MIN_SIMULATION_LIFESPAN = 1;
    private static final int MAX_SIMULATION_LIFESPAN = 10000;
    private static final int MIN_GENETICS_POPULATION = 2;
    private static final int MAX_GENETICS_POPULATION = 10000;
    private static final int MIN_GENETICS_GENERATIONS = 2;
    private static final int MAX_GENETICS_GENERATIONS = 10000;
    private static final double MIN_MUTATION_RATE = 0;
    private static final double MAX_MUTATION_RATE = 1;

	// Variables for Display:
	private static int CELL_SIZE = 4, NUM_CELLS_DISPLAY = 161; // display number is odd so that center axis fits nicely
    private static int WIDTH = CELL_SIZE * NUM_CELLS_DISPLAY, HEIGHT = CELL_SIZE * NUM_CELLS_DISPLAY;

    private static int SIDEBAR_SPACING = 4;
    private static double SIDEBAR_WIDTH = 250;
    private static double TRAY_HEIGHT = 20;

	private static Desktop desktop = Desktop.getDesktop(); // for file menu
	private static Label fitnessLabel = new Label("Fitness: ");
	private static Font fitnessFont = new Font("Calibri",30);
	private static Color CELL_COLOR = new Color(0,128,0);
	private static Color BACKGROUND_COLOR = new Color(245,245,245);

    private static VBox tray;

	// protection with semaphores between the UI updater thread and the event click thread to change the initial board without conflicting access
	public static Semaphore UI = new Semaphore(1);
	private static boolean isPaused;
    private static final ImageView imageViewer = new ImageView(); // for the game display
    private static UIUpdater updateThread = new UIUpdater();
    private static GeneticsSimulator genetic_simulator;
    private static Thread genetic_thread;

	// Other Variables for the UI: set one at a time
	private static boolean optimal = false;

    // Default values for simulation
	private static int DEFAULT_INITIALIZATION_CELL_RADIUS = 2;
	private static int DEFAULT_SIMULATION_LIFESPAN = 100;
	private static double DEFAULT_GENETICS_POPULATION = 200; // larger seems to do better
    private static int DEFAULT_GENETICS_GENERATIONS = 250;
	private static double DEFAULT_MUTATION_RATE = .15; // doesn't seem to matter too much

	private static CellBoard sim; // the board modified for displaying the simulation
	private static CellBoard initialBoard; // the preserved board for restoration on reset

	// @purpose: launch the application
	public static void main(String[] args) {
		launch(args);
	}

	// @purpose: initialize the cell data etc. for initial testing
	// @Guide: uncomment the desired section of code to use and comment the other sections
	@Override
	public void init() {
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
		CellBoard temp = new CellBoard(optimal5x5,2);
		sim = temp;
		initialBoard = sim.copy();
		fitnessLabel.setText("Fitness: "+Simulation.simulatedFitness(sim, DEFAULT_SIMULATION_LIFESPAN));
	}

	// @effect: Creating the environment for the GUI
	@Override
	public void start(final Stage stage) throws Exception {
		// Initialize window settings
		stage.setTitle("Conway's Game of Life + Genetics!");
		imageViewer.setFitHeight(HEIGHT);
		imageViewer.setFitWidth(WIDTH);

        // Create the environment for the display
        BorderPane main_layout = new BorderPane();
        Scene s = new Scene(main_layout);

        // Set up the Sidebar for parameter setting and running the genetic algorithm
        VBox parameter_bar = new VBox(SIDEBAR_SPACING);
        parameter_bar.setMinWidth(SIDEBAR_WIDTH);
        parameter_bar.getChildren().add(new Label("Cell Start Radius:"));
        HBox radius_slider = makeSlider(MIN_CELL_RADIUS, MAX_CELL_RADIUS,
            DEFAULT_INITIALIZATION_CELL_RADIUS, false);
        parameter_bar.getChildren().add(radius_slider);
        parameter_bar.getChildren().add(new Label("Simulation Lifespan:"));
        HBox sim_lifespan_slider = makeSlider(MIN_SIMULATION_LIFESPAN, MAX_SIMULATION_LIFESPAN,
            DEFAULT_SIMULATION_LIFESPAN, false);
        parameter_bar.getChildren().add(sim_lifespan_slider);
        parameter_bar.getChildren().add(new Label("Board Population Size:"));
        HBox board_pop_slider = makeSlider(MIN_GENETICS_POPULATION, MAX_GENETICS_POPULATION,
            DEFAULT_GENETICS_POPULATION, false);
        parameter_bar.getChildren().add(board_pop_slider);
        parameter_bar.getChildren().add(new Label("Mutation Rate:"));
        HBox mut_rate_slider = makeSlider(MIN_MUTATION_RATE, MAX_MUTATION_RATE,
            DEFAULT_MUTATION_RATE, true);
        parameter_bar.getChildren().add(mut_rate_slider);
        parameter_bar.getChildren().add(new Label("Number of Simulation Generations:"));
        HBox sim_gen_slider = makeSlider(MIN_GENETICS_GENERATIONS, MAX_GENETICS_GENERATIONS,
            DEFAULT_GENETICS_GENERATIONS, false);
        parameter_bar.getChildren().add(sim_gen_slider);

        // Setup the compute button to run the simulation and load the result on completion using these parameters
        Button compute_gen_best = new Button("Compute Genetics Best!");
        compute_gen_best.setOnAction((event) -> {
            // Read slider values
            int cell_radius = (int) ((Slider) (radius_slider.getChildren().get(0))).getValue();
            int sim_lifespan = (int) ((Slider) (sim_lifespan_slider.getChildren().get(0))).getValue();
            int board_pop_size = (int) ((Slider) (board_pop_slider.getChildren().get(0))).getValue();
            double mut_rate = ((Slider) (mut_rate_slider.getChildren().get(0))).getValue();
            int genetics_gens = (int) ((Slider) (sim_gen_slider.getChildren().get(0))).getValue();

            // Run the simulation on another thread
            if (genetic_simulator != null) { genetic_simulator.done(); }
            genetic_simulator = new GeneticsSimulator(cell_radius, sim_lifespan, board_pop_size, mut_rate,
                genetics_gens, new SimulationEvent() {
                        @Override
                        public void finished(CellBoard cell_board) {
                            Platform.runLater(() -> loadToSimulator(cell_board));
                        }

                        @Override
                        public void progress(double percentage) {
                            Platform.runLater(() -> setLoadingBar(percentage));
                        }
                    }
            );
            genetic_thread = new Thread(genetic_simulator);
            genetic_thread.start();
        });
        parameter_bar.getChildren().add(compute_gen_best);
        parameter_bar.setStyle("-fx-background-color: #DDDDDD");
        main_layout.setLeft(parameter_bar);

		// Setting up the MenuBar with ability to load a new file
		final FileChooser fileChooser = new FileChooser();

		MenuBar menuBar = new MenuBar();
		Menu menuFile = new Menu("File");
		MenuItem openFile = new MenuItem("Open File...     ");
		openFile.setOnAction(t -> {
            setupFilechooser(fileChooser);
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                CellBoard tmp = loadBoard(file);
                loadToSimulator(tmp);
            }
        });
		menuFile.getItems().add(openFile);
		menuBar.getMenus().add(menuFile);

        VBox vBox = new VBox();
        vBox.getChildren().add(menuBar);
        main_layout.setTop(vBox);

        // Set up the center panel
        VBox main = new VBox();
        StackPane overlay = new StackPane();
        overlay.getChildren().add(imageViewer);
        overlay.getChildren().add(fitnessLabel);
        overlay.setAlignment(Pos.TOP_LEFT);

        tray = new VBox();
        tray.setMinHeight(TRAY_HEIGHT);
        tray.setPrefHeight(TRAY_HEIGHT);
        tray.setStyle("-fx-background-color: #DDDDDD");

        main.getChildren().add(overlay);
        main.getChildren().add(tray);
        main_layout.setCenter(main); // The imageViewer is what displays the next image of the simulation

		stage.setScene(s);
		stage.show();
		displayNext(); // Display the initial image

		// acquire the UI semaphore so that the UI updater (for the simulation) can't start yet
		try {UI.acquire();} catch (InterruptedException e) {e.printStackTrace();}
		isPaused = true;

		updateThread.start();

		// This resets the display and pauses using semaphores to make sure the UI updater isn't working while the data is changed
		overlay.setOnMouseClicked(me -> {
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
        });
	}

    /**
     * Sets the bottom notification tray to have a progress bar with the given percentage. If it is at 100% also say
     *  that it is done.
     * @param percentage the percentage to show in the bar
     */
    public synchronized void setLoadingBar(double percentage) {
        ProgressBar bar;
        Label per;
        HBox tray_combo;

        if (tray.getChildren().size() == 0) {
            tray_combo = new HBox();
            bar = new ProgressBar(percentage);
            tray_combo.getChildren().add(bar);
            per = new Label(String.format(" %.0f%%", percentage*100));
            tray_combo.getChildren().add(per);
            tray.getChildren().add(tray_combo);
        }
        else {
            tray_combo = (HBox) tray.getChildren().get(0);

            bar = ((ProgressBar) tray_combo.getChildren().get(0));
            bar.setProgress(percentage);

            per = ((Label) tray_combo.getChildren().get(1));
            String tmp;
            if (percentage == 1) { tmp = "Done!"; }
            else { tmp = String.format(" %.0f%%", percentage*100); }
            per.setText(tmp);
        }

        per.setMinWidth(.25 * tray_combo.getWidth());
        bar.setMinWidth(.75 * tray_combo.getWidth());
    }

	// @effect: set up the file chooser window
	private static void setupFilechooser(final FileChooser fileChooser) {
		fileChooser.setTitle("Choose Initial Board");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("TXT", "*.txt")
				);
	}

    /**
     * Load the given CellBoard into the simulation.
     * @param cell_board the CellBoard that you would like to load
     */
    private synchronized void loadToSimulator(CellBoard cell_board) {
        // acquire the UI updater so that this is the only one running
        if (!isPaused) {
            try {UI.acquire();} catch (InterruptedException e) {e.printStackTrace();}
        }
        initialBoard = cell_board.copy();
        sim = cell_board.copy();
        fitnessLabel.setText("Fitness: "+Simulation.simulatedFitness(sim, DEFAULT_SIMULATION_LIFESPAN));
        displayNext();
        isPaused = true;
    }

	// @effect: load in the file and change the initial board to the one loaded from the file
	private synchronized CellBoard loadBoard(File file) {
		CellBoard cellBoard = new CellBoard();
		// Read in the file 
		try {
			Scanner scan = new Scanner(file);
			String temp; // stores the line before turning it into a point
			String[] tempArray;
			while(scan.hasNext()) {
				temp = scan.next();
                // TODO: 7/5/16 Update to not be affected by whitespace if it doesn't already
                tempArray = temp.split(",");
				cellBoard.addCell(new Point(Integer.parseInt(tempArray[0]), Integer.parseInt(tempArray[1])));
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return cellBoard;
	}

	// @effect: save the cell board to a file within a folder where the application resides; Note: file is deleted if it already exists
	private synchronized void saveBoard(CellBoard cellBoard, String name) {
		// Create the writer to save the board
		PrintWriter writer;
		try {
			writer = new PrintWriter(name+".txt", "UTF-8");
			// Save each point of the cellBoard into the file as a list of points with each point on it's own line
			HashSet<Point> cellPoints = cellBoard.getCells();
			for (Point p: cellPoints) {
				writer.println(p.x+","+p.y);
			}
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// @purpose: move the simulation forward one step and update the display to show the changes
	public static synchronized void displayNext() {
		// create the buffered image for drawing the next simulation image on
		BufferedImage bufImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D drawOn = bufImage.createGraphics();
		drawOn.setColor(BACKGROUND_COLOR);
		drawOn.fillRect(0, 0, WIDTH, HEIGHT);
		drawOn.setColor(CELL_COLOR);

		Point tempPoint;
        // TODO: 7/5/16 Flip the loop so it only looks at the cells that need drawing and not all the cells
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

    /**
     * @param min The min value of the slider
     * @param def The default value of the slider
     * @param max The max value of the slider
     * @param floating If the variable contains a floating point value
     * @return an HBox containing the slider with the given settings and a corresponding value label
     */
    public HBox makeSlider(double min, double max, double def, boolean floating) {
        Slider slider = new Slider(min, max, def);
        Label display_value;
        if (floating) {
            display_value = new Label(String.format("%.3f", slider.getValue()));
            slider.setMajorTickUnit(.001);
            slider.setBlockIncrement(.001);
        }
        else {
            display_value = new Label(String.format("%.0f", slider.getValue()));
            slider.setMajorTickUnit(1);
            slider.setBlockIncrement(1);
        }
        slider.valueProperty().addListener((observable, old_value, new_value) -> {
            if (floating) {
                display_value.setText(String.format("%.3f", new_value));
            }
            else {
                display_value.setText(String.format("%.0f", new_value));
            }
        });

        HBox container = new HBox();
        container.getChildren().add(slider);
        container.getChildren().add(display_value);
        return container;
    }
}