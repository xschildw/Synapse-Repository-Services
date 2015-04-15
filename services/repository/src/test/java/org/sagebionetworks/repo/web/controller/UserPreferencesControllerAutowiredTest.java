package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.web.service.UserPreferencesService;
import org.springframework.beans.factory.annotation.Autowired;

public class UserPreferencesControllerAutowiredTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private UserPreferencesService userPreferencesService;
	
	private Long adminUserId;

	HttpServletRequest mockRequest;

	@Before
	public void setUp() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		assertNotNull(userPreferencesService);

		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testUserPreferencesCRUD() {
	}

}
