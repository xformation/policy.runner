/**
 * 
 */
package com.synectiks.policy.runner.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.entities.PolicyRuleResult;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.executor.PolicyExecutor;
import com.synectiks.policy.runner.repositories.PolicyRepository;
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

	@Autowired
	private PolicyRepository policies;

	/**
	 * API to translate the input query string into elastic DSL query.
	 * @param req
	 * @param res
	 * @param body
	 * @return
	 */
	@RequestMapping(path = "/translate", method = RequestMethod.POST)
	public @ResponseBody ObjectNode translate(HttpServletRequest req,
			HttpServletResponse res, @RequestBody String body) {
		logger.info("body: " + body);
		logger.info("Params: " + req.getParameterMap());
		JSONObject jObj = null;
		try {
			jObj = new JSONObject(body);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		logger.info("query: " + jObj);
		ObjectNode json = null;
		try {
			JSONObject map = new QueryParser(jObj.optString("query")).parse();
			json = IUtils.getObjectFromValue(map.toString(), ObjectNode.class);
			logger.info("output: " + json);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		return json;
	}

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
		return ResponseEntity.status(HttpStatus.OK).body(json);
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
		return ResponseEntity.status(HttpStatus.OK).body(suggestions);
	}

	/**
	 * Api to parse input query into elastic-search DSL query format.
	 * @param query
	 * @return
	 */
	@RequestMapping(path = IConstants.API_EXECUTE, method = RequestMethod.POST)
	public ResponseEntity<Object> execute(String policyId) {
		PolicyRuleResult json = null;
		logger.info("Policy to execute: " + policyId);
		if (!IUtils.isNullOrEmpty(policyId)) {
			try {
				Policy policy = policies.findById(policyId);
				PolicyExecutor executor = new PolicyExecutor(policy);
				json = executor.execute();
			} catch (Throwable th) {
				logger.error(th.getMessage(), th);
				return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
						.body(IUtils.getFailedResponse(th.getMessage()));
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(json);
		
	}
}
