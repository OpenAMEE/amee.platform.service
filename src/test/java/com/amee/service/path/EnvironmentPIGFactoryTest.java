package com.amee.service.path;

import com.amee.domain.path.PathItem;
import com.amee.domain.path.PathItemGroup;
import com.amee.service.ServiceTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
public class EnvironmentPIGFactoryTest extends ServiceTest {

    public void init() {
    }

    @Test
    public void canFindPathsInCreationOrderedCategories() {
        EnvironmentPIGFactory o = new EnvironmentPIGFactory(null, null);
        PathItem rootPI = new PathItem(serviceData.DC_ROOT);
        PathItemGroup pig = new PathItemGroup(rootPI);
        o.addDataCategories(pig, serviceData.creationOrderedCategories);
        PathItem a1a2a3 = pig.findByPath("a1/a2/a3", false);
        assertNotNull(a1a2a3);
        assertEquals("/a1/a2/a3", a1a2a3.getFullPath());
        PathItem b1b2b3 = pig.findByPath("b1/b2/b3", false);
        assertNotNull(b1b2b3);
        assertEquals("/b1/b2/b3", b1b2b3.getFullPath());
    }

    @Test
    public void canFindPathsInReverseCreationOrderedCategories() {
        EnvironmentPIGFactory o = new EnvironmentPIGFactory(null, null);
        PathItem rootPI = new PathItem(serviceData.DC_ROOT);
        PathItemGroup pig = new PathItemGroup(rootPI);
        o.addDataCategories(pig, serviceData.reverseCreationOrderedCategories);
        PathItem a1a2a3 = pig.findByPath("a1/a2/a3", false);
        assertNotNull(a1a2a3);
        assertEquals("/a1/a2/a3", a1a2a3.getFullPath());
        PathItem b1b2b3 = pig.findByPath("b1/b2/b3", false);
        assertNotNull(b1b2b3);
        assertEquals("/b1/b2/b3", b1b2b3.getFullPath());
    }
}
