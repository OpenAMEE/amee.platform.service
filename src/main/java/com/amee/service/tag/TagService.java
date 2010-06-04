package com.amee.service.tag;

import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.tag.EntityTag;
import com.amee.domain.tag.Tag;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagService {

    @Autowired
    private TagServiceDAO dao;

    // A thread bound Map of Tags keyed by IAMEEEntityReferences.
    // private final ThreadLocal<Map<IAMEEEntityReference, List<Tag>>> TAGS =
    //        new ThreadLocal<Map<IAMEEEntityReference, List<Tag>>>() {
    //            protected Map<IAMEEEntityReference, List<Tag>> initialValue() {
    //                return new WeakHashMap<IAMEEEntityReference, List<Tag>>();
    //            }
    //        };

    public List<Tag> getTags() {
        return dao.getTagsWithCount();
    }

    public List<Tag> getTags(IAMEEEntityReference entity) {
        if (entity != null) {
            List<Tag> tags = new ArrayList<Tag>();
            for (EntityTag entityTag : dao.getEntityTags(entity)) {
                tags.add(entityTag.getTag());
            }
            return tags;
        } else {
            return getTags();
        }
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

    public void persist(Tag tag) {
        dao.persist(tag);
    }

    public void remove(Tag tag) {
        dao.remove(tag);
    }

    public void persist(EntityTag entityTag) {
        // TAGS.get().remove(entityTag.getEntityReference());
        dao.persist(entityTag);
    }

    public void remove(EntityTag entityTag) {
        // TAGS.get().remove(entityTag.getEntityReference());
        dao.remove(entityTag);
    }
}