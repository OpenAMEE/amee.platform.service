package com.amee.service.metadata;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.Metadata;
import com.amee.domain.MetadataService;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.item.data.DataItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
public class MetadataServiceImpl implements MetadataService, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private MetadataServiceDAO dao;

    // A thread bound entity identity keyed Map of Metadata name keyed maps of Metadatas.
    private final ThreadLocal<Map<String, Map<String, Metadata>>> METADATAS =
            new ThreadLocal<Map<String, Map<String, Metadata>>>() {
                protected Map<String, Map<String, Metadata>> initialValue() {
                    return new HashMap<String, Map<String, Metadata>>();
                }
            };

    @Override
    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.trace("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound data.
                    clearMetadatas();
                    break;
                case END:
                    log.trace("onApplicationEvent() END");
                    // Reset thread bound data.
                    clearMetadatas();
                    break;
                default:
                    // Do nothing!
            }
        }
    }

    @Override
    public Metadata getMetadataForEntity(IAMEEEntityReference entity, String name) {
        return getMetadatas(entity).get(name);
    }

    private Map<String, Metadata> getMetadatas(IAMEEEntityReference entity) {
        Map<String, Metadata> metadatas;
        if (METADATAS.get().containsKey(entity.toString())) {
            // Return existing Metadata Map, or at least an empty Map.
            metadatas = METADATAS.get().get(entity.toString());
            if (metadatas == null) {
                metadatas = new HashMap<String, Metadata>();
            }
        } else {
            // Lookup Metadatas.
            metadatas = new HashMap<String, Metadata>();
            for (Metadata metadata : dao.getMetadatas(entity)) {
                metadatas.put(metadata.getName(), metadata);
            }
            METADATAS.get().put(entity.toString(), metadatas);
        }
        return metadatas;
    }

    @Override
    public void loadMetadatasForDataCategories(Collection<DataCategory> dataCategories) {
        loadMetadatas(ObjectType.DC, new HashSet<IAMEEEntityReference>(dataCategories));
    }

    @Override
    public void loadMetadatasForDataItems(Collection<DataItem> dataItems) {
        loadMetadatas(ObjectType.NDI, new HashSet<IAMEEEntityReference>(dataItems));
    }

    @Override
    public void loadMetadatasForItemDefinitions(Collection<ItemDefinition> itemDefinitions) {
        loadMetadatas(ObjectType.ID, new HashSet<IAMEEEntityReference>(itemDefinitions));
    }

    @Override
    public void loadMetadatasForItemValueDefinitions(Collection<ItemValueDefinition> itemValueDefinitions) {
        loadMetadatas(ObjectType.IVD, new HashSet<IAMEEEntityReference>(itemValueDefinitions));
    }

    @Override
    public void loadMetadatas(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        // A null entry for when there is no Metadata for the entity.
        // Ensure a null entry exists for all entities.
        for (IAMEEEntityReference entity : entities) {
            if (!METADATAS.get().containsKey(entity.toString())) {
                METADATAS.get().put(entity.toString(), null);
            }
        }
        // Store Metadata against entities.
        // If there is no Metadata for an entity the entry will remain null.
        for (Metadata metadata : dao.getMetadatas(objectType, entities)) {
            Map<String, Metadata> metadatas = METADATAS.get().get(metadata.getEntityReference().toString());
            if (metadatas == null) {
                metadatas = new HashMap<String, Metadata>();
            }
            metadatas.put(metadata.getName(), metadata);
            METADATAS.get().put(metadata.getEntityReference().toString(), metadatas);
        }
    }

    @Override
    public void clearMetadatas() {
        METADATAS.get().clear();
    }

    @Override
    public void persist(Metadata metadata) {
        METADATAS.get().remove(metadata.getEntityReference().toString());
        dao.persist(metadata);
    }

    @Override
    public void remove(Metadata metadata) {
        METADATAS.get().remove(metadata.getEntityReference().toString());
        dao.remove(metadata);
    }
}
