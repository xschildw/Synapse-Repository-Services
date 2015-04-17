package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerInfo(displayName="User Preferences Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class UserPreferencesController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Get the user preferences of the caller
	 * @param userId
	 * @param request
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PREFERENCES, method = RequestMethod.GET)
	public @ResponseBody 
		UserPreferences getUserPreferences(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, 
			HttpServletRequest request) throws NotFoundException {
		return serviceProvider.getUserPreferencesService().getUserPreferences(userId);
	}

	/**
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.USER_PREFERENCES, method = RequestMethod.POST)
	public @ResponseBody 
		UserPreferences createUserPreferences(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody UserPreferences userPreferences) throws NotFoundException, IOException {
		return serviceProvider.getUserPreferencesService().createUserPreferences(userId, userPreferences);
	}

	/**
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PREFERENCES, method = RequestMethod.PUT)
	public @ResponseBody 
		UserPreferences updateUserPreferences(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody UserPreferences userPreferences) throws NotFoundException, IOException {
		return serviceProvider.getUserPreferencesService().updateUserPreferences(userId, userPreferences);
	}

	/**
	 * 
	 * @param userId
	 * @param request
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.USER_PREFERENCES, method = RequestMethod.DELETE)
	public @ResponseBody 
		void deleteUserPreferences(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, 
			HttpServletRequest request) throws NotFoundException {
		serviceProvider.getUserPreferencesService().deleteUserPreferences(userId);
	}

}
