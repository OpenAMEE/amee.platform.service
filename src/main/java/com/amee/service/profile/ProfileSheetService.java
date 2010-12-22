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
package com.amee.service.profile;

import com.amee.domain.cache.CacheHelper;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.data.DataCategory;
import com.amee.domain.profile.Profile;
import com.amee.domain.sheet.Sheet;
import com.amee.base.utils.ThreadBeanHolder;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class ProfileSheetService implements Serializable {

    private CacheHelper cacheHelper = CacheHelper.getInstance();

    ProfileSheetService() {
        super();
    }

    public Sheet getSheet(CacheableFactory builder) {
        return (Sheet) cacheHelper.getCacheable(builder);
    }

    public Sheet getSheet(DataCategory dataCategory, CacheableFactory builder) {
        ThreadBeanHolder.set(DataCategory.class, dataCategory);
        return (Sheet) cacheHelper.getCacheable(builder);
    }

    public void removeSheets(Profile profile) {
        cacheHelper.clearCache("ProfileSheets", "ProfileSheet_" + profile.getUid());
    }
}