package com.amee.service.unit;

import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEStatus;
import com.amee.domain.unit.AMEEUnitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitServiceDAO dao;

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

    @Override
    public void persist(AMEEUnitType unitType) {
        dao.persist(unitType);
    }

    @Override
    public void remove(AMEEUnitType unitType) {
        unitType.setStatus(AMEEStatus.TRASH);
    }
}
