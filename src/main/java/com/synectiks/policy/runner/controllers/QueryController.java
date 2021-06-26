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
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.entities.SourceEntity;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.executor.PolicyExecutor;
import com.synectiks.policy.runner.executor.RuleEngine;
import com.synectiks.policy.runner.repositories.PolicyRepository;
import com.synectiks.policy.runner.repositories.RuleRepository;
import com.synectiks.policy.runner.translators.QueryParser;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;
import com.synectiks.policy.runner.utils.OperatorsDesc;

/**
 * @author Rajesh
 */
@RestController
@CrossOrigin
public class QueryController {
	
	private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

	@Autowired
	private PolicyRepository policies;
	@Autowired
	private RuleRepository ruleRepo;

	@Autowired
	private Environment env;
	@Autowired
	private RestTemplate rest;

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
	 * API to return mappings for entity class.
	 * @return
	 */
	@RequestMapping(path = IConstants.API_OPRTORS_BY_TYPE, method = RequestMethod.GET)
	public ResponseEntity<Object> getFieldsMap() {
		OperatorsDesc opDesc = IConstants.Keywords.listFieldsMap();
		return new ResponseEntity<>(opDesc.getMap(), HttpStatus.OK);
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
	 * Api to provide auto complete suggestions for input string.
	 * @param query
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(path = IConstants.API_SUGGEST, method = RequestMethod.POST)
	public ResponseEntity<Object> keySuggestion(String query,
			@RequestParam(name = IConsts.PRM_CLASS, required= false) String cls) {
		List<String> suggestions = new ArrayList<>();
		if (!IUtils.isNullOrEmpty(query)) {
			if (!IUtils.isNullOrEmpty(cls)) {
				List<String> lst = null;
				if (IUtilities.entityFields.containsKey(cls)) {
					lst = IUtilities.entityFields.get(cls);
				} else {
					lst = (List<String>) IUtilities.fillIndexedKeys(rest, env, cls);
					IUtilities.entityFields.put(cls, lst);
				}
				for (String key : lst) {
					if (key.toLowerCase().contains(query.trim().toLowerCase())) {
						suggestions.add(key);
					}
				}
			} else {
				String srcEnt = SourceEntity.class.getName();
				if (!IUtilities.entityFields.isEmpty() &&
						IUtilities.entityFields.containsKey(srcEnt)) {
					for (String key : IUtilities.entityFields.get(srcEnt)) {
						if (!IUtils.isNullOrEmpty(key) &&
								key.toLowerCase().contains(query.toLowerCase())) {
							suggestions.add(key);
						}
					}
				} else {
					return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
							.body(IUtils.getFailedResponse("Failed to load initial kyes"));
				}
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(suggestions);
	}

	/**
	 * Api to execute a policy by its id and generate response
	 * with matching elastic document ids list.
	 * @param policyId
	 * @param noCache send true if no need to save results
	 * @return
	 */
	@RequestMapping(path = IConstants.API_EXECUTE, method = RequestMethod.POST)
	public ResponseEntity<Object> execute(long policyId,
			@RequestParam(name = "noCache", required = false) boolean noCache) {
		List<?> json = null;
		logger.info("Policy to execute: " + policyId);
		if (policyId > 0) {
			try {
				Policy policy = policies.findById(policyId).orElse(null);
				if (!IUtils.isNull(policy)) {
					if (policy.isSearchable()) {
						PolicyExecutor executor = new PolicyExecutor(policy);
						executor.setNoCache(noCache);
						json = executor.execute();
					} else {
						// Add your logic to execute non searchable query.
						RuleEngine re = new RuleEngine(env, rest, ruleRepo, !noCache);
						json = re.execute(policy);
					}
				} else {
					throw new Exception("Policy not found for id: " + policyId);
				}
			} catch (Throwable th) {
				logger.error(th.getMessage(), th);
				return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
						.body(IUtils.getFailedResponse(th.getMessage()));
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(json);
	}

	/**
	 * Api endpoint to execute a query for class or index entities.
	 * @param qry
	 * @param cls
	 * @param index
	 * @return
	 */
	@RequestMapping(path = "/executeQry", method = RequestMethod.POST)
	public ResponseEntity<Object> executeSynQry(String qry,
			@RequestParam(required = false) String cls,
			@RequestParam(required = false) String index,
			@RequestParam(required = false) String type) {
		Object res = null;
		try {
			if (IUtils.isNullOrEmpty(index)) {
				if (IUtils.isNullOrEmpty(cls)) {
					throw new Exception("cls/index name is required.");
				}
			}
			RuleEngine re = new RuleEngine(env, rest, ruleRepo);
			res = re.execute(qry, cls, index, type);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}
}
