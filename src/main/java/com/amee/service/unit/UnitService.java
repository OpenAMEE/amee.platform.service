package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnitType;

import java.util.List;

public interface UnitService {

    public List<AMEEUnitType> getUnitTypes();

    public AMEEUnitType getUnitTypeByIdentifier(String unitTypeIdentifier);

    public AMEEUnitType getUnitTypeByUid(String uid);

    public AMEEUnitType getUnitTypeByName(String name);

    public boolean isUnitTypeUniqueByName(AMEEUnitType unitType);

    public void persist(AMEEUnitType unitType);

    public void remove(AMEEUnitType unitType);
}
