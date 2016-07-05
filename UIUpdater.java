// Author: Sean Godard
// Purpose: used to update the display at regular intervals 

public class UIUpdater extends Thread {
	// Variables
	private static boolean DONE = false; // Let's the thread know to stop
	private static int FRAME_DELAY = 100; // How long to wait before updating the next frame

	@Override
	// @effect: regularly update the GUI as long as it is not paused
	public void run() {
		while (!DONE) {
			// protect access to this area with semaphore (competing with the event handler when this is to be reset)
			try {Main.UI.acquire();} catch (InterruptedException e1) {e1.printStackTrace();}
			Main.displayNext();
			Main.UI.release();
			try {Thread.sleep(FRAME_DELAY);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}

	// @effect: when called it sets the thread to stop updating the UI
	public void done() {
		DONE = true;
	}
}
