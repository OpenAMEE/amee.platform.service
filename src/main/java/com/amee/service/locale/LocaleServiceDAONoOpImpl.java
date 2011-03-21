package com.amee.service.locale;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.data.LocaleName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A no-op implementation of LocaleServiceDAO.
 */
public class LocaleServiceDAONoOpImpl implements LocaleServiceDAO {

    @Override
    public List<LocaleName> getLocaleNames(IAMEEEntityReference entity) {
        return new ArrayList<LocaleName>();
    }

    @Override
    public List<LocaleName> getLocaleNames(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        return new ArrayList<LocaleName>();
    }

    @Override
    public void persist(LocaleName localeName) {
        // Do nothing.
    }

    @Override
    public void remove(LocaleName localeName) {
        // Do nothing.
    }
}
