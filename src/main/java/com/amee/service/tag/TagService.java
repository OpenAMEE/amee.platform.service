package com.amee.service.tag;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.tag.EntityTag;
import com.amee.domain.tag.Tag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TagService implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private TagServiceDAO dao;

    // A thread bound Map of EntityTags keyed by entity identity.
    private final ThreadLocal<Map<String, List<EntityTag>>> ENTITY_TAGS =
            new ThreadLocal<Map<String, List<EntityTag>>>() {
                protected Map<String, List<EntityTag>> initialValue() {
                    return new HashMap<String, List<EntityTag>>();
                }
            };

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.debug("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound data.
                    clearEntityTags();
                    break;
                case END:
                    log.debug("onApplicationEvent() END");
                    // Reset thread bound data.
                    clearEntityTags();
                    break;
                default:
                    // Do nothing!
            }
        }
    }

    public List<Tag> getAllTags() {
        return dao.getTagsWithCount();
    }

    public List<Tag> getTags(IAMEEEntityReference entity) {
        List<EntityTag> entityTags;
        List<Tag> tags = new ArrayList<Tag>();
        if (entity != null) {
            if (ENTITY_TAGS.get().containsKey(entity.toString())) {
                // Return existing Tag list, or at least an empty list.
                entityTags = ENTITY_TAGS.get().get(entity.toString());
                if (entityTags != null) {
                    for (EntityTag entityTag : entityTags) {
                        tags.add(entityTag.getTag());
                    }
                }
            } else {
                // Look up EntityTags.
                entityTags = dao.getEntityTags(entity);
                ENTITY_TAGS.get().put(entity.toString(), entityTags);
                // Populate Tag list.
                for (EntityTag entityTag : entityTags) {
                    tags.add(entityTag.getTag());
                }
            }
        } else {
            tags.addAll(getAllTags());
        }
        return tags;
    }

    public String getTagsCSV(IAMEEEntityReference entity) {
        String tags = "";
        for (Tag tag : getTags(entity)) {
            tags = tags + tag.getTag() + ", ";
        }
        StringUtils.chomp(tags, ", ");
        return tags;
    }

    public Tag getTag(String tag) {
        return dao.getTag(tag);
    }

    public EntityTag getEntityTag(IAMEEEntityReference entity, String tag) {
        return dao.getEntityTag(entity, tag);
    }

    public void loadEntityTagsForDataCategories(Collection<DataCategory> dataCategories) {
        loadEntityTags(ObjectType.DC, new HashSet<IAMEEEntityReference>(dataCategories));
    }

    public void loadEntityTags(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        // A null entry for when there are no Tags for the entity.
        // Ensure a null entry exists for all entities.
        for (IAMEEEntityReference entity : entities) {
            if (!ENTITY_TAGS.get().containsKey(entity.toString())) {
                ENTITY_TAGS.get().put(entity.toString(), null);
            }
        }
        // Store Tags against entities.
        // If there are no Tags for an entity the entry will remain null.
        for (EntityTag entityTag : dao.getEntityTags(objectType, entities)) {
            List<EntityTag> entityTags = ENTITY_TAGS.get().get(entityTag.getEntityReference().toString());
            if (entityTags == null) {
                entityTags = new ArrayList<EntityTag>();
            }
            entityTags.add(entityTag);
            ENTITY_TAGS.get().put(entityTag.getEntityReference().toString(), entityTags);
        }
    }

    public void clearEntityTags() {
        ENTITY_TAGS.get().clear();
    }

    public void persist(Tag tag) {
        clearEntityTags();
        dao.persist(tag);
    }

    public void remove(Tag tag) {
        dao.remove(tag);
    }

    public void persist(EntityTag entityTag) {
        ENTITY_TAGS.get().remove(entityTag.getEntityReference().toString());
        dao.persist(entityTag);
    }

    public void remove(EntityTag entityTag) {
        ENTITY_TAGS.get().remove(entityTag.getEntityReference().toString());
        dao.remove(entityTag);
    }
}