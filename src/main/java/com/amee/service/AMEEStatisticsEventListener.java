package com.amee.service;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.AMEEStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class AMEEStatisticsEventListener implements ApplicationListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AMEEStatistics ameeStatistics;

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.trace("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound statistics.
                    ameeStatistics.resetThread();
                    break;
                case COMMIT:
                    log.trace("onApplicationEvent() COMMIT");
                    // Update statistics.
                    ameeStatistics.commitThread();
                    break;
                case ROLLBACK:
                    log.trace("onApplicationEvent() ROLLBACK");
                    // Update statistics.
                    ameeStatistics.resetThread();
                    break;
                case END:
                    log.trace("onApplicationEvent() END - {calculationDuration=" + ameeStatistics.getThreadCalculationDuration() + "}");
                    break;
                default:
                    // Do nothing!
            }
        }
    }
}
