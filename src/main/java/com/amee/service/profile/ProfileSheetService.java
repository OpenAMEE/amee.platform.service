package com.amee.service.profile;

import com.amee.domain.cache.CacheHelper;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.profile.Profile;
import com.amee.domain.sheet.Sheet;
import org.springframework.stereotype.Service;

@Service
public class ProfileSheetService {

    private CacheHelper cacheHelper = CacheHelper.getInstance();

    ProfileSheetService() {
        super();
    }

    public Sheet getSheet(CacheableFactory builder) {
        return (Sheet) cacheHelper.getCacheable(builder);
    }

    public void removeSheets(Profile profile) {
        cacheHelper.clearCache("ProfileSheets", "ProfileSheet_" + profile.getUid());
    }
}