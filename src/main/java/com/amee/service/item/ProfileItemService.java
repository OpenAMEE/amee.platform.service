package com.amee.service.item;

import com.amee.domain.IProfileItemService;
import com.amee.domain.data.ItemValue;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

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
        for (BaseItemValue biv : getItemValues(profileItem)) {
            ItemValue iv = biv.getAdapter();
            if (iv.hasPerTimeUnit() && iv.isNonZero()) {
                return true;
            }
        }
        return false;
    }

    //TODO - TEMP HACK - will remove as soon we decide how to handle return units in V1 correctly.
    @Override
    public boolean isSingleFlight(NuProfileItem profileItem) {
        for (BaseItemValue biv : getItemValues(profileItem)) {
            ItemValue iv = biv.getAdapter();
            if ((iv.getName().startsWith("IATA") && iv.getValue().length() > 0) ||
                    (iv.getName().startsWith("Lat") && !iv.getValue().equals("-999")) ||
                    (iv.getName().startsWith("Lon") && !iv.getValue().equals("-999"))) {
                return true;
            }
        }
        return false;
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

    // ItemValues.

    public void persist(BaseItemValue itemValue) {
        dao.persist(itemValue);
    }

    public void loadItemValuesForItems(Collection<BaseItem> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ProfileItemServiceDAO getDao() {
        return dao;
    }
}

