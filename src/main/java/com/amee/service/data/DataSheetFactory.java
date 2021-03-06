package com.amee.service.data;

import com.amee.domain.DataItemService;
import com.amee.domain.LocaleHolder;
import com.amee.domain.ValueType;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.sheet.Cell;
import com.amee.domain.sheet.Column;
import com.amee.domain.sheet.Row;
import com.amee.domain.sheet.Sheet;
import com.amee.platform.science.StartEndDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DataSheetFactory implements CacheableFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DataItemService dataItemService;
    private DataBrowser dataBrowser;
    private String cacheName;

    private DataSheetFactory() {
        super();
    }

    public DataSheetFactory(DataItemService dataItemService, DataBrowser dataBrowser, String cacheName) {
        this();
        this.dataItemService = dataItemService;
        this.dataBrowser = dataBrowser;
        this.cacheName = cacheName;
    }

    public Object create() {

        log.debug("create()");

        List<Column> columns;
        Row row;
        BaseItemValue itemValue;
        Sheet sheet = null;
        ItemDefinition itemDefinition;
        DataCategory dataCategory = dataBrowser.getDataCategory();

        // must have an ItemDefinition
        itemDefinition = dataCategory.getItemDefinition();
        if (itemDefinition != null) {

            log.debug("create() - Create Sheet and Columns.");

            // create sheet and columns
            sheet = new Sheet();
            sheet.setKey(getKey());
            sheet.setLabel("DataItems");
            for (ItemValueDefinition itemValueDefinition : itemDefinition.getItemValueDefinitions()) {
                if (itemValueDefinition.isFromData()) {
                    new Column(sheet, itemValueDefinition.getPath(), itemValueDefinition.getName());
                }
            }
            new Column(sheet, "label");
            new Column(sheet, "path");
            new Column(sheet, "uid", true);
            new Column(sheet, "created", true);
            new Column(sheet, "modified", true);
            new Column(sheet, "startDate");
            new Column(sheet, "endDate");

            log.debug("create() - Create Rows and Cells.");

            // create rows and cells
            columns = sheet.getColumns();
            StartEndDate startDate = dataBrowser.getQueryStartDate();
            for (DataItem dataItem : dataItemService.getDataItems(dataCategory)) {
                row = new Row(sheet, dataItem.getUid());
                row.setLabel("DataItem");
                for (Column column : columns) {
                    itemValue = dataItemService.getItemValue(dataItem, column.getName(), startDate);
                    if (itemValue != null) {
                        new Cell(column, row, itemValue.getValueAsString(), itemValue.getUid(), itemValue.getItemValueDefinition().getValueDefinition().getValueType());
                    } else if ("label".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItemService.getLabel(dataItem), ValueType.TEXT);
                    } else if ("path".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItem.getDisplayPath(), ValueType.TEXT);
                    } else if ("uid".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItem.getUid(), ValueType.TEXT);
                    } else if ("created".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItem.getCreated(), ValueType.DATE);
                    } else if ("modified".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItem.getModified(), ValueType.DATE);
                    } else if ("startDate".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItemService.getStartDate(dataItem), ValueType.DATE);
                    } else if ("endDate".equalsIgnoreCase(column.getName())) {
                        new Cell(column, row, dataItemService.getEndDate(dataItem), ValueType.DATE);
                    } else {
                        // add empty cell
                        new Cell(column, row);
                    }
                }
            }

            log.debug("create() - Do sorts.");

            // sort columns and rows in sheet
            sheet.setDisplayBy(itemDefinition.getDrillDown());
            sheet.sortColumns();
            sheet.setSortBy(itemDefinition.getDrillDown());
            sheet.sortRows();
        }

        log.debug("create() - Done.");

        return sheet;
    }

    public String getKey() {
        return "DataSheet_" + dataBrowser.getDataCategory().getUid() +
                "_" +
                dataBrowser.getQueryStartDate() +
                "_" +
                ((dataBrowser.getQueryEndDate() != null) ? dataBrowser.getQueryEndDate() : "") +
                "_" +
                LocaleHolder.getLocale();
    }

    public String getCacheName() {
        return cacheName;
    }
}
