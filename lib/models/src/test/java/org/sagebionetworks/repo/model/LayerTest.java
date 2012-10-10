package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class LayerTest {
	
	@Test
	public void testRoundTripLayer() throws JSONObjectAdapterException {
		Data l1 = new Data();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		l1.setAccessControlList("/acl");
		l1.setAnnotations("/annotations");
		l1.setCreatedBy("createdBy");
		l1.setCreatedOn(d);
		l1.setDescription("description");
		l1.setEtag("1");
		l1.setId("1");
		l1.setModifiedBy("modifiedBy");
		l1.setModifiedOn(d);
		l1.setName("name");
		l1.setParentId("0");
		l1.setUri("uri");

		l1.setVersionComment("versionComment");
		l1.setVersionLabel("versionLabel");
		l1.setVersionNumber(1L);
		l1.setVersionUrl("/versions/1");
		l1.setVersions("/versions");
		l1.setContentType("text");
		l1.setMd5("01234567890123456789012345678901");
		
		List<LocationData> ll = new ArrayList<LocationData>();
		LocationData l = new LocationData();
		l.setPath("path");
		l.setType(LocationTypeNames.sage);
		ll.add(l);
		l1.setLocations(ll);

		l1.setNumSamples(1000L);
		l1.setPlatform("platform");
		l1.setTissueType("tissueType");
		l1.setType(LayerTypeNames.E);

		adapter1 = l1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Data l2 = new Data(adapter2);
		
		assertEquals(l1, l2);
		return;
	}
	
	@Test
	public void testIsLocationable1() throws Exception {
		Data d = new Data();
		boolean v;
		
		v = (Locationable.class.isInstance(d));
		assertTrue(v);
		
		v = (d instanceof Locationable);
		assertTrue(v);
		
		v = (Locationable.class.isAssignableFrom(Data.class));
		assertTrue(v);
		
		Object o = new Data();
		
		v = (Locationable.class.isInstance(o));
		assertTrue(v);
		
		v = (o instanceof Locationable);
		assertTrue(v);
		
	}
	
	@Test
	public void testIsLocationable2() throws Exception {
		boolean v;
		Object o;
		
		EntityType et = EntityType.valueOf("layer");
		EntityTypeMetadata etm = et.getMetadata();
		String cn = etm.getEntityType();
		// o's class is org.sagebionetworks.repo.model.Data
		o = Class.forName(cn).newInstance();
		String s = o.getClass().getName();
		assertEquals("org.sagebionetworks.repo.model.Data", s);
		
		v = (Locationable.class.isInstance(o));
		assertTrue(v);
		
		v = (o instanceof Locationable);
		assertTrue(v);
		
	}

}
