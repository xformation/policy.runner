/**
 * 
 */
package com.synectiks.policy.runner.controllers;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.translators.QueryParser;
import com.synectiks.policy.runner.utils.IConstants;

/**
 * @author Rajesh
 */
@RestController
@RequestMapping(path = IApiController.QRY_API, method = RequestMethod.POST)
@CrossOrigin
public class QueryController {
	
	private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

	@RequestMapping(path = IConstants.API_PARSE_QUERY)
	public ResponseEntity<Object> parseQuery(String query) {
		JSONObject json = null;
		try {
			QueryParser parser = new QueryParser(query);
			json = parser.parse();
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(json);
	}
}
