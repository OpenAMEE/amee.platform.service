package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnitType;

import java.util.List;

public interface UnitServiceDAO {

    public List<AMEEUnitType> getUnitTypes();

    public AMEEUnitType getUnitTypeByUid(String uid);

    public AMEEUnitType getUnitTypeByName(String name);

    public boolean isUnitTypeUniqueByName(AMEEUnitType ameeUnit);

    public void persist(AMEEUnitType unitType);
}
