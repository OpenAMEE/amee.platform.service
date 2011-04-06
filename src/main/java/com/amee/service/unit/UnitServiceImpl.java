package com.amee.service.unit;

import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEStatus;
import com.amee.domain.unit.AMEEUnit;
import com.amee.domain.unit.AMEEUnitSymbolComparator;
import com.amee.domain.unit.AMEEUnitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitServiceDAO dao;

    // Unit Types.

    public List<AMEEUnitType> getUnitTypes() {
        return dao.getUnitTypes();
    }

    @Override
    public AMEEUnitType getUnitTypeByIdentifier(String identifier) {
        AMEEUnitType unitType = null;
        if (UidGen.INSTANCE_12.isValid(identifier)) {
            unitType = getUnitTypeByUid(identifier);
        }
        if (unitType == null) {
            unitType = getUnitTypeByName(identifier);
        }
        return unitType;
    }

    @Override
    public AMEEUnitType getUnitTypeByUid(String uid) {
        return dao.getUnitTypeByUid(uid);
    }

    @Override
    public AMEEUnitType getUnitTypeByName(String name) {
        return dao.getUnitTypeByName(name);
    }

    /**
     * Returns true if the name of the supplied UnitType is unique.
     *
     * @param unitType to check for uniqueness
     * @return true if the UnitType has a unique name
     */
    @Override
    public boolean isUnitTypeUniqueByName(AMEEUnitType unitType) {
        return dao.isUnitTypeUniqueByName(unitType);
    }

    @Override
    public void persist(AMEEUnitType unitType) {
        dao.persist(unitType);
    }

    @Override
    public void remove(AMEEUnitType unitType) {
        unitType.setStatus(AMEEStatus.TRASH);
    }

    // Units.

    /**
     * Returns all Units. See JavaDoc below for details on sorting.
     *
     * @return the list of Units
     */
    public List<AMEEUnit> getUnits() {
        return getUnits(null);
    }

    /**
     * Returns a sorted list of Units for the supplied Unit Type. If the Unit Type is null all Units will be
     * returned.
     * <p/>
     * Units are sorted by external symbols or internal symbol, with external symbol as the preference.
     *
     * @param unitType to filter Units by, or null if all Units are required
     * @return the list of Units
     */
    public List<AMEEUnit> getUnits(AMEEUnitType unitType) {
        List<AMEEUnit> units = dao.getUnits(unitType);
        Collections.sort(units, new AMEEUnitSymbolComparator());
        return units;
    }

    @Override
    public AMEEUnit getUnitByIdentifier(String identifier) {
        AMEEUnit unit = null;
        if (UidGen.INSTANCE_12.isValid(identifier)) {
            unit = getUnitByUid(identifier);
        }
        if (unit == null) {
            unit = getUnitBySymbol(identifier);
        }
        return unit;
    }

    @Override
    public AMEEUnit getUnitByUid(String uid) {
        return dao.getUnitByUid(uid);
    }

    @Override
    public AMEEUnit getUnitBySymbol(String symbol) {
        return dao.getUnitBySymbol(symbol);
    }

    /**
     * Returns true if the symbol of the supplied Unit is unique.
     *
     * @param unit to check for uniqueness
     * @return true if the Unit has a unique symbol
     */
    @Override
    public boolean isUnitUniqueBySymbol(AMEEUnit unit) {
        return dao.isUnitUniqueBySymbol(unit);
    }

    @Override
    public void persist(AMEEUnit unit) {
        dao.persist(unit);
    }

    @Override
    public void remove(AMEEUnit unit) {
        unit.setStatus(AMEEStatus.TRASH);
    }

    // For tests.

    public void setUnitServiceDAO(UnitServiceDAO dao) {
        this.dao = dao;
    }
}
