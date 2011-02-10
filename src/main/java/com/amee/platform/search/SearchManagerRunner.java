package com.amee.platform.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

public class SearchManagerRunner implements Runnable, SmartLifecycle {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private SearchManager searchManager;

    // A Thread to do the initialisation work in.
    private Thread thread;

    // A flag to indicate the thread should stop soon.
    private boolean stopping = false;

    @Override
    public void run() {
        searchManager.updateAll();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        // Start as late as possible.
        return Integer.MAX_VALUE;
    }

    @Override
    public synchronized void start() {
        log.info("start()");
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public synchronized void stop() {
        log.info("stop()");
        stopping = true;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public boolean isRunning() {
        return (thread != null) && (thread.isAlive());
    }
}

