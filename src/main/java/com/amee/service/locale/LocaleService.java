package com.amee.service.locale;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.AMEEEntityReference;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ILocaleService;
import com.amee.domain.LocaleConstants;
import com.amee.domain.LocaleHolder;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValue;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.LocaleName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Service
public class LocaleService implements ILocaleService, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private LocaleServiceDAO dao;

    // A thread bound Map of LocaleNames keyed by IAMEEEntityReferences.
    private final ThreadLocal<Map<IAMEEEntityReference, List<LocaleName>>> LOCALE_NAMES =
            new ThreadLocal<Map<IAMEEEntityReference, List<LocaleName>>>() {
                protected Map<IAMEEEntityReference, List<LocaleName>> initialValue() {
                    return new WeakHashMap<IAMEEEntityReference, List<LocaleName>>();
                }
            };

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.debug("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound data.
                    clearLocaleNames();
                    break;
                case END:
                    log.debug("onApplicationEvent() END");
                    // Reset thread bound data.
                    clearLocaleNames();
                    break;
                default:
                    log.debug("onApplicationEvent() Unhandled TransactionEventType: " + te.getType());
            }
        }
    }

    public Map<String, LocaleName> getLocaleNames(IAMEEEntityReference entity) {
        Map<String, LocaleName> localeNames = new HashMap<String, LocaleName>();
        for (LocaleName localeName : getLocaleNameList(entity)) {
            localeNames.put(localeName.getLocale(), localeName);
        }
        return localeNames;
    }

    private List<LocaleName> getLocaleNameList(IAMEEEntityReference entity) {
        if (LOCALE_NAMES.get().containsKey(entity)) {
            return LOCALE_NAMES.get().get(entity);
        } else {
            List<LocaleName> localeNameList = dao.getLocaleNames(entity);
            LOCALE_NAMES.get().put(entity, localeNameList);
            return localeNameList;
        }
    }

    public LocaleName getLocaleName(IAMEEEntityReference entity) {
        return getLocaleName(entity, LocaleHolder.getLocale());
    }

    public LocaleName getLocaleName(IAMEEEntityReference entity, String locale) {
        return getLocaleNames(entity).get(LocaleHolder.getLocale());
    }

    public String getLocaleNameValue(IAMEEEntityReference entity) {
        return getLocaleNameValue(entity, null);
    }

    public String getLocaleNameValue(IAMEEEntityReference entity, String defaultName) {
        LocaleName localeName = getLocaleName(entity);
        if (localeName != null) {
            return localeName.getName();
        } else {
            return defaultName;
        }
    }

    public void clearLocaleName(IAMEEEntityReference entity, String locale) {
        // Is there an existing LocaleName?
        LocaleName localeName = getLocaleName(entity, locale);
        if (localeName != null) {
            remove(localeName);
        }
    }

    public void setLocaleName(IAMEEEntityReference entity, String locale, String name) {
        // Is there an existing LocaleName?
        LocaleName localeName = getLocaleName(entity, locale);
        if (localeName != null) {
            // Update existing LocaleName.
            localeName.setName(name);
        } else {
            // Create new LocaleName.
            localeName = new LocaleName(
                    entity,
                    LocaleConstants.AVAILABLE_LOCALES.get(locale),
                    name);
            persist(localeName);
        }
    }

    public void loadLocaleNamesForDataCategories(Collection<DataCategory> dataCategories) {
        AMEEEntityReference entity;
        Set<IAMEEEntityReference> entities = new HashSet<IAMEEEntityReference>();
        for (DataCategory dataCategory : dataCategories) {
            entity = new AMEEEntityReference(dataCategory);
            entity.setEntity(null);
            entities.add(entity);
        }
        loadLocaleNames(entities);
    }

    public void loadLocaleNamesForDataItems(Collection<DataItem> dataItems) {
        loadLocaleNamesForDataItems(dataItems, true);
    }

    public void loadLocaleNamesForDataItems(Collection<DataItem> dataItems, boolean values) {
        AMEEEntityReference entity;
        Set<IAMEEEntityReference> entities = new HashSet<IAMEEEntityReference>();
        for (DataItem dataItem : dataItems) {
            entity = new AMEEEntityReference(dataItem);
            entity.setEntity(null);
            entities.add(entity);
            if (values) {
                for (ItemValue itemValue : dataItem.getItemValues()) {
                    entity = new AMEEEntityReference(itemValue);
                    entity.setEntity(null);
                    entities.add(entity);
                }
            }
        }
        loadLocaleNames(entities);
    }

    public void loadLocaleNamesForItemValueDefinitions(Collection<ItemValueDefinition> itemValueDefinitions) {
        AMEEEntityReference entity;
        Set<IAMEEEntityReference> entities = new HashSet<IAMEEEntityReference>();
        for (ItemValueDefinition itemValueDefinition : itemValueDefinitions) {
            entity = new AMEEEntityReference(itemValueDefinition);
            entity.setEntity(null);
            entities.add(entity);
        }
        loadLocaleNames(entities);
    }

    public void loadLocaleNames(Collection<IAMEEEntityReference> entities) {
        for (IAMEEEntityReference entity : entities) {
            if (!LOCALE_NAMES.get().containsKey(entity)) {
                LOCALE_NAMES.get().put(entity, new ArrayList<LocaleName>());
            }
        }
        for (LocaleName localeName : dao.getLocaleNames(entities)) {
            if (LOCALE_NAMES.get().containsKey(localeName.getEntityReference())) {
                LOCALE_NAMES.get().get(localeName.getEntityReference()).add(localeName);
            }
        }
    }

    public void clearLocaleNames() {
        LOCALE_NAMES.get().clear();
    }

    public void persist(LocaleName localeName) {
        LOCALE_NAMES.get().remove(localeName.getEntityReference());
        dao.persist(localeName);
    }

    public void remove(LocaleName localeName) {
        LOCALE_NAMES.get().remove(localeName.getEntityReference());
        dao.remove(localeName);
    }
}
