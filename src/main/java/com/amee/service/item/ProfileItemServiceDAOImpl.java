/**
 * This file is part of AMEE.
 *
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
package com.amee.service.item;

import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class ProfileItemServiceDAOImpl extends ItemServiceDAOImpl implements ProfileItemServiceDAO {

    private final Log log = LogFactory.getLog(getClass());

    public Class getEntityClass() {
        return NuProfileItem.class;
    }

    /**
     * Returns the ProfileItem matching the specified UID.
     *
     * @param uid for the requested ProfileItem
     * @return the matching ProfileItem or null if not found
     */
    public NuProfileItem getItemByUid(String uid) {
        return (NuProfileItem) super.getItemByUid(uid);
    }

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        throw new UnsupportedOperationException();
    }
}