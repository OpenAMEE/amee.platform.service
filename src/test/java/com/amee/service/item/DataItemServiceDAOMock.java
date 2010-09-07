package com.amee.service.item;

import com.amee.domain.item.data.NuDataItem;
import org.springframework.stereotype.Repository;

@Repository
public class DataItemServiceDAOMock implements DataItemServiceDAO {

    @Override
    public NuDataItem getItemByUid(String uid) {
        return null;
    }
}