package com.amee.service;

import com.amee.base.transaction.TransactionEvent;
import com.amee.base.utils.ThreadBeanHolder;
import com.amee.domain.*;
import com.amee.domain.profile.CO2CalculationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * A service bean to expose other commonly used service beans via ThreadBeanHolder. The service beans
 * are set and then cleared at the start and end of each transaction.
 */
public class TransactionThreadBeanManager implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DataItemService dataItemService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private IDataService dataService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ProfileItemService profileItemService;

    @Autowired
    private CO2CalculationService calculationService;

    /**
     * Listens for TransactionEvents of type BEFORE_BEGIN and END to set commonly used service beans at
     * the beginning of a transaction and to clear them from the end of a transaction.
     *
     * @param e the ApplicationEvent
     */
    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.trace("onApplicationEvent() BEFORE_BEGIN");
                    // Clear the ThreadBeanHolder at the start of each transaction.
                    ThreadBeanHolder.clear();
                    // Store commonly used services.
                    ThreadBeanHolder.set(IDataService.class, dataService);
                    ThreadBeanHolder.set(DataItemService.class, dataItemService);
                    ThreadBeanHolder.set(LocaleService.class, localeService);
                    ThreadBeanHolder.set(MetadataService.class, metadataService);
                    ThreadBeanHolder.set(ProfileItemService.class, profileItemService);
                    ThreadBeanHolder.set(CO2CalculationService.class, calculationService);
                    break;
                case END:
                    log.trace("onApplicationEvent() END");
                    // Clear the ThreadBeanHolder at the end of each transaction.
                    ThreadBeanHolder.clear();
                    break;
                default:
                    // Do nothing!
            }
        }
    }
}
