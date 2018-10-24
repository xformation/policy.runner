package com.synectiks.policy.runner.executor;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.entities.Rule;
import com.synectiks.commons.entities.PolicyRuleResult;
import com.synectiks.commons.entities.SourceEntity;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.PolicyApplication;
import com.synectiks.policy.runner.repositories.RuleRepository;
import com.synectiks.policy.runner.translators.QueryParser;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IConstants.Keywords;
import com.synectiks.policy.runner.utils.IUtilities;

public class PolicyExecutor {

	private static final Logger logger = LoggerFactory.getLogger(PolicyExecutor.class);

	private static final int PG_SIZE = 100;
	
	@Autowired
	private Environment env;
	@Autowired
	private RestTemplate rest;
	@Autowired
	private RuleRepository rules;

	private Policy policy;

	public PolicyExecutor(Policy policy) {
		assertNotNull("Policy should not be null", policy);
		this.policy = policy;
		if (IUtils.isNull(env)) {
			env = PolicyApplication.getBean(Environment.class);
		}
		if (IUtils.isNull(rest)) {
			rest = PolicyApplication.getBean(RestTemplate.class);
		}
		if (IUtils.isNull(rules)) {
			rules = PolicyApplication.getBean(RuleRepository.class);
		}
	}

	/**
	 * Method to execute policy to collect and store results.
	 * @return
	 */
	public PolicyRuleResult execute() {
		if (!IUtils.isNull(policy)) {
			logger.info("Executing policy: " + policy.getName());
			JSONObject query = createPolicyQuery();
			return executeQuery(query, 1);
		}
		return null;
	}

	/**
	 * Method to call search service to execute elastic query.
	 * @param query
	 * @param page
	 * @return
	 */
	private PolicyRuleResult executeQuery(JSONObject query, int page) {
		String srchUlr = IUtilities.getSearchUrl(env, IConstants.ELASTIC_QUERY);
		logger.info("searchUrl: " + srchUlr);
		Map<String, Object> params = IUtils.getRestParamMap(
				IConsts.PRM_QUERY, query.toString(),
				IConsts.PRM_CLASS, SourceEntity.class.getName(),
				IConsts.PRM_PAGE, String.valueOf(page),
				IConsts.PRM_PAGE_SIZE, String.valueOf(PG_SIZE));
		logger.info("Request: " + params);
		PolicyRuleResult res = null;
		try {
			res = IUtils.sendPostRestRequest(rest,
					srchUlr, null, PolicyRuleResult.class,
					params, MediaType.APPLICATION_FORM_URLENCODED);
			logger.info("Indexing response: " + res);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return res;
	}

	/**
	 * Method to translate policy into elastic query.
	 * @return
	 */
	private JSONObject createPolicyQuery() {
		if (!IUtils.isNull(policy) && !IUtils.isNull(policy.getRules()) &&
				policy.getRules().size() > 0) {
			JSONArray arr = new JSONArray();
			for (String ruleid : policy.getRules()) {
				logger.info("rule id: " + ruleid);
				// Reload rule by id
				Rule rule = rules.findById(ruleid);
				List<JSONObject> checks = processRule(rule);
				for (JSONObject check : checks) {
					arr.put(check);
				}
			}
			if (arr.length() > 0) {
				JSONObject json = new JSONObject();
				try {
					json.put(IConstants.QUERY, IUtilities.createBoolQueryFor(Keywords.AND, arr));
				} catch (JSONException e) {
					logger.error(e.getMessage(), e);
				}
				return json;
			}
		}
		return null;
	}

	/**
	 * Method to process all checks to produce elastic queries.
	 * @param rule
	 * @return
	 */
	private List<JSONObject> processRule(Rule rule) {
		List<JSONObject> list = new ArrayList<>();
		if (!IUtils.isNull(rule) && !IUtils.isNull(rule.getChecks())) {
			logger.info("Processing rule: " + rule.getName());
			for (String check : rule.getChecks()) {
				QueryParser parser = new QueryParser(check);
				JSONObject query = parser.parse();
				if (!IUtils.isNull(query)) {
					list.add(query);
				}
			}
		}
		return list;
	}

}
