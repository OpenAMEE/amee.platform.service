package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnit;
import com.amee.domain.unit.AMEEUnitType;

import java.util.List;

public interface UnitService {

    // Unit Types.

    public List<AMEEUnitType> getUnitTypes();

    public AMEEUnitType getUnitTypeByIdentifier(String unitTypeIdentifier);

    public AMEEUnitType getUnitTypeByUid(String uid);

    public AMEEUnitType getUnitTypeByName(String name);

    public boolean isUnitTypeUniqueByName(AMEEUnitType unitType);

    public void persist(AMEEUnitType unitType);

    public void remove(AMEEUnitType unitType);

    // Units.

    public List<AMEEUnit> getUnits();

    public List<AMEEUnit> getUnits(AMEEUnitType unitType);

    public AMEEUnit getUnitByIdentifier(String unitIdentifier);

    public AMEEUnit getUnitByUid(String uid);

    public AMEEUnit getUnitBySymbol(String symbol);

    public boolean isUnitUniqueByName(AMEEUnit unit);

    public boolean isUnitUniqueByInternalSymbol(AMEEUnit unit);

    public boolean isUnitUniqueByExternalSymbol(AMEEUnit unit);

    public void persist(AMEEUnit unit);

    public void remove(AMEEUnit unit);

    // For tests.

    public void setUnitServiceDAO(UnitServiceDAO dao);
}
