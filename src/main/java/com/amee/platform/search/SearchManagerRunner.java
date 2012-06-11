package com.amee.platform.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class SearchManagerRunner implements Runnable, SmartLifecycle {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SearchManager searchManager;

    @Autowired
    @Qualifier("searchIndexerTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    // A Thread to do the initialisation work in.
    private Thread thread;

    // A flag to indicate the thread should stop soon.
    private boolean stopping = false;

    @Override
    public void run() {
        searchManager.updateAll();
        searchManager.updateLoop();
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
        // Remember that we're stopping...
        stopping = true;
        // Stop the thread.
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        // Shutdown the SearchIndexer thread pool.
        taskExecutor.shutdown();
    }

    @Override
    public boolean isRunning() {
        return (thread != null) && (thread.isAlive());
    }
}

