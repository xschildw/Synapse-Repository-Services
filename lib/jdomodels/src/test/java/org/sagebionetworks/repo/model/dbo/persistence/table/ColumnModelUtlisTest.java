package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ColumnModelUtlisTest {
	
	ColumnModel original;
	
	@Before
	public void before(){
		original = new ColumnModel();
		original.setId("123");
		original.setName("Name");
		original.setDefaultValue("DefaultValue");
		original.setMaximumSize(444l);
		original.setColumnType(ColumnType.FILEHANDLEID);
		original.setEnumValues(new LinkedList<String>());
		original.getEnumValues().add("Fox");
		original.getEnumValues().add("Trot");
		original.getEnumValues().add("Alpha");
	}
	
	@Test
	public void testNormalize() throws JSONObjectAdapterException{
		// The expected normalized
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("defaultvalue");
		expected.setColumnType(ColumnType.FILEHANDLEID);
		expected.setEnumValues(new LinkedList<String>());
		expected.getEnumValues().add("alpha");
		expected.getEnumValues().add("fox");
		expected.getEnumValues().add("trot");
		expected.setMaximumSize(444L);
		
		// Normalize
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized == original);
		assertEquals(expected, normlaized);
	}
	
	@Test
	public void testNormalizedStringColumnNullSize(){
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("123");
		expected.setColumnType(ColumnType.STRING);
		expected.setMaximumSize(ColumnModelUtlis.DEFAULT_MAX_STRING_SIZE);
		//input
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		// Setting this to null should result in the default size.
		original.setMaximumSize(null);
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized == original);
		assertEquals(expected, normlaized);
	}
	
	@Test
	public void testNormalizedStringColumnSizeTooBig(){
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		original.setMaximumSize(ColumnModelUtlis.MAX_ALLOWED_STRING_SIZE+1);
		try {
			ColumnModelUtlis.createNormalizedClone(original);
			fail("Should have failed as the size is too large");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(ColumnModelUtlis.MAX_ALLOWED_STRING_SIZE.toString()));
		}
	}
	
	@Test
	public void testNormalizedStringColumnJustRight(){
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("123");
		expected.setColumnType(ColumnType.STRING);
		expected.setMaximumSize(ColumnModelUtlis.DEFAULT_MAX_STRING_SIZE-1);
		// input
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		original.setMaximumSize(ColumnModelUtlis.DEFAULT_MAX_STRING_SIZE-1);
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized == original);
		assertEquals(expected, normlaized);
	}
	
	
	@Test
	public void testCalculateHash() throws JSONObjectAdapterException{
		// Create two copies of the original
		ColumnModel clone = ColumnModelUtlis.createNormalizedClone(original);
		clone.setId("999");
		clone.setName(clone.getName().toUpperCase());
		Collections.shuffle(clone.getEnumValues());
		// The clone and the original should produce the same hash.
		String originalHash = ColumnModelUtlis.calculateHash(original);
		String cloneHash = ColumnModelUtlis.calculateHash(clone);
		System.out.println(cloneHash);
		assertEquals("The two objects have the same normalized from so they should have the same hash.",originalHash, cloneHash);
		// Now changing anything should give a new hash
		clone.setDefaultValue("newDefaultValueForTheClone");
		String cloneHash2 = ColumnModelUtlis.calculateHash(clone);
		System.out.println(cloneHash2);
		assertFalse(cloneHash2.equals(cloneHash));
	}
	
	@Test
	public void testRoundTrip() {
		// first calculate the hash of the original object
		String originalHash = ColumnModelUtlis.calculateHash(original);
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		normlaized.setId("123");
		// Now write to DTO
		DBOColumnModel dbo = ColumnModelUtlis.createDBOFromDTO(original);
		assertEquals(new Long(123), dbo.getId());
		assertEquals(originalHash, dbo.getHash());
		// Now make a clone
		ColumnModel clone = ColumnModelUtlis.createDTOFromDBO(dbo);
		assertEquals(normlaized, clone);
	}
	

}
