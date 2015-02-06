package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserPreferencesTest {
	
	private List<Long> toDelete;

	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Before
	public void setUp() throws Exception {
		toDelete = new ArrayList<Long>();
	}

	@After
	public void tearDown() throws Exception {
		for (Long id: toDelete) {
			dboBasicDao.deleteObjectByPrimaryKey(DBOUserPreferences.class,
					new SinglePrimaryKeySqlParameterSource(id));		}
	}

	@Test
	public void testCRUD() throws DatastoreException, NotFoundException {
		DBOUserPreferences dbo = new DBOUserPreferences();
		dbo.setEtag("etag");
		dbo.setOwnerId(100L);
		dbo.setPropsBlob("someBlobContent".getBytes());
		
		DBOUserPreferences dbo2 = dboBasicDao.createNew(dbo);
		assertNotNull(dbo2);
		assertEquals(dbo, dbo2);
		
		dbo2 = dboBasicDao.getObjectByPrimaryKey(DBOUserPreferences.class,
				new SinglePrimaryKeySqlParameterSource(100L));
		assertNotNull(dbo2);
		assertEquals(dbo, dbo2);
		
		String str = "someNewBlobContent";
		dbo2.setPropsBlob(str.getBytes());
		dbo2.setEtag("etag2");
		dboBasicDao.update(dbo2);
		dbo2 = dboBasicDao.getObjectByPrimaryKey(DBOUserPreferences.class,
				new SinglePrimaryKeySqlParameterSource(100L));
		assertNotNull(dbo2);
		assertEquals(str, new String(dbo2.getPropsBlob()));

		// Delete it
		boolean res = dboBasicDao.deleteObjectByPrimaryKey(DBOUserPreferences.class,
				new SinglePrimaryKeySqlParameterSource(100L));
		assertTrue("Failed to delete the type created", res);
		
	}

}
