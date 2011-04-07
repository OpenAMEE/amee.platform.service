package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnit;
import com.amee.domain.unit.AMEEUnitType;

import java.util.List;

public interface UnitServiceDAO {

    // Unit Types.

    public List<AMEEUnitType> getUnitTypes();

    public AMEEUnitType getUnitTypeByUid(String uid);

    public AMEEUnitType getUnitTypeByName(String name);

    public boolean isUnitTypeUniqueByName(AMEEUnitType ameeUnit);

    public void persist(AMEEUnitType unitType);

    // Units.

    public List<AMEEUnit> getUnits(AMEEUnitType unitType);

    public AMEEUnit getUnitByUid(String uid);

    public AMEEUnit getUnitBySymbol(String symbol);

    public boolean isUnitUniqueByName(AMEEUnit unit);

    public boolean isUnitUniqueBySymbol(AMEEUnit unit);

    public void persist(AMEEUnit unit);
}
