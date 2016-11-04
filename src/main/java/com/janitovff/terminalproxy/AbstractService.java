package com.janitovff.terminalproxy;

public abstract class AbstractService {
    private boolean shouldStop = false;
    private boolean running = false;
    private Thread thread;

    public synchronized void start() {
        if (running)
            stop();

        shouldStop = false;
        running = true;

        thread = new Thread(threadEntryPoint);
        thread.start();
    }

    public synchronized void stop() {
        shouldStop = true;

        while (running)
            tryToStop();
    }

    public synchronized boolean isRunning() {
        return running;
    }

    protected void tryToStop() {
        try {
            thread.interrupt();
            thread.join();

            running = false;
            thread = null;
        } catch (InterruptedException cause) {
            System.err.println("Interrupted while stopping thread");
        }
    }

    protected boolean shouldStop() {
        return shouldStop;
    }

    private Runnable threadEntryPoint = new Runnable() {
        @Override
        public void run() {
            AbstractService.this.run();
        }
    };

    protected abstract void run();
}
