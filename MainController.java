import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Scanner;

/**
 * A class for controlling the main application view.
 * @author Sean Godard
 */
public class MainController implements Initializable {
    // Injecting the FXML objects
    @FXML protected HBox radius_slider;
    @FXML protected HBox sim_lifespan_slider;
    @FXML protected HBox board_pop_slider;
    @FXML protected HBox mut_rate_slider;
    @FXML protected HBox sim_gen_slider;
    @FXML protected HBox num_threads_slider;
    @FXML protected ImageView simulation_image;
    @FXML protected VBox simulation_shadow;
    @FXML protected VBox tray;
    @FXML protected Label fitness_label;
    @FXML protected VBox parameter_bar;
    @FXML protected BorderPane main_layout;
    @FXML protected StackPane overlay;
    @FXML protected VBox main;
    @FXML protected MenuBar top_menu;

    protected UIUpdater sim_graphic_update_thread;

    protected static boolean optimal = false;

    protected static int DEFAULT_SIMULATION_LIFESPAN = 100;

    protected CellBoard sim; // the board modified for displaying the simulation
    protected CellBoard initialBoard; // the preserved board for restoration on reset
    protected GeneticsSimulator genetic_simulator;
    protected Thread genetic_thread;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the board to the 5x5 board
        Point[] optimal5x5Points = {new Point(-2,-2), new Point(-2,0), new Point(-2,2), new Point(0,-2), new Point(0,2),
                new Point(2,-2), new Point(2,0), new Point(-2,-1), new Point(-2,1), new Point(0,1), new Point(2,-1),
                new Point(-1,1), new Point(1,1), new Point(-1,0), new Point(-1,2)};
        HashSet<Point> optimal5x5 = new HashSet<>();
        for (Point point : optimal5x5Points) {
            optimal5x5.add(point);
        }
        CellBoard temp = new CellBoard(optimal5x5,2);
        sim = temp;
        initialBoard = sim.copy();
        fitness_label.setText("Fitness: 6708");

        // Set up the slider label values to update when slid
        Main.initializeSlider(radius_slider, false);
        Main.initializeSlider(sim_lifespan_slider, false);
        Main.initializeSlider(board_pop_slider, false);
        Main.initializeSlider(mut_rate_slider, true);
        Main.initializeSlider(sim_gen_slider, false);
        Main.initializeSlider(num_threads_slider, false);
        // Set up the thread slider
        Slider thread_slider = ((Slider) num_threads_slider.getChildren().get(0));
        thread_slider.setMin(1);
        int max_threads = Runtime.getRuntime().availableProcessors();
        thread_slider.setMax(max_threads);
        thread_slider.setValue(max_threads);
        ((Label) num_threads_slider.getChildren().get(1)).setText(""+max_threads);

        // Set the start sizes for the layout
        parameter_bar.setPrefWidth(Main.SIDEBAR_WIDTH);
        parameter_bar.setMaxWidth(Main.SIDEBAR_WIDTH);
        parameter_bar.setPrefHeight(Main.SCREEN_HEIGHT - Main.MENU_HEIGHT);
        parameter_bar.setMaxHeight(Main.SCREEN_HEIGHT - Main.MENU_HEIGHT);
        for (Node child : parameter_bar.getChildren()) {
            if (child instanceof VBox) {
                ((VBox) child).setPadding(new javafx.geometry.Insets(Main.SIDEBAR_PADDING, Main.SIDEBAR_PADDING,
                        Main.SIDEBAR_PADDING, Main.SIDEBAR_PADDING));
                ((VBox) child).setAlignment(Pos.TOP_CENTER);
            }
            else if (child instanceof Button) {
                ((Button) child).setPadding(new javafx.geometry.Insets(Main.SIDEBAR_PADDING, Main.SIDEBAR_PADDING,
                        Main.SIDEBAR_PADDING, Main.SIDEBAR_PADDING));
            }
        }
        top_menu.setPrefWidth(Main.SCREEN_WIDTH);
        top_menu.setMaxWidth(Main.SCREEN_WIDTH);
        top_menu.setPrefHeight(Main.MENU_HEIGHT);
        top_menu.setMaxHeight(Main.MENU_HEIGHT);
        tray.setPrefHeight(Main.TRAY_HEIGHT);
        tray.setMaxHeight(Main.TRAY_HEIGHT);
        overlay.setPrefHeight(Main.SIM_HEIGHT);
        overlay.setMaxHeight(Main.SIM_HEIGHT);
        overlay.setPrefWidth(Main.SIM_WIDTH);
        overlay.setMaxWidth(Main.SIM_WIDTH);
        simulation_shadow.setPrefHeight(Main.SIM_HEIGHT);
        simulation_shadow.setMaxHeight(Main.SIM_HEIGHT);
        simulation_shadow.setPrefWidth(Main.SIM_WIDTH);
        simulation_shadow.setMaxWidth(Main.SIM_WIDTH);
        main.setPrefHeight(Main.SIM_HEIGHT+Main.TRAY_HEIGHT);
        main.setMaxHeight(Main.SIM_HEIGHT+Main.TRAY_HEIGHT);
        main.setPrefWidth(Main.SIM_WIDTH);
        main.setMaxWidth(Main.SIM_WIDTH);
        main_layout.setAlignment(main_layout.getCenter(), Pos.TOP_LEFT);

        // Setup the updater thread
        sim_graphic_update_thread = new UIUpdater(new UIUpdaterCallbacks() {
            @Override
            public void showNext() {
                displayNext();
            }
        });
        sim_graphic_update_thread.start();

        displayNext();
    }

    /**
     * Handle clicking events by stopping and starting the simulation from the beginning on click.
     */
    @FXML
    protected void simulationClicked() {
        synchronized (sim_graphic_update_thread) {
            if (sim_graphic_update_thread.isPaused()) {
                sim_graphic_update_thread.cont();
            }
            else {
                sim_graphic_update_thread.pause();
                sim = initialBoard;
                displayNext();
            }
        }
    }

    /**
     * Setup the compute button to run the simulation and load the result on completion using the slider parameters.
     */
    @FXML
    protected void computeGeneticsBest() {
        // Read slider values
        int cell_radius = (int) ((Slider) (radius_slider.getChildren().get(0))).getValue();
        int sim_lifespan = (int) ((Slider) (sim_lifespan_slider.getChildren().get(0))).getValue();
        int board_pop_size = (int) ((Slider) (board_pop_slider.getChildren().get(0))).getValue();
        double mut_rate = ((Slider) (mut_rate_slider.getChildren().get(0))).getValue();
        int genetics_gens = (int) ((Slider) (sim_gen_slider.getChildren().get(0))).getValue();
        // TODO: 7/14/16 Update to retrieve from sliders
        int num_workers = 4;

        // Run the simulation on another thread
        if (genetic_simulator != null) { genetic_simulator.done(); }
        genetic_simulator = new GeneticsSimulator(cell_radius, sim_lifespan, board_pop_size, mut_rate,
            genetics_gens, num_workers, new SimulationCallbacks() {
                    @Override
                    public void finished(CellBoard cell_board, int fitness) {
                        Platform.runLater(() -> {
                            loadToSimulator(cell_board, fitness);
                        });
                    }

                    @Override
                    public void progress(double percentage) {
                        Platform.runLater(() -> {
                            Main.setLoadingBar(tray, percentage);
                        });
                    }
                }
        );
        genetic_thread = new Thread(genetic_simulator);
        genetic_thread.setName("Genetic Simulator");
        genetic_thread.start();
    }

    /**
     * Load the given CellBoard into the simulation.
     * @param cell_board the CellBoard that you would like to load
     * @param fitness the fitness of the board for the simulation lifespan it was produced under
     */
    protected synchronized void loadToSimulator(CellBoard cell_board, int fitness) {
        synchronized (sim_graphic_update_thread) {
            if (!sim_graphic_update_thread.isPaused()) {
                sim_graphic_update_thread.pause();
            }
        }
        initialBoard = cell_board.copy();
        sim = cell_board.copy();
        Main.setLabel(fitness_label,
                "Fitness: "+fitness);
        displayNext();
    }

    /**
     * Open up a file chooser and attempt to load the opened file as a CellBoard file.
     */
    @FXML
    protected synchronized void openFile() {
        File file = Main.showFileChooseDialog();

        if (file != null) {
            CellBoard tmp = loadBoard(file);
            // TODO: 7/13/16 Update to be the file fitness
            loadToSimulator(tmp, 0);
        }
    }

    // @effect: load in the file and change the initial board to the one loaded from the file
    protected synchronized CellBoard loadBoard(File file) {
        CellBoard cellBoard = new CellBoard();
        // Read in the file
        try {
            Scanner scan = new Scanner(file);
            String temp; // stores the line before turning it into a point
            String[] tempArray;
            while(scan.hasNext()) {
                temp = scan.next();
                // TODO: 7/5/16 Update to not be affected by whitespace if it isn't already
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
    protected synchronized void saveBoard(CellBoard cellBoard, String name) {
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

    /**
     * Move the simulation forward one step and update the display to show the changes.
     */
    protected void displayNext() {
        Main.setSimulationImage(simulation_image, sim);
        sim = Simulation.updateCells(sim).getBoard();
    }

    /**
     * When asked, end the update thread in preparation to exit.
     */
    protected void stop() {
        if (sim_graphic_update_thread != null) {
            synchronized (sim_graphic_update_thread) {
                if (sim_graphic_update_thread.isPaused()) {
                    sim_graphic_update_thread.cont();
                }
                sim_graphic_update_thread.done();
                try {sim_graphic_update_thread.join();} catch (InterruptedException e) {e.printStackTrace();}
            }
        }
    }
}
