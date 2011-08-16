package com.amee.service;

import com.amee.domain.TimeZoneHolder;
import com.amee.platform.science.StartEndDate;

import java.util.TimeZone;

public class BaseBrowser {

    protected StartEndDate startDate;
    protected StartEndDate endDate;

    /**
     * Get the query start date.
     *
     * @return the {@link com.amee.platform.science.StartEndDate StartEndDate} submitted with the GET request. If no date
     * was submitted, the default date corresponding to the start of the month is returned.
     */
    public StartEndDate getQueryStartDate() {
        if (startDate != null) {
            return startDate;
        } else {
            TimeZone timeZone = TimeZoneHolder.getTimeZone();
            return StartEndDate.getStartOfMonthDate(timeZone);
        }
    }

    public void setQueryStartDate(String date) {
        if (date != null) {
            startDate = new StartEndDate(date);
        }
    }

    /**
     * Get the query end date.
     *
     * @return the {@link com.amee.platform.science.StartEndDate StartEndDate} submitted with the GET request. If no date
     * was submitted, then Null is returned.
     */
    public StartEndDate getQueryEndDate() {
        return endDate;
    }

    public void setQueryEndDate(String date) {
        if (date != null) {
            endDate = new StartEndDate(date);
        }
    }

    public boolean isQuery() {
        return (startDate != null) || (endDate != null);
    }
}
