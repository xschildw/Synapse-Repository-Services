package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the DDLUtilsImpl
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
@Ignore
public class DDLUtilsImplTest {
	
	@Autowired
	DDLUtils ddlUtils;
	
	String tableName = "EXAMPLE_TEST";
	String ddlFile = "Example.sql";
	
	@After
	public void after(){
		// Drop the table
		ddlUtils.dropTable(tableName);
	}

	@Test
	public void testValidateTableExists() throws IOException{
		// Make sure we start without the table
		ddlUtils.dropTable(tableName);
		// the first time this is called the table should not exist.
		boolean result = ddlUtils.validateTableExists(new DBOExample().getTableMapping());
		assertFalse("The first time we called this method it should have created the table", result);
		// the second time the table should already exist
		result = ddlUtils.validateTableExists(new DBOExample().getTableMapping());
		assertTrue("The second time we called this method, the table should have already existed", result);
	}
}
