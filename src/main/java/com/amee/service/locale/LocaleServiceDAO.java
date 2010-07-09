package com.amee.service.locale;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.data.LocaleName;

import java.util.Collection;
import java.util.List;

public interface LocaleServiceDAO {

    public List<LocaleName> getLocaleNames(IAMEEEntityReference entity);

    public List<LocaleName> getLocaleNames(ObjectType objectType, Collection<IAMEEEntityReference> entities);

    public void persist(LocaleName localeName);

    public void remove(LocaleName localeName);
}
