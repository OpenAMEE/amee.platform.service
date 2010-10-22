package com.amee.service.item;

import com.amee.domain.IProfileItemService;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.BaseProfileItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileItemService extends ItemService implements IProfileItemService {

    @Autowired
    private ProfileItemServiceDAO dao;

    @Override
    public NuProfileItem getItemByUid(String uid) {
        NuProfileItem profileItem = dao.getItemByUid(uid);
        if ((profileItem != null) && (!profileItem.isTrash())) {
            return profileItem;
        } else {
            return null;
        }
    }

    @Override
    public boolean hasNonZeroPerTimeValues(NuProfileItem profileItem) {
        // TODO: See com.amee.domain.profile.LegacyProfileItem#hasNonZeroPerTimeValues.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSingleFlight(NuProfileItem profileItem) {
        // TODO: See com.amee.domain.profile.LegacyProfileItem#isSingleFlight.
        throw new UnsupportedOperationException();
    }

    /**
     * Get an {@link com.amee.domain.data.LegacyItemValue} belonging to this Item using some identifier and prevailing datetime context.
     *
     * @param identifier - a value to be compared to the path and then the uid of the {@link com.amee.domain.data.LegacyItemValue}s belonging
     *                   to this Item.
     * @return the matched {@link com.amee.domain.data.LegacyItemValue} or NULL if no match is found.
     */
    @Override
    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        if (!NuProfileItem.class.isAssignableFrom(item.getClass()))
            throw new IllegalStateException("A NuProfileItem instance was expected.");
        return getItemValue(item, identifier, ((NuProfileItem) item).getEffectiveStartDate());
    }

    @Override
    public void persist(NuProfileItem profileItem) {
        dao.persist(profileItem);
    }

    // Representations.

    @Override
    public JSONObject getJSONObject(BaseItem item, boolean detailed) throws JSONException {
        if (!NuProfileItem.class.isAssignableFrom(item.getClass()))
            throw new IllegalStateException("A NuProfileItem instance was expected.");
        return getJSONObject((NuProfileItem) item, detailed);
    }

    @Override
    public JSONObject getJSONObject(NuProfileItem profileItem, boolean detailed) throws JSONException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JSONObject getJSONObject(BaseItemValue itemValue, boolean detailed) throws JSONException {
        if (!BaseProfileItemValue.class.isAssignableFrom(itemValue.getClass()))
            throw new IllegalStateException("A BaseProfileItemValue instance was expected.");
        return getJSONObject((BaseProfileItemValue) itemValue, detailed);
    }

    @Override
    public JSONObject getJSONObject(BaseProfileItemValue itemValue, boolean detailed) throws JSONException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProfileItemServiceDAO getDao() {
        return dao;
    }
}
