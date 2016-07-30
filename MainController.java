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
import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;

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

    // Tracking the parameters of the currently loaded board
    private static CellBoard initial_board; // the preserved board for restoration on reset and saving
    private static int cell_radius;
    private static int board_width, board_height;
    private static double milli_compute_time;
    private static int loaded_board_fitness;
    private static int sim_lifespan;
    private static int board_pop_size;
    private static int genetics_gens;
    private static int num_workers;
    private static boolean generated;
    private static double mutation_rate;
    private static int genetic_gens;

    protected UIUpdater sim_graphic_update_thread;

    protected static boolean optimal = false;

    protected CellBoard sim; // the board modified for displaying the simulation
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
        CellBoard tmp = new CellBoard(optimal5x5,2);
        sim = tmp;
        initial_board = sim.copy();
        generated = false;
        cell_radius = 2;
        board_width = 5;
        board_height = 5;
        sim_lifespan = 100; // TODO: 7/25/16 Is this correct? It may have actually been 250...
        milli_compute_time = -1; // TODO: 7/25/16 Update to be the time it took
        loaded_board_fitness = 6708;
        fitness_label.setText("Fitness: "+ loaded_board_fitness);

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

        // TODO: 7/25/16 Move these to style sheet/fxml where possible
        // Set the start sizes for the layout
        parameter_bar.setPrefWidth(Main.SIDEBAR_WIDTH);
        parameter_bar.setMaxWidth(Main.SIDEBAR_WIDTH);
        parameter_bar.setPrefHeight(Main.SCREEN_HEIGHT - Main.MENU_HEIGHT);
        parameter_bar.setMaxHeight(Main.SCREEN_HEIGHT - Main.MENU_HEIGHT);
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
                sim = initial_board;
                displayNext();
            }
        }
    }

    /**
     * Setup the compute button to run the simulation and load the result on completion using the slider parameters.
     */
    @FXML
    protected synchronized void computeGeneticsBest() {
        // Read slider values
        int cell_radius = (int) ((Slider) (radius_slider.getChildren().get(0))).getValue();
        int sim_lifespan = (int) ((Slider) (sim_lifespan_slider.getChildren().get(0))).getValue();
        int board_pop_size = (int) ((Slider) (board_pop_slider.getChildren().get(0))).getValue();
        double mut_rate = ((Slider) (mut_rate_slider.getChildren().get(0))).getValue();
        int genetics_gens = (int) ((Slider) (sim_gen_slider.getChildren().get(0))).getValue();
        int num_workers = (int) ((Slider) (num_threads_slider.getChildren().get(0))).getValue();

        // Run the simulation on another thread
        if (genetic_simulator != null) { genetic_simulator.done(); }
        genetic_simulator = new GeneticsSimulator(cell_radius, sim_lifespan, board_pop_size, mut_rate,
            genetics_gens, num_workers, new SimulationCallbacks() {
                    @Override
                    public void finished(CellBoard cell_board, int fitness, double milli_compute_time) {
                        Platform.runLater(() -> {
                            loadToSimulator(cell_board, fitness, cell_radius, true, milli_compute_time,
                                    sim_lifespan, board_pop_size, mut_rate, genetics_gens, num_workers);
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
     * Load the given CellBoard into the simulation and update the global stored parameters stored for the board to use
     *  when saving.
     * @param cell_board the CellBoard that you would like to load
     * @param fitness the fitness of the board for the simulation lifespan it was produced under
     * @param board_radius the cell radius of the initial board
     * @param generated whether or not this board was created using the genetic algorithm
     * @param milli_compute_time the millisecond time that it took to compute the genetic algorithm based board
     * @param sim_lifespan the lifetime that each individual fitness simulation was given
     * @param population_size the size of the pool of CellBoards used in the genetic algorithm
     * @param mutation_rate the rate of mutation in the CellBoards for the genetic algorithm
     * @param genetic_gens the number of rounds that the genetic algorithm was given to find a solution
     * @param number_threads the number of threads used to run the computation
     */
    protected synchronized void loadToSimulator(CellBoard cell_board, int fitness, int board_radius,
            boolean generated, double milli_compute_time, int sim_lifespan, int population_size,
            double mutation_rate, int genetic_gens, int number_threads) {

        this.cell_radius = board_radius;
        this.board_width = (board_radius * 2) + 1;
        this.board_height = (board_radius * 2) + 1;
        this.milli_compute_time = milli_compute_time;
        this.loaded_board_fitness = fitness;

        this.sim_lifespan = sim_lifespan;
        this.board_pop_size = population_size;
        this.mutation_rate = mutation_rate;
        this.genetics_gens = genetic_gens;
        this.generated = generated;
        this.genetic_gens = genetic_gens;
        this.num_workers = number_threads;

        synchronized (sim_graphic_update_thread) {
            if (!sim_graphic_update_thread.isPaused()) {
                sim_graphic_update_thread.pause();
            }
        }
        initial_board = cell_board.copy();
        sim = cell_board.copy();
        Main.setLabel(fitness_label,
                "Fitness: "+fitness);
        displayNext();
    }

    /**
     * Open up a file chooser and attempt to load the opened file into the simulation window
     */
    @FXML
    protected synchronized void openBoard() {
        File file = Main.showFileChooseDialog();

        if (file != null) {
            RandomAccessFile file_reader = null;

            try {
                file_reader = new RandomAccessFile(file, "rw");

                boolean generated = file_reader.readBoolean();
                int fitness = file_reader.readInt();
                double milli_compute_time = file_reader.readDouble();
                int num_workers = file_reader.readInt();

                // Leave some space in the file in case we want to add more info to the file format
                file_reader.readInt();
                file_reader.readInt();
                file_reader.readInt();

                int sim_lifespan = file_reader.readInt();
                int board_pop_size = file_reader.readInt();
                double mutation_rate = file_reader.readDouble();
                int genetic_gens = file_reader.readInt();

                int board_width = file_reader.readInt();
                int board_height = file_reader.readInt();

                int num_points = file_reader.readInt();

                CellBoard cell_board = new CellBoard();
                for (int i = 0; i < num_points; i++) {
                    int x = file_reader.readInt();
                    int y = file_reader.readInt();
                    cell_board.addCell(new Point(x, y));
                }

                if (file_reader.length() == file_reader.getFilePointer()) {
                    // TODO: 7/24/16 Update once we transition away from the radius variable to width and height
                    loadToSimulator(cell_board, fitness, (board_height - 1) / 2, generated, milli_compute_time,
                            sim_lifespan, board_pop_size, mutation_rate, genetic_gens, num_workers);
                }
                else {
                    Main.showError("File Loading Error!", "The file appears to be corrupted.");
                }
            } catch (FileNotFoundException e) {
                Main.showError("File Loading Error!", "Could not locate the given file.");
                if (Main.IS_DEBUG) { e.printStackTrace(); }
            } catch (IOException e) {
                Main.showError("File Loading Error!", "An error occurred while attempting to read from the file.");
                if (Main.IS_DEBUG) { e.printStackTrace(); }
            } catch (Exception e) {
                Main.showError("File Loading Error!", "An unknown error occurred while attempting to read the file.");
            } finally {
                if (file_reader != null) {
                    try {
                        file_reader.close();
                    } catch (IOException e) {
                        if (Main.IS_DEBUG) { e.printStackTrace(); }
                    }
                }
            }
        }
    }

    /**
     * Save the cell board to the desired save location with the chosen by the user in the file chooser dialog;
     *    Note: file is deleted if it already exists
     */
    @FXML
    protected synchronized void saveBoard() {
        File file = Main.showSaveFileChooseDialog();

        if (file != null) {
            file = (file.getPath().matches(".*\\.ejc")) ? file : new File(file.getPath() + ".ejc");

            RandomAccessFile file_writer = null;
            try {
                file_writer = new RandomAccessFile(file, "rw");

                file_writer.writeBoolean(generated);
                file_writer.writeInt(loaded_board_fitness);
                file_writer.writeDouble(milli_compute_time);
                file_writer.writeInt(num_workers);

                // Leave some space in the file in case we want to add more info to the file format at a later point
                file_writer.writeInt(0);
                file_writer.writeInt(0);
                file_writer.writeInt(0);

                file_writer.writeInt(sim_lifespan);
                file_writer.writeInt(board_pop_size);
                file_writer.writeDouble(mutation_rate);
                file_writer.writeInt(genetic_gens);

                file_writer.writeInt(board_width);
                file_writer.writeInt(board_height);

                HashSet<Point> cell_points = initial_board.getCells();
                file_writer.writeInt(cell_points.size());
                for (Point p: cell_points) {
                    file_writer.writeInt((int) p.getX());
                    file_writer.writeInt((int) p.getY());
                }
            } catch (FileNotFoundException e) {
                Main.showError("File Save Error!", "Could not locate the given file.");
                if (Main.IS_DEBUG) { e.printStackTrace(); }
            } catch (IOException e) {
                Main.showError("File Save Error!", "An issue occurred while writing to the file. The file has" +
                    " likely been left in a corrupted state.");
                if (Main.IS_DEBUG) { e.printStackTrace(); }
            } catch (Exception e) {
                Main.showError("File Save Error!", "An unknown error occurred while attempting to save the file. The " +
                    "file may have been left in a corrupted state.");
            } finally {

                if (file_writer != null) {
                    try {
                        file_writer.close();
                    } catch (IOException e) {
                        if (Main.IS_DEBUG) { e.printStackTrace(); }
                    }
                }
            }
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
                try {sim_graphic_update_thread.join();}
                catch (InterruptedException e) {
                    if (Main.IS_DEBUG) { e.printStackTrace(); }
                }
            }
        }
    }
}
