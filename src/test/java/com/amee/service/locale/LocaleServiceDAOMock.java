package com.amee.service.locale;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.data.LocaleName;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class LocaleServiceDAOMock implements LocaleServiceDAO {

    @SuppressWarnings(value = "unchecked")
    public List<LocaleName> getLocaleNames(IAMEEEntityReference entity) {
        return new ArrayList<LocaleName>();
    }

    @SuppressWarnings(value = "unchecked")
    public List<LocaleName> getLocaleNames(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        return new ArrayList<LocaleName>();
    }

    public void persist(LocaleName localeName) {
        throw new UnsupportedOperationException();
    }

    public void remove(LocaleName localeName) {
        throw new UnsupportedOperationException();
    }
}