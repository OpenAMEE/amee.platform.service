package com.amee.service.locale;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ILocaleService;
import com.amee.domain.LocaleConstants;
import com.amee.domain.LocaleHolder;
import com.amee.domain.data.LocaleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LocaleService implements ILocaleService {

    @Autowired
    private LocaleServiceDAO dao;

    public Map<String, LocaleName> getLocaleNames(IAMEEEntityReference entity) {
        Map<String, LocaleName> localeNames = new HashMap<String, LocaleName>();
        for (LocaleName localeName : dao.getLocaleNames(entity)) {
            localeNames.put(localeName.getLocale(), localeName);
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

    public void persist(LocaleName localeName) {
        dao.persist(localeName);
    }

    public void remove(LocaleName localeName) {
        dao.remove(localeName);
    }
}
