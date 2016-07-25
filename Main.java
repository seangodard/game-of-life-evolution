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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main extends Application {
    // Set to true to turn on debug messages such as stack trace
    protected static final boolean IS_DEBUG = true;

	protected static Stage stage;

    protected static FXMLLoader loader;
    protected static MainController main_controller = new MainController();

    private static int CELL_SIZE = 4, NUM_CELLS_DISPLAY = 161; // display number is odd so that center axis fits nicely
    protected static int SIM_WIDTH = CELL_SIZE * NUM_CELLS_DISPLAY, SIM_HEIGHT = CELL_SIZE * NUM_CELLS_DISPLAY;

    private static Color CELL_COLOR = new Color(0, 160,0);
    private static Color BACKGROUND_COLOR = new Color(248, 248, 248);
    protected static int SIDEBAR_WIDTH = 250;
    protected static int SIDEBAR_PADDING = 14;
    protected static int TRAY_HEIGHT = 20;
    protected static int MENU_HEIGHT = 30;

    protected static int SCREEN_HEIGHT = SIM_HEIGHT + TRAY_HEIGHT + MENU_HEIGHT;
    protected static int SCREEN_WIDTH = SIDEBAR_WIDTH + SIM_WIDTH;

    /**
     * launch the application
     * @param args
     */
	public static void main(String[] args) {
		launch(args);
	}

    /**
     * Creating the environment for the GUI
     * @param stage
     * @throws Exception
     */
	@Override
	public void start(final Stage stage) throws Exception {
		this.stage = stage;

		// Initialize window settings
		this.stage.setTitle("Conway's Game of Life + Genetics!");

        this.setUserAgentStylesheet("Main.css");

        // Create the environment for the display with FXML and initialize the main_controller for the UI
		loader = new FXMLLoader();
        Parent root = loader.load(Main.class.getResource("Main.fxml"));

        // Setup the main controller with its callback functions
		loader.setController(main_controller);

        Scene s = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
		this.stage.setScene(s);
		this.stage.show();
    }

    /**
     * Displays an error message pop-up box to the user.
     * @param title the title of the error message box
     * @param body the body of the error message box
     */
    protected synchronized static void showError(String title, String body) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(body);
        alert.showAndWait();
    }

    /**
     * Display a file chooser dialog for selecting a file to load.
     * @return the file that was selected
     */
    protected static File showFileChooseDialog() {
        FileChooser file_chooser = new FileChooser();

        file_chooser.setTitle("Choose Game of Life Board");
        file_chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        file_chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("EJC", "*.ejc")
        );

        return file_chooser.showOpenDialog(stage);
    }

    /**
     * Display a window for a user to name and specify where to save the current game of life board.
     * @return the filepath and name of where the user would like to save to
     */
    protected static File showSaveFileChooseDialog() {
        FileChooser file_chooser = new FileChooser();

        file_chooser.setTitle("Save Game of Life Board");
        file_chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        file_chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("EJC", "*.ejc")
        );

        return file_chooser.showSaveDialog(stage);
    }

    /**
     * Sets the given notification tray to have a progress bar with the given percentage.
     * @param percentage the percentage to show in the tray
     */
    protected static synchronized void setLoadingBar(VBox tray, double percentage) {
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
            if (percentage == 1) { tmp = " Done!"; }
            else { tmp = String.format(" %.0f%%", percentage*100); }
            per.setText(tmp);
        }

        per.setMinWidth(.25 * tray_combo.getWidth());
        bar.setMinWidth(.75 * tray_combo.getWidth());
    }

    /**
     * Setup the slider with a listener that updates the visible value when the slider changes.
     * @param slider_container The HBox containing the slider and slider label
     * @param floating If the slider contains a floating point value
     */
    protected static void initializeSlider(HBox slider_container, boolean floating) {
        assert(slider_container.getChildren().get(0) instanceof Slider);
        assert(slider_container.getChildren().get(1) instanceof Label);

        Slider slider = (Slider) slider_container.getChildren().get(0);
        Label slider_label = (Label) slider_container.getChildren().get(1);

        slider.valueProperty().addListener((observable, old_value, new_value) -> {
            if (floating) {
                slider_label.setText(String.format("%.3f", new_value));
            }
            else {
                slider_label.setText(String.format("%.0f", new_value));
            }
        });
    }

    /**
     * Sets the given label to have the given text.
     * @param label
     * @param text
     */
    protected static void setLabel(Label label, String text) {
        label.setText(text);
    }

    /**
     * Set the simulation image for the given image view an image modelled after the cell board.
     * @param img_view
     * @param sim
     */
    protected static void setSimulationImage(ImageView img_view, CellBoard sim) {
        // create the buffered image for drawing the next simulation image on
        BufferedImage bufImage = new BufferedImage(SIM_WIDTH, SIM_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D drawOn = bufImage.createGraphics();
        drawOn.setColor(BACKGROUND_COLOR);
        drawOn.fillRect(0, 0, SIM_WIDTH, SIM_HEIGHT);
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
                    // Translate to standard xy coordinates to the way that java does coordinates
                    drawOn.fillRect(x*CELL_SIZE+winRadius*CELL_SIZE, -y*CELL_SIZE+winRadius*CELL_SIZE, CELL_SIZE,
                        CELL_SIZE);
                }
            }
        }

        // Convert the buffered image to an image and display it in the simulation_image node
        WritableImage img = new WritableImage(bufImage.getWidth(), bufImage.getHeight());
        SwingFXUtils.toFXImage(bufImage, img);
        img_view.setImage(img);
    }

    /**
	 * Called when the application is closed, in this case calls on the main main_controller to stop anything it might
     * 	have been doing.
	 */
	@Override
	public void stop() {
        try { super.stop(); } catch (Exception e) { if (Main.IS_DEBUG) { e.printStackTrace(); } }
        main_controller.stop();
        Platform.exit();
        System.exit(0);
    }
}