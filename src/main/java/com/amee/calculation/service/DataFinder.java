/**
 * This file is part of AMEE.
 *
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
package com.amee.calculation.service;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.IDataItemService;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Scope("prototype")
public class DataFinder {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DataService dataService;

    @Autowired
    private IDataItemService dataItemService;

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