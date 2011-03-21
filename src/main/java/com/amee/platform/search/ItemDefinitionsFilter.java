package com.amee.platform.search;

import com.amee.domain.LimitFilter;

public class ItemDefinitionsFilter extends LimitFilter {

    private String name = "";

    public ItemDefinitionsFilter() {
        super();
    }

    @Override
    public int getResultLimitDefault() {
        return 50;
    }

    @Override
    public int getResultLimitMax() {
        return 100;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        this.name = name;
    }
}