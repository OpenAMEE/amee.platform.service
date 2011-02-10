package com.amee.platform.search;

import com.amee.service.invalidation.InvalidationMessage;
import org.springframework.context.ApplicationListener;

public interface SearchManager extends ApplicationListener<InvalidationMessage> {

    public void update();

    public void updateAll();
}
