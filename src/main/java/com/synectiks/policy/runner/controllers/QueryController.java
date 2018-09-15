/**
 * 
 */
package com.synectiks.policy.runner.controllers;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.translators.QueryParser;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * @author Rajesh
 */
@RestController
@CrossOrigin
public class QueryController {
	
	private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

	/**
	 * Api to parse input query into elastic-search DSL query format.
	 * @param query
	 * @return
	 */
	@RequestMapping(path = IConstants.API_PARSE_QUERY, method = RequestMethod.POST)
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

	/**
	 * Api to parse input query into elastic-search DSL query format.
	 * @param query
	 * @return
	 */
	@RequestMapping(path = IConstants.API_SUGGEST, method = RequestMethod.POST)
	public ResponseEntity<Object> keySuggestion(String query) {
		List<String> suggestions = new ArrayList<>();
		if (!IUtils.isNullOrEmpty(query)) {
			try {
				if (!IUtilities.srcEntityFields.isEmpty()) {
					for (String key : IUtilities.srcEntityFields) {
						if (!IUtils.isNullOrEmpty(key) &&
								key.toLowerCase().contains(query.toLowerCase())) {
							suggestions.add(key);
						}
					}
				}
			} catch (Throwable th) {
				logger.error(th.getMessage(), th);
				return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
						.body(IUtils.getFailedResponse(th.getMessage()));
			}
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(suggestions);
	}
}
