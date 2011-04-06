package com.amee.service.unit;

import com.amee.domain.unit.AMEEUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnitServiceTest {

    @Test
    public void canSortUnitsByInternalSymbols() {

        // Create list of units to check for sort.
        // Sort order should be: a, b, c
        List<AMEEUnit> unitsBefore = new ArrayList<AMEEUnit>();
        unitsBefore.add(new AMEEUnit("Unit 1", "c"));
        unitsBefore.add(new AMEEUnit("Unit 2", "b"));
        unitsBefore.add(new AMEEUnit("Unit 3", "a"));

        // Setup UnitServiceDAO.
        UnitServiceDAO dao = mock(UnitServiceDAO.class);
        when(dao.getUnits(null)).thenReturn(unitsBefore);

        // Setup UnitService with UnitServiceDAO.
        UnitService unitService = new UnitServiceImpl();
        unitService.setUnitServiceDAO(dao);

        // Check the units after sorting.
        List<AMEEUnit> unitsAfter = unitService.getUnits();
        assertEquals("a", unitsAfter.get(0).getSymbol());
        assertEquals("b", unitsAfter.get(1).getSymbol());
        assertEquals("c", unitsAfter.get(2).getSymbol());
    }

    @Test
    public void canSortUnitsByExternalSymbols() {

        // Create list of units to check for sort.
        // Sort order should be: d, e, f
        List<AMEEUnit> unitsBefore = new ArrayList<AMEEUnit>();
        unitsBefore.add(new AMEEUnit("Unit 1", "a", "f"));
        unitsBefore.add(new AMEEUnit("Unit 2", "c", "e"));
        unitsBefore.add(new AMEEUnit("Unit 3", "b", "d"));

        // Setup UnitServiceDAO.
        UnitServiceDAO dao = mock(UnitServiceDAO.class);
        when(dao.getUnits(null)).thenReturn(unitsBefore);

        // Setup UnitService with UnitServiceDAO.
        UnitService unitService = new UnitServiceImpl();
        unitService.setUnitServiceDAO(dao);

        // Check the units after sorting.
        List<AMEEUnit> unitsAfter = unitService.getUnits();
        assertEquals("d", unitsAfter.get(0).getSymbol());
        assertEquals("e", unitsAfter.get(1).getSymbol());
        assertEquals("f", unitsAfter.get(2).getSymbol());
    }


    @Test
    public void canSortUnitsByInternalAndExternalSymbols() {

        // Create list of units to check for sort.
        // Sort order should be: c, d, f
        List<AMEEUnit> unitsBefore = new ArrayList<AMEEUnit>();
        unitsBefore.add(new AMEEUnit("Unit 1", "a", "f"));
        unitsBefore.add(new AMEEUnit("Unit 2", "c"));
        unitsBefore.add(new AMEEUnit("Unit 3", "b", "d"));

        // Setup UnitServiceDAO.
        UnitServiceDAO dao = mock(UnitServiceDAO.class);
        when(dao.getUnits(null)).thenReturn(unitsBefore);

        // Setup UnitService with UnitServiceDAO.
        UnitService unitService = new UnitServiceImpl();
        unitService.setUnitServiceDAO(dao);

        // Check the units after sorting.
        List<AMEEUnit> unitsAfter = unitService.getUnits();
        assertEquals("c", unitsAfter.get(0).getSymbol());
        assertEquals("d", unitsAfter.get(1).getSymbol());
        assertEquals("f", unitsAfter.get(2).getSymbol());
    }
}
