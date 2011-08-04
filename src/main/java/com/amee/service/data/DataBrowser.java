package com.amee.service.data;

import com.amee.domain.data.DataCategory;
import com.amee.service.BaseBrowser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("dataBrowser")
@Scope("prototype")
public class DataBrowser extends BaseBrowser {

    private DataCategory dataCategory = null;

    public DataBrowser() {
        super();
    }

    public DataCategory getDataCategory() {
        return dataCategory;
    }

    public void setDataCategory(DataCategory dataCategory) {
        this.dataCategory = dataCategory;
    }
}