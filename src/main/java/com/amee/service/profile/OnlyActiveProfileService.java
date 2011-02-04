package com.amee.service.profile;

import com.amee.domain.item.data.NuDataItem;
import com.amee.domain.item.profile.NuProfileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This file is part of AMEE.
 * <p/>
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
@Service
public class OnlyActiveProfileService {

    @Autowired
    ProfileService profileService;

    /**
     * Filter the supplied ProfileItem list to only include the latest item in the historical
     * sequence for each dataItem and name.
     *
     * @param profileItems - list to filter
     * @return the List of active {@link com.amee.domain.item.profile.NuProfileItem}
     */
    @SuppressWarnings("unchecked")
    public List<NuProfileItem> getProfileItems(List<NuProfileItem> profileItems) {
        NuDataItem di = new NuDataItem();
        String name = "";
        List<NuProfileItem> activeProfileItems = new ArrayList<NuProfileItem>();
        // The profileItems collection is in name, dataItem, startDate DESC order for we can
        // just select the first entry in the collection for each name/dataItem combination.
        for (NuProfileItem pi : profileItems) {
            if (!name.equals(pi.getName()) || !pi.getDataItem().getId().equals(di.getId())) {
                activeProfileItems.add(pi);
                di = pi.getDataItem();
                name = pi.getName();
            }
        }
        return activeProfileItems;
    }
}
