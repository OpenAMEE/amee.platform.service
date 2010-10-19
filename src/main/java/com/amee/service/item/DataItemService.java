package com.amee.service.item;

import com.amee.domain.IDataItemService;
import com.amee.domain.item.data.NuDataItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataItemService extends ItemService implements IDataItemService {

    @Autowired
    private DataItemServiceDAO dao;

    public NuDataItem getItemByUid(String uid) {
        NuDataItem dataItem = dao.getItemByUid(uid);
        if ((dataItem != null) && (!dataItem.isTrash())) {
            return dataItem;
        } else {
            return null;
        }
    }

    public String getLabel(NuDataItem dataItem) {
        // TODO: See com.amee.domain.data.LegacyDataItem#getLabel.
        throw new UnsupportedOperationException();
    }
}
