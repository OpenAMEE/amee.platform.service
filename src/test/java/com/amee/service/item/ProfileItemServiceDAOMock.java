package com.amee.service.item;

import com.amee.domain.item.profile.NuProfileItem;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileItemServiceDAOMock implements ProfileItemServiceDAO {

    @Override
    public NuProfileItem getItemByUid(String uid) {
        return null;
    }
}