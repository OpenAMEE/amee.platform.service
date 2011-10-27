package com.amee.service.profile;

import com.amee.domain.item.data.DataItem;
import com.amee.domain.item.profile.ProfileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OnlyActiveProfileService {

    /**
     * Filter the supplied ProfileItem list to only include the latest item in the historical
     * sequence for each dataItem and name.
     *
     * @param profileItems - list to filter
     * @return the List of active {@link com.amee.domain.item.profile.ProfileItem}
     */
    @SuppressWarnings("unchecked")
    public List<ProfileItem> getProfileItems(List<ProfileItem> profileItems) {
        DataItem di = new DataItem();
        String name = "";
        List<ProfileItem> activeProfileItems = new ArrayList<ProfileItem>();
        // The profileItems collection is in name, dataItem, startDate DESC order for we can
        // just select the first entry in the collection for each name/dataItem combination.
        for (ProfileItem pi : profileItems) {
            if (!name.equals(pi.getName()) || !pi.getDataItem().getId().equals(di.getId())) {
                activeProfileItems.add(pi);
                di = pi.getDataItem();
                name = pi.getName();
            }
        }
        return activeProfileItems;
    }
}
