package com.amee.service.metadata;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.Metadata;
import com.amee.domain.ObjectType;

import java.util.Collection;
import java.util.List;

public interface MetadataServiceDAO {

    public Metadata getMetadataForEntity(IAMEEEntityReference entity, String name);

    public List<Metadata> getMetadatas(IAMEEEntityReference entity);

    public List<Metadata> getMetadatas(ObjectType objectType, Collection<IAMEEEntityReference> entities);

    public void persist(Metadata metadata);

    public void remove(Metadata metadata);
}
