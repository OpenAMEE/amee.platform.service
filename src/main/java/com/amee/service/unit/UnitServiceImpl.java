package com.amee.service.unit;

import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEStatus;
import com.amee.domain.unit.AMEEUnitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitServiceDAO dao;

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
}
