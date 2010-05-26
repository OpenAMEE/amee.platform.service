package com.amee.platform.search;

import com.amee.domain.ObjectType;

import java.beans.PropertyEditorSupport;
import java.util.HashSet;
import java.util.Set;

public class ObjectTypesEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        if (text != null) {
            Set<ObjectType> types = new HashSet<ObjectType>();
            for (String s : text.split(",")) {
                types.add(ObjectType.valueOf(s.trim().toUpperCase()));
            }
            setValue(types);
        }
    }
}

