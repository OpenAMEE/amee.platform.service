package com.amee.service.item;

import com.amee.domain.IProfileItemService;
import com.amee.domain.item.profile.NuProfileItem;
import com.amee.platform.science.ReturnValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileItemService extends ItemService implements IProfileItemService {

    @Autowired
    private ProfileItemServiceDAO dao;

    public NuProfileItem getItemByUid(String uid) {
        NuProfileItem profileItem = dao.getItemByUid(uid);
        if ((profileItem != null) && (!profileItem.isTrash())) {
            return profileItem;
        } else {
            return null;
        }
    }

    public ReturnValues getAmounts(NuProfileItem profileItem, boolean recalculate) {
        // TODO: See com.amee.domain.profile.LegacyProfileItem#getAmounts.
        throw new UnsupportedOperationException();
    }

    public ReturnValues getAmounts(NuProfileItem profileItem) {
        return getAmounts(profileItem, false);
    }

    @Deprecated
    public double getAmount(NuProfileItem profileItem) {
        return getAmounts(profileItem).defaultValueAsDouble();
    }
    
    public boolean hasNonZeroPerTimeValues(NuProfileItem profileItem) {
        // TODO: See com.amee.domain.profile.LegacyProfileItem#hasNonZeroPerTimeValues.
        throw new UnsupportedOperationException();
    }

    public boolean isSingleFlight(NuProfileItem profileItem) {
        // TODO: See com.amee.domain.profile.LegacyProfileItem#isSingleFlight.
        throw new UnsupportedOperationException();
    }
}

