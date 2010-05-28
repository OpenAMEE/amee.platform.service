package com.amee.service.metadata;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.AMEEEntityReference;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.IMetadataService;
import com.amee.domain.Metadata;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValueDefinition;
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
import java.util.Set;
import java.util.WeakHashMap;

@Service
public class MetadataService implements IMetadataService, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private MetadataServiceDAO dao;

    // A thread bound IAMEEEntityReference keyed Map of Metadata name keyed maps of Metadatas.
    private final ThreadLocal<Map<IAMEEEntityReference, Map<String, Metadata>>> METADATAS =
            new ThreadLocal<Map<IAMEEEntityReference, Map<String, Metadata>>>() {
                protected Map<IAMEEEntityReference, Map<String, Metadata>> initialValue() {
                    return new WeakHashMap<IAMEEEntityReference, Map<String, Metadata>>();
                }
            };

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.debug("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound data.
                    clearMetadatas();
                    break;
                case END:
                    log.debug("onApplicationEvent() END");
                    // Reset thread bound data.
                    clearMetadatas();
                    break;
                default:
                    log.debug("onApplicationEvent() Unhandled TransactionEventType: " + te.getType());
            }
        }
    }

    public Metadata getMetadataForEntity(IAMEEEntityReference entity, String name) {
        return getMetadatas(entity).get(name);
    }

    private Map<String, Metadata> getMetadatas(IAMEEEntityReference entity) {
        if (METADATAS.get().containsKey(entity)) {
            return METADATAS.get().get(entity);
        } else {
            Map<String, Metadata> metadataMap = new HashMap<String, Metadata>();
            for (Metadata metadata : dao.getMetadatas(entity)) {
                metadataMap.put(metadata.getName(), metadata);
            }
            METADATAS.get().put(entity, metadataMap);
            return metadataMap;
        }
    }

    public void loadMetadatasForDataItems(Collection<DataItem> dataItems) {
        AMEEEntityReference entity;
        Set<IAMEEEntityReference> entities = new HashSet<IAMEEEntityReference>();
        for (DataItem dataItem : dataItems) {
            entity = new AMEEEntityReference(dataItem);
            entity.setEntity(null);
            entities.add(entity);
        }
        loadMetadatas(entities);
    }

    public void loadMetadatasForItemValueDefinitions(Collection<ItemValueDefinition> itemValueDefinitions) {
        AMEEEntityReference entity;
        Set<IAMEEEntityReference> entities = new HashSet<IAMEEEntityReference>();
        for (ItemValueDefinition itemValueDefinition : itemValueDefinitions) {
            entity = new AMEEEntityReference(itemValueDefinition);
            entity.setEntity(null);
            entities.add(entity);
        }
        loadMetadatas(entities);
    }

    public void loadMetadatas(Collection<IAMEEEntityReference> entities) {
        for (IAMEEEntityReference entity : entities) {
            if (!METADATAS.get().containsKey(entity)) {
                METADATAS.get().put(entity, new HashMap<String, Metadata>());
            }
        }
        for (Metadata metadata : dao.getMetadatas(entities)) {
            if (METADATAS.get().containsKey(metadata.getEntityReference())) {
                METADATAS.get().get(metadata.getEntityReference()).put(metadata.getName(), metadata);
            }
        }
    }

    public void clearMetadatas() {
        METADATAS.get().clear();
    }

    public void persist(Metadata metadata) {
        METADATAS.get().remove(metadata.getEntityReference());
        dao.persist(metadata);
    }

    public void remove(Metadata metadata) {
        METADATAS.get().remove(metadata.getEntityReference());
        dao.remove(metadata);
    }
}
