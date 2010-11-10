package com.amee.service.data;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.cache.CacheableFactory;

public class DataCategoryChildrenFactory implements CacheableFactory {

    private IDataCategoryReference dc;
    private DataServiceDAO dao;

    public DataCategoryChildrenFactory(IDataCategoryReference dc, DataServiceDAO dao) {
        super();
        this.dc = dc;
        this.dao = dao;
    }

    @Override
    public Object create() {
        return dao.getDataCategories(dc);
    }

    @Override
    public String getKey() {
        return dc.getEntityUid();
    }

    @Override
    public String getCacheName() {
        return "DataCategoryChildren";
    }
}