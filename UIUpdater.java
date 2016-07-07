/**
 * Used to update the simulation display at regular intervals.
 * @author Sean Godard
 */

// TODO: 7/6/16 Fix this so that all the graphics run on the main thread and use callbacks
public class UIUpdater extends Thread {
	private static boolean DONE = false;
	private static int FRAME_DELAY = 100; // How long to wait before updating the next frame

	@Override
    /**
     * Regularly update the GUI as long as it is not paused.
     */
	public void run() {
		while (!DONE) {
			// protect access to this area with semaphore (competing with the event handler when this is to be reset)
			try {Main.UI.acquire();} catch (InterruptedException e1) {e1.printStackTrace();}
			Main.displayNext();
			Main.UI.release();
			try {Thread.sleep(FRAME_DELAY);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}

    /**
     * when called it sets the thread to stop updating the simulation.
     */
	public synchronized void done() { DONE = true; }
}
