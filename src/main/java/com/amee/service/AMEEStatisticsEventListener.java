package com.amee.service;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.AMEEStatistics;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class AMEEStatisticsEventListener implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private AMEEStatistics ameeStatistics;

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.debug("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound statistics.
                    ameeStatistics.resetThread();
                    break;
                case COMMIT:
                    log.debug("onApplicationEvent() COMMIT");
                    // Update statistics.
                    ameeStatistics.commitThread();
                    break;
                case ROLLBACK:
                    log.debug("onApplicationEvent() ROLLBACK");
                    // Update statistics.
                    ameeStatistics.resetThread();
                    break;
                case END:
                    log.debug("onApplicationEvent() END - {calculationDuration=" + ameeStatistics.getThreadCalculationDuration() + "}");
                    break;
                default:
                    // Do nothing!
            }
        }
    }
}