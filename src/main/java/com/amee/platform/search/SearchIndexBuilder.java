package com.amee.platform.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class SearchIndexBuilder implements Runnable {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private SearchService searchService;

    private Thread thread;
    private boolean stopping = false;
    private boolean indexDataCategories = false;
    private boolean indexDataItems = false;

    @PostConstruct
    public synchronized void start() {
        log.info("start()");
        thread = new Thread(this);
        thread.start();
    }

    @PreDestroy
    public synchronized void stop() {
        log.info("stop()");
        stopping = true;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void run() {
        log.info("run() Starting...");
        searchService.build(indexDataCategories, indexDataItems);
        log.info("run() ...done.");
    }

    @Value("#{ systemProperties['amee.indexDataCategories'] }")
    public void setIndexDataCategories(Boolean indexDataCategories) {
        this.indexDataCategories = indexDataCategories;
    }

    @Value("#{ systemProperties['amee.indexDataItems'] }")
    public void setIndexDataItems(Boolean indexDataItems) {
        this.indexDataItems = indexDataItems;
    }
}
