package com.amee.service.metadata;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.IMetadataService;
import com.amee.domain.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataService implements IMetadataService {

    @Autowired
    MetadataServiceDAO dao;

    public Metadata getMetadataForEntity(IAMEEEntityReference entity, String name) {
        return dao.getMetadataForEntity(entity, name);
    }

    public void persist(Metadata metadata) {
        dao.persist(metadata);
    }

    public void remove(Metadata metadata) {
        dao.remove(metadata);
    }
}
