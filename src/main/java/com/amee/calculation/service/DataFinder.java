package com.amee.calculation.service;

import com.amee.domain.DataItemService;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.sheet.Choice;
import com.amee.domain.sheet.Choices;
import com.amee.platform.science.Amount;
import com.amee.platform.science.DataPoint;
import com.amee.platform.science.DataSeries;
import com.amee.platform.science.ExternalHistoryValue;
import com.amee.service.data.DataService;
import com.amee.service.data.DrillDownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * This class provides methods to be used by javascript algorithms for Data Item access.
 * An instance of this class is provided to the algorithm see: {@link CalculationService}
 */
@Component
@Scope("prototype")
public class DataFinder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataService dataService;

    @Autowired
    private DataItemService dataItemService;

    @Autowired
    private DrillDownService drillDownService;

    private Date startDate = new Date();
    private Date endDate;

    public DataFinder() {
        super();
    }

    /**
     * Gets a DataSeries for a DataItemValue
     * 
     * @param path the DataCategory path, eg "home/energy/electricity"
     * @param drillDown comma separated drilldown selection, eg "country=United Kingdom"
     * @param name the item value path, eg "kgCO2PerKWh"
     * @return a DataSeries for the DataItemValue.
     */
    public DataSeries getDataSeries(String path, String drillDown, String name) {
        DataSeries series = new DataSeries();
        series.setSeriesStartDate(new DateTime(startDate));
        series.setSeriesEndDate(new DateTime(endDate));
        DataItem dataItem = getDataItem(path, drillDown);
        if (dataItem != null) {
            // Get all the values
            List<BaseItemValue> itemValues = dataItemService.getAllItemValues(dataItem, name);
            for (BaseItemValue value : itemValues) {
                if (ExternalHistoryValue.class.isAssignableFrom(value.getClass())) {
                    series.addDataPoint(new DataPoint(((ExternalHistoryValue) value).getStartDate().toDateTime(), new Amount(value.getValueAsString())));
                } else {
                    series.addDataPoint(new DataPoint(new Amount(value.getValueAsString())));
                }
            }
        }
        log.debug("getDataSeries() - path: " + path + ", drillDown: " + drillDown + ", name: " + name + ", values: " + series);
        return series;
    }

    /**
     *
     * @param path the DataCategory path, eg "home/energy/electricity"
     * @param drillDown comma separated drilldown selection, eg "country=United Kingdom"
     * @param name the item value path, eg "kgCO2PerKWh"
     * @return the current value for the DataItemValue.
     */
    public String getDataItemValue(String path, String drillDown, String name) {
        String value = null;
        BaseItemValue itemValue;
        DataItem dataItem = getDataItem(path, drillDown);
        if (dataItem != null) {
            itemValue = dataItemService.getItemValue(dataItem, name);
            if (itemValue != null) {
                value = itemValue.getValueAsString();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("getDataItemValue() - path: " + path + ", drillDown: " + drillDown + ", name: " + name + ", value: " + value);
        }
        return value;
    }

    public DataItem getDataItem(String path, String drillDown) {
        DataItem dataItem = null;
        Choices choices;
        IDataCategoryReference dataCategory = getDataCategory(path);
        if (dataCategory != null) {
            choices = drillDownService.getChoices(dataCategory, Choice.parseChoices(drillDown));
            if (choices.getName().equals("uid") && (choices.getChoices().size() > 0)) {
                dataItem = dataItemService.getItemByUid(
                        choices.getChoices().get(0).getValue());
                dataItem.setEffectiveStartDate(startDate);
            }
        }
        return dataItem;
    }

    public IDataCategoryReference getDataCategory(String path) {
        return dataService.getDataCategoryByFullPath(path);
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}
