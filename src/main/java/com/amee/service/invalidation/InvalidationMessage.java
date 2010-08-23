package com.amee.service.invalidation;

import com.amee.domain.AMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.auth.AccessSpecification;
import com.amee.messaging.Message;
import com.amee.messaging.MessagingException;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class InvalidationMessage extends Message implements IAMEEEntityReference {

    private String serverName;
    private String instanceName;
    private ObjectType objectType;
    private Long entityId;
    private String entityUid;
    private String options;

    public InvalidationMessage(Object source) {
        super(source);
        init();
        setServerName(System.getProperty("server.name"));
        setInstanceName(System.getProperty("instance.name"));
    }

    public InvalidationMessage(Object source, ObjectType objectType, Long entityId, String entityUid) {
        this(source);
        setObjectType(objectType);
        setEntityId(entityId);
        setEntityUid(entityUid);
    }

    public InvalidationMessage(Object source, IAMEEEntityReference entity) {
        this(source, entity.getObjectType(), entity.getEntityId(), entity.getEntityUid());
    }

    public InvalidationMessage(Object source, IAMEEEntityReference entity, String options) {
        this(source, entity.getObjectType(), entity.getEntityId(), entity.getEntityUid());
        setOptions(options);
    }

    public InvalidationMessage(Object source, String message) {
        super(source, message);
    }

    public void init() {
        setEntityId(0L);
        setEntityUid("");
        setOptions("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || !InvalidationMessage.class.isAssignableFrom(o.getClass())) return false;
        InvalidationMessage message = (InvalidationMessage) o;
        return (getEntityUid().equals(message.getEntityUid())) &&
                getObjectType().equals(message.getObjectType()) &&
                getOptions().equals(message.getOptions());
    }

    /**
     * Returns a hash code based on the entityId and entityType properties.
     * <p/>
     * This should remain similar to com.amee.domain.AMEEEntity#hashCode.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == getEntityUid() ? 0 : getEntityUid().hashCode());
        hash = 31 * hash + (null == getObjectType() ? 0 : getObjectType().hashCode());
        hash = 31 * hash + (null == getOptions() ? 0 : getOptions().hashCode());
        return hash;
    }

    @Override
    public String getMessage() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("sn", getServerName());
            obj.put("in", getInstanceName());
            obj.put("ot", getObjectType().getName());
            obj.put("id", getEntityId());
            obj.put("uid", getEntityUid());
            obj.put("op", getOptions());
        } catch (JSONException e) {
            throw new MessagingException("Caught JSONException: " + e.getMessage(), e);
        }
        return obj.toString();
    }

    @Override
    public void setMessage(String message) {
        if (message == null) {
            throw new MessagingException("InvalidationEvent message was null.");
        }
        init();
        try {
            JSONObject obj = new JSONObject(message);
            if (obj.has("sn")) {
                setServerName(obj.getString("sn"));
            }
            if (obj.has("in")) {
                setInstanceName(obj.getString("in"));
            }
            if (obj.has("ot")) {
                setObjectType(ObjectType.valueOf(obj.getString("ot")));
            }
            if (obj.has("id")) {
                setEntityId(obj.getLong("id"));
            }
            if (obj.has("uid")) {
                setEntityUid(obj.getString("uid"));
            }
            if (obj.has("op")) {
                setOptions(obj.getString("op"));
            }
        } catch (IllegalArgumentException e) {
            throw new MessagingException("InvalidationEvent message has ObjectType cannot be parsed.");
        } catch (JSONException e) {
            throw new MessagingException("Caught JSONException: " + e.getMessage(), e);
        }
    }

    public boolean isFromSameInstance() {
        return getServerName().equalsIgnoreCase(System.getProperty("server.name")) &&
                getInstanceName().equalsIgnoreCase(System.getProperty("instance.name"));
    }

    public boolean isFromOtherInstance() {
        return !isFromSameInstance();
    }

    public boolean hasOption(String option) {
        return !getOptions().isEmpty() && ArrayUtils.contains(getOptions().split(","), option);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    @Override
    public AccessSpecification getAccessSpecification() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAccessSpecification(AccessSpecification accessSpecification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AMEEEntity getEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntity(AMEEEntity entity) {
        throw new UnsupportedOperationException();
    }

    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEntityUid() {
        return entityUid;
    }

    public void setEntityUid(String entityUid) {
        if (entityUid == null) {
            entityUid = "";
        }
        this.entityUid = entityUid;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        if (options == null) {
            options = "";
        }
        this.options = options;
    }
}
