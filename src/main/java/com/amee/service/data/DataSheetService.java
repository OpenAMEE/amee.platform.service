package com.amee.service.data;

import com.amee.domain.APIVersion;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.sheet.Choices;
import com.amee.domain.sheet.Sheet;
import com.amee.service.invalidation.InvalidationMessage;
import org.springframework.context.ApplicationListener;

import java.util.Set;

public interface DataSheetService extends ApplicationListener<InvalidationMessage> {

    void clearCaches(DataCategory dataCategory);

    Sheet getSheet(DataBrowser browser, String fullPath);

    void removeSheet(DataCategory dataCategory);

    Set<String> getEternalPaths();

    void setEternalPaths(Set<String> eternalPaths);

    Choices getUserValueChoices(DataItem dataItem, APIVersion apiVersion);
}
