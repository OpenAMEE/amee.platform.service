package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEEntity;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import org.apache.lucene.document.Document;

public interface SearchQueryService {

    public ResultsWrapper<Document> doSearch(SearchFilter filter);

    public Document getDocument(AMEEEntity entity);

    public Document getDocument(AMEEEntity entity, boolean removeIfDuplicated);

    public Document getDocument(ObjectType objectType, String uid);

    public Document getDocument(ObjectType objectType, String uid, boolean removeIfDuplicated);

    public void remove(ObjectType entityType, String uid);

    public void remove(ObjectType entityType);

    public void removeDataCategory(DataCategory dataCategory);

    public void removeDataItems(DataCategory dataCategory);
}
