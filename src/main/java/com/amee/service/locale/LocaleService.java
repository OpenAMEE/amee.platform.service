package com.amee.service.locale;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ILocaleService;
import com.amee.domain.LocaleConstants;
import com.amee.domain.LocaleHolder;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
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

@Service
public class LocaleService implements ILocaleService, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private LocaleServiceDAO dao;

    // A thread bound Map of LocaleNames keyed by entity identity.
    private final ThreadLocal<Map<String, List<LocaleName>>> LOCALE_NAMES =
            new ThreadLocal<Map<String, List<LocaleName>>>() {
                protected Map<String, List<LocaleName>> initialValue() {
                    return new HashMap<String, List<LocaleName>>();
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
                    // Do nothing!
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
        List<LocaleName> localeNames;
        if (LOCALE_NAMES.get().containsKey(entity.toString())) {
            // Return existing LocaleName list, or at least an empty list.
            localeNames = LOCALE_NAMES.get().get(entity.toString());
            if (localeNames == null) {
                localeNames = new ArrayList<LocaleName>();
            }
        } else {
            // Look up LocaleNames.
            localeNames = dao.getLocaleNames(entity);
            LOCALE_NAMES.get().put(entity.toString(), localeNames);
        }
        return localeNames;
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
        loadLocaleNames(ObjectType.DC, new HashSet<IAMEEEntityReference>(dataCategories));
    }

    public void loadLocaleNamesForDataItems(Collection<DataItem> dataItems) {
        loadLocaleNamesForDataItems(dataItems, true);
    }

    public void loadLocaleNamesForDataItems(Collection<DataItem> dataItems, boolean values) {
        loadLocaleNames(ObjectType.DI, new HashSet<IAMEEEntityReference>(dataItems));
        if (values) {
            Set<IAMEEEntityReference> itemValueRefs = new HashSet<IAMEEEntityReference>();
            for (DataItem dataItem : dataItems) {
                itemValueRefs.addAll(dataItem.getItemValues());
            }
            loadLocaleNames(ObjectType.IV, itemValueRefs);
        }
    }

    public void loadLocaleNamesForItemValueDefinitions(Collection<ItemValueDefinition> itemValueDefinitions) {
        loadLocaleNames(ObjectType.IVD, new HashSet<IAMEEEntityReference>(itemValueDefinitions));
    }

    public void loadLocaleNames(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        // A null entry for when there are no LocaleNames for the entity.
        // Ensure a null entry exists for all entities.
        for (IAMEEEntityReference entity : entities) {
            if (!LOCALE_NAMES.get().containsKey(entity.toString())) {
                LOCALE_NAMES.get().put(entity.toString(), null);
            }
        }
        // Store LocaleNames against entities.
        // If there are no LocaleNames for an entity the entry will remain null.
        for (LocaleName localeName : dao.getLocaleNames(objectType, entities)) {
            List<LocaleName> localeNames = LOCALE_NAMES.get().get(localeName.getEntityReference().toString());
            if (localeNames == null) {
                localeNames = new ArrayList<LocaleName>();
            }
            localeNames.add(localeName);
            LOCALE_NAMES.get().put(localeName.getEntityReference().toString(), localeNames);
        }
    }

    public void clearLocaleNames() {
        LOCALE_NAMES.get().clear();
    }

    public void persist(LocaleName localeName) {
        LOCALE_NAMES.get().remove(localeName.getEntityReference().toString());
        dao.persist(localeName);
    }

    public void remove(LocaleName localeName) {
        LOCALE_NAMES.get().remove(localeName.getEntityReference().toString());
        dao.remove(localeName);
    }
}
