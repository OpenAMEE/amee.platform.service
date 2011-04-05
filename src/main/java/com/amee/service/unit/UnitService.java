package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnitType;

public interface UnitService {

    public AMEEUnitType getUnitTypeByIdentifier(String unitTypeIdentifier);

    public AMEEUnitType getUnitTypeByUid(String uid);

    public AMEEUnitType getUnitTypeByName(String name);
}
