package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;

/**
 * Test for ChangeMessageUtils.
 * 
 * @author John
 *
 */
public class ChangeMessageUtilsTest {
	
	@Test
	public void testRoundTrip(){
		// Start with a list
		List<ChangeMessage> list = new ArrayList<ChangeMessage>();
		for(int i=0; i<3; i++){
			ChangeMessage cm = new ChangeMessage();
			cm.setChangeType(ChangeType.CREATE);
			cm.setObjectEtag("etag"+i);
			cm.setObjectType(ObjectType.ENTITY);
			cm.setObjectId("syn"+i);
			list.add(cm);
		}
		// Make the round trip
		List<DBOChange> dboList = ChangeMessageUtils.createDBOList(list);
		assertNotNull(dboList);
		// Now return
		List<ChangeMessage> clone = ChangeMessageUtils.createDTOList(dboList);
		assertNotNull(clone);
		assertEquals("Failed to make a round trip without data loss", list, clone);
	}
	
	@Test
	public void testValidateChangeMessage() {
		ChangeMessage valid = createValidChange();
		// call under test
		ChangeMessageUtils.validateChangeMessage(valid);
	}
	
	@Test
	public void testCreateDBOWithVersionNull() {
		ChangeMessage dto = createValidChange();
		dto.setObjectVersion(null);
		// call under test
		DBOChange dbo =  ChangeMessageUtils.createDBO(dto);
		assertNotNull(dbo);
		assertEquals(new Long(DBOChange.DEFAULT_NULL_VERSION), dbo.getObjectVersion());
	}
	
	@Test
	public void testCreateDBOWithVersion() {
		ChangeMessage dto = createValidChange();
		dto.setObjectVersion(333L);
		// call under test
		DBOChange dbo =  ChangeMessageUtils.createDBO(dto);
		assertNotNull(dbo);
		assertEquals(dto.getObjectVersion(), dbo.getObjectVersion());
	}
	
	@Test
	public void testCreateDTONullVersion() {
		DBOChange dbo = createValidDBO();
		dbo.setObjectVersion(DBOChange.DEFAULT_NULL_VERSION);
		// call under test
		ChangeMessage dto = ChangeMessageUtils.createDTO(dbo);
		assertNotNull(dto);
		assertNull(dto.getObjectVersion());
	}
	
	@Test
	public void testCreateDTOVersion() {
		DBOChange dbo = createValidDBO();
		dbo.setObjectVersion(444L);
		// call under test
		ChangeMessage dto = ChangeMessageUtils.createDTO(dbo);
		assertNotNull(dto);
		assertEquals(dbo.getObjectVersion(), dto.getObjectVersion());
	}
	
	@Test
	public void testCompareWithNull() {
		assertEquals(0, ChangeMessageUtils.compareWithNull(null, null));
		assertEquals(1, ChangeMessageUtils.compareWithNull(123L, null));
		assertEquals(-1, ChangeMessageUtils.compareWithNull(null, 123L));
		assertEquals(0, ChangeMessageUtils.compareWithNull(123L, 123L));
		assertEquals(-1, ChangeMessageUtils.compareWithNull(122L, 123L));
		assertEquals(1, ChangeMessageUtils.compareWithNull(123L, 122L));
	}
	
	@Test
	public void testcCompareIdVersionType() {
		ChangeMessage one = createValidChange();
		ChangeMessage two = createValidChange();
		assertEquals(0,ChangeMessageUtils.compareIdVersionType(one, two));
		one.setObjectId("syn456");
		assertEquals(1,ChangeMessageUtils.compareIdVersionType(one, two));
		two.setObjectId("syn789");
		assertEquals(-1,ChangeMessageUtils.compareIdVersionType(one, two));
		one.setObjectId(two.getObjectId());
		one.setObjectVersion(2L);
		two.setObjectVersion(2L);
		assertEquals(0,ChangeMessageUtils.compareIdVersionType(one, two));
		one.setObjectVersion(3L);
		assertEquals(1,ChangeMessageUtils.compareIdVersionType(one, two));
		two.setObjectVersion(4L);
		assertEquals(-1,ChangeMessageUtils.compareIdVersionType(one, two));
		one.setObjectVersion(two.getObjectVersion());
		one.setObjectType(ObjectType.ACTIVITY);
		two.setObjectType(ObjectType.ENTITY);
		assertTrue(ChangeMessageUtils.compareIdVersionType(one, two) > 0);
		one.setObjectType(ObjectType.ENTITY);
		two.setObjectType(ObjectType.ACTIVITY);
		assertTrue(ChangeMessageUtils.compareIdVersionType(one, two) < 0);
	}
	
	/**
	 * Helper to create a valid ChangeMessage
	 * @return
	 */
	public static ChangeMessage createValidChange() {
		ChangeMessage change = new ChangeMessage();
		change.setChangeNumber(1L);
		change.setObjectId("syn123");
		change.setObjectVersion(null);
		change.setObjectType(ObjectType.ENTITY);
		change.setChangeType(ChangeType.CREATE);
		return change;
	}
	
	/**
	 * Helper to create a valid DBO.
	 * @return
	 */
	public static DBOChange createValidDBO() {
		DBOChange dbo = new DBOChange();
		dbo.setObjectType(ObjectType.ACCESS_APPROVAL.name());
		dbo.setObjectId(123L);
		dbo.setChangeType(ChangeType.CREATE.name());
		dbo.setObjectVersion(DBOChange.DEFAULT_NULL_VERSION);
		return dbo;
	}

}
