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

import com.amee.domain.AMEEStatus;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;
import com.amee.domain.item.data.NuDataItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class DataItemServiceDAOImpl extends ItemServiceDAOImpl implements DataItemServiceDAO {

    private final Log log = LogFactory.getLog(getClass());

    public Class getEntityClass() {
        return NuDataItem.class;
    }

    /**
     * Returns the DataItem matching the specified UID.
     *
     * @param uid for the requested DataItem
     * @return the matching DataItem or null if not found
     */
    public NuDataItem getItemByUid(String uid) {
        return (NuDataItem) super.getItemByUid(uid);
    }

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        if (!NuDataItem.class.isAssignableFrom(item.getClass())) throw new IllegalStateException();
        return getDataItemValues((NuDataItem) item);
    }

    @Override
    public Set<BaseItemValue> getDataItemValues(NuDataItem dataItem) {
        Set<BaseItemValue> rawItemValues = new HashSet<BaseItemValue>();
        rawItemValues.addAll(getDataItemNumberValues(dataItem));
        rawItemValues.addAll(getDataItemTextValues(dataItem));
        return rawItemValues;
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param dataItem
     * @return
     */
    public List<DataItemNumberValue> getDataItemNumberValues(NuDataItem dataItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItemNumberValue.class);
        criteria.add(Restrictions.eq("dataItem.id", dataItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param dataItem
     * @return
     */
    public List<DataItemTextValue> getDataItemTextValues(NuDataItem dataItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItemTextValue.class);
        criteria.add(Restrictions.eq("dataItem.id", dataItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }
}