package com.amee.platform.search;

import java.io.Serializable;

public abstract class LimitFilter implements Serializable {

    private int resultStart = 0;
    private int resultLimit = 0;

    public LimitFilter() {
        super();
        setResultLimit(getResultLimitDefault());
    }

    public int getResultStart() {
        return resultStart;
    }

    public void setResultStart(int resultStart) {
        if (resultStart < 0) {
            throw new IllegalArgumentException("resultStart is less than zero");
        }
        this.resultStart = resultStart;
    }

    public int getResultLimit() {
        return resultLimit;
    }

    public void setResultLimit(int resultLimit) {
        if (resultLimit > getResultLimitMax()) {
            throw new IllegalArgumentException("resultLimit is greater than resultLimitMax");
        }
        if (resultLimit < 0) {
            throw new IllegalArgumentException("resultLimit is less than zero");
        }
        this.resultLimit = resultLimit;
    }

    public abstract int getResultLimitDefault();

    public abstract int getResultLimitMax();
}
