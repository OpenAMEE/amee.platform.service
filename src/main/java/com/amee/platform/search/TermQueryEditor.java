package com.amee.platform.search;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import java.beans.PropertyEditorSupport;

public class TermQueryEditor extends PropertyEditorSupport {

    private String field;

    public TermQueryEditor(String field) {
        this.setField(field);
    }

    @Override
    public void setAsText(String text) {
        if (text != null) {
            setValue(new TermQuery(new Term(getField(), text)));
        } else {
            setValue(null);
        }
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
