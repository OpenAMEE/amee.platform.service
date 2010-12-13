package com.amee.service.tag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TagsFilter implements Serializable {

    List<String> incTags = new ArrayList<String>();
    List<String> excTags = new ArrayList<String>();

    public TagsFilter() {
        super();
    }

    public List<String> getIncTags() {
        return incTags;
    }

    public boolean hasIncTags() {
        return !getIncTags().isEmpty();
    }

    public void setIncTags(List<String> incTags) {
        if (incTags != null) {
            this.incTags = incTags;
        } else {
            this.incTags.clear();
        }
    }

    public List<String> getExcTags() {
        return excTags;
    }

    public boolean hasExcTags() {
        return !getExcTags().isEmpty();
    }

    public void setExcTags(List<String> excTags) {
        if (excTags != null) {
            this.excTags = excTags;
        } else {
            this.excTags.clear();
        }
    }
}