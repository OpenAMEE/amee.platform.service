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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public abstract class ItemServiceDAOImpl implements ItemServiceDAO {

    private final Log log = LogFactory.getLog(getClass());

    protected static final String CACHE_REGION = "query.itemService";

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Returns the Class for the Item implementation.
     *
     * @return the entity Class
     */
    public abstract Class getEntityClass();

    /**
     * Returns the Item matching the specified UID.
     *
     * @param uid for the requested Item
     * @return the matching Item or null if not found
     */
    @SuppressWarnings(value = "unchecked")
    public BaseItem getItemByUid(String uid) {
        BaseItem item = null;
        if (!StringUtils.isBlank(uid)) {
            // See http://www.hibernate.org/117.html#A12 for notes on DISTINCT_ROOT_ENTITY.
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(getEntityClass());
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<BaseItem> items = criteria.list();
            if (items.size() == 1) {
                item = items.get(0);
            } else {
                log.debug("getItemByUid() NOT found: " + uid);
            }
        }
        return item;
    }
}