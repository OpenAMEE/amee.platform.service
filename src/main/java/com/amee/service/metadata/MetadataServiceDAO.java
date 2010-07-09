package com.amee.service.metadata;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.Metadata;
import com.amee.domain.ObjectType;

import java.util.Collection;
import java.util.List;

public interface MetadataServiceDAO {

    Metadata getMetadataForEntity(IAMEEEntityReference entity, String name);

    List<Metadata> getMetadatas(IAMEEEntityReference entity);

    List<Metadata> getMetadatas(ObjectType objectType, Collection<IAMEEEntityReference> entities);

    void persist(Metadata metadata);

    void remove(Metadata metadata);
}
