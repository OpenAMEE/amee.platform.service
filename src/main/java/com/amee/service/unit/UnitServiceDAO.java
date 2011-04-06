package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnitType;

public interface UnitServiceDAO {

    public AMEEUnitType getUnitTypeByUid(String uid);

    public AMEEUnitType getUnitTypeByName(String name);

    public boolean isUnitTypeUniqueByName(AMEEUnitType ameeUnit);

    public void persist(AMEEUnitType unitType);
}
