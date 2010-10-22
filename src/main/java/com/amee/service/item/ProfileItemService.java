package com.amee.service.item;

import com.amee.domain.IProfileItemService;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.BaseProfileItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import com.amee.platform.science.ReturnValues;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

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

    public Date getEffectiveStartDate(BaseItem item) {
        throw new UnsupportedOperationException();
    }

    // Representations.

    public JSONObject getJSONObject(BaseItem item, boolean detailed) throws JSONException {
        if (!NuProfileItem.class.isAssignableFrom(item.getClass()))
            throw new IllegalStateException("A NuProfileItem instance was expected.");
        return getJSONObject((NuProfileItem) item, detailed);
    }

    public JSONObject getJSONObject(NuProfileItem profileItem, boolean detailed) throws JSONException {
        throw new UnsupportedOperationException();
    }

    public JSONObject getJSONObject(BaseItemValue itemValue, boolean detailed) throws JSONException {
        if (!BaseProfileItemValue.class.isAssignableFrom(itemValue.getClass()))
            throw new IllegalStateException("A BaseProfileItemValue instance was expected.");
        return getJSONObject((BaseProfileItemValue) itemValue, detailed);
    }

    public JSONObject getJSONObject(BaseProfileItemValue itemValue, boolean detailed) throws JSONException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProfileItemServiceDAO getDao() {
        return dao;
    }
}

