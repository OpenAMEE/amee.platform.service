package com.amee.platform.search;

import com.amee.domain.LimitFilter;

public class ProfilesFilter extends LimitFilter {

    @Override
    public int getResultLimitDefault() {
        return 50;
    }

    @Override
    public int getResultLimitMax() {
        return 100;
    }
}
