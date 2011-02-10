package com.amee.service.metadata;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.Metadata;
import com.amee.domain.ObjectType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class MetadataServiceDAOMock implements MetadataServiceDAO {

    @Override
    @SuppressWarnings(value = "unchecked")
    public Metadata getMetadataForEntity(IAMEEEntityReference entity, String name) {
        return null;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<Metadata> getMetadatas(IAMEEEntityReference entity) {
        return new ArrayList<Metadata>();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<Metadata> getMetadatas(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        return new ArrayList<Metadata>();
    }

    @Override
    public void persist(Metadata metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Metadata metadata) {
        throw new UnsupportedOperationException();
    }
}