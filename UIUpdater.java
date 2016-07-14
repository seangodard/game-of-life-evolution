/**
 * Used to update the simulation display at regular intervals.
 * @author Sean Godard
 */

public class UIUpdater extends Thread {
	private boolean done = false;
    private boolean paused = true;
	private int FRAME_DELAY = 100; // How long to wait before updating the next frame (milliseconds)
    private UIUpdaterCallbacks callbacks;

	public UIUpdater(UIUpdaterCallbacks callbacks) {
        this.callbacks = callbacks;
    }

	@Override
    /**
     * Regularly update the GUI as long as it is not paused.
     */
	public void run() {
        super.run();

		while (!done) {
            if (paused) {
                try {
                    synchronized (this) {  wait();  }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            callbacks.showNext();
            try { this.sleep(FRAME_DELAY);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}

    /**
     * when called it sets the thread to stop updating the simulation.
     */
	public synchronized void done() { done = true; }

    /**
     * Let the thread know to pause its execution.
     */
    public synchronized void pause() { this.paused = true; }

    /**
     * Let the thread know to resume its execution.
     */
    public synchronized void cont() {
        this.paused = false;
        this.notify();
    }

    /**
     * @return If the thread is set to paused
     */
    public boolean isPaused() { return paused; }
}
