package com.amee.service.tag;

import com.amee.base.transaction.TransactionEvent;
import com.amee.base.utils.UidGen;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.tag.EntityTag;
import com.amee.domain.tag.Tag;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TagService implements ApplicationListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TagServiceDAO dao;

    // A thread bound Map of EntityTags keyed by entity identity.
    private final ThreadLocal<Map<String, List<EntityTag>>> ENTITY_TAGS =
            new ThreadLocal<Map<String, List<EntityTag>>>() {
                protected Map<String, List<EntityTag>> initialValue() {
                    return new HashMap<String, List<EntityTag>>();
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
                    clearEntityTags();
                    break;
                case END:
                    log.trace("onApplicationEvent() END");
                    // Reset thread bound data.
                    clearEntityTags();
                    break;
                default:
                    // Do nothing!
            }
        }
    }

    public List<Tag> getTags(IAMEEEntityReference entity) {
        List<EntityTag> entityTags;
        List<Tag> tags = new ArrayList<Tag>();
        if (ENTITY_TAGS.get().containsKey(entity.toString())) {
            // Return cached Tag list, or at least an empty list.
            entityTags = ENTITY_TAGS.get().get(entity.toString());
            if (entityTags != null) {
                for (EntityTag entityTag : entityTags) {
                    tags.add(entityTag.getTag());
                }
            }
        } else {
            // Look up EntityTags for entity.
            entityTags = dao.getEntityTags(entity);
            ENTITY_TAGS.get().put(entity.toString(), entityTags);
            // Populate Tag list.
            for (EntityTag entityTag : entityTags) {
                tags.add(entityTag.getTag());
            }
        }
        return tags;
    }

    public List<Tag> getTagsWithCount() {
        return dao.getTagsWithCount();
    }

    public List<Tag> getTagsWithCount(Collection<String> incTags, Collection<String> excTags) {
        return dao.getTagsWithCount(incTags, excTags);
    }

    public String getTagsCSV(IAMEEEntityReference entity) {
        StringBuilder sb = new StringBuilder("");
        for (Tag tag : getTags(entity)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(tag.getTag());
        }
        return sb.toString();
    }

    public Tag getTagByIdentifier(String identifier) {
        Tag tag = null;
        if (UidGen.INSTANCE_12.isValid(identifier)) {
            tag = getTagByUid(identifier);
        }
        if (tag == null) {
            tag = getTagByTag(identifier);
        }
        return tag;
    }

    public Tag getTagByUid(String uid) {
        return dao.getTagByUid(uid);
    }

    public Tag getTagByTag(String tag) {
        return dao.getTagByTag(tag);
    }

    public EntityTag getEntityTag(IAMEEEntityReference entity, String tag) {
        return dao.getEntityTag(entity, tag);
    }

    public List<EntityTag> getEntityTagsForTag(ObjectType objectType, Tag tag) {
        return dao.getEntityTagsForTag(objectType, tag);
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
        for (EntityTag entityTag : dao.getEntityTagsForEntities(objectType, entities)) {
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

    public Long getTagCount() {
        return dao.getTagCount();
    }
}
