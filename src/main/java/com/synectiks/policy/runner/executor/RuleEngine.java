/**
 * 
 */
package com.synectiks.policy.runner.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.EvalPolicyRuleResult;
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.entities.Rule;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.parsers.Expression;
import com.synectiks.policy.runner.repositories.RuleRepository;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * Class to evaluate eval rule checks on data provided.
 * @author Rajesh Upadhyay
 */
public class RuleEngine {

	private static Logger logger = LoggerFactory.getLogger(RuleEngine.class);

	@Autowired
	private Environment env;
	@Autowired
	private RestTemplate rest;
	@Autowired
	private RuleRepository ruleRepo;

	private static String[] QUERIES = {
			"Rajesh", // Full text search in all fields- { "query_string": { "query": "abc" } } or { "match": { "_all": "abc" } }
			"has onRoll", // Check if field exists
			"gender = 'F'",
			"age > 10",
			"salary < 2500",
			"age != 40",
			"address.pin >= 302030",
			"address.pin <= 302030",
			"name regex('^R.*sh.*')",
			"onRoll isNull",
			"address.street isEmpty",
			"onRoll isNotNull",
			"address.street isNotEmpty",
			"*ame Raj", // Search value in key fields with wildcard key name
			"[name, fatherName] +\"Rajesh\"", // Search Rajesh AND Kumar  in key fields multi_match
			"name = 'R.j.*'", // Like
			"address.city != 'J..p.*r'", // NOT Like
			"name = (Rajesh, Rajani, Ram)", // IN
			"name != (Rajesh, Rajani, Ram)", // NOT IN
			"doj <= toDate('2018-08-15 13:20:30')", // Default format: yyyy-MM-dd HH:mm:ss
			"dob >= toDate('1999-01-01 00:00:00', 'yyyy-MM-dd HH:mm:ss')",
			"doj = toDate('01/09/2009 00:00:00.000', 'dd/MM/yyyy hh:mm:ss.SSS')",
			"name = Rajesh OR address.city = Jaipur",
			"name = 'Rajesh Upadhyay' AND fatherName = 'Ramesh Chand Sharma'",
			"name = Raj AND (gender = M OR gender = F)",
			"(name = Rajesh OR name = Rajani) AND (gender = M OR onRoll = true)",
			"onRoll = true",
			"gender.length = 1"
	};
	public static String[] VALS = {
			"value", // Full text search in all fields- { "query_string": { "query": "abc" } } or { "match": { "_all": "abc" } }
			"'value'",
			"10",
			"regex('^R.*esh$')",
			"+\"Rajesh Kumar\"", // Search Rajesh AND Kumar  in key fields multi_match
			"'A?c*fg'", // Like
			"(value1, value2, value3)", // IN
			"toDate('2018-08-15 13:20:30')", // Default format: yyyy-MM-dd HH:mm:ss
			"toDate('2018-08-15 13:20:30', 'yyyy-MM-dd HH:mm:ss')"
	};

	private static final Check[] checks = {
			new Check("isdCode", null, "<",  100),
			new Check("isdCode", null, "<=",  1000),
			new Check("isdCode", null, ">=",  100),
			new Check("isdCode", null, "=",  1000),
			new Check("isdCode", null, ">",  10000),
			new Check("countryName", null, "=",  "India"),
			new Check("countryName", "length", "<",  10) };

	private static List<JSONObject> listEntities;

	static {
		//Load entities from json file
		File fJson = new File(RuleEngine.class.getClassLoader().getResource("employees.json").getFile());
		try (BufferedReader br = new BufferedReader(new FileReader(fJson))) {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			String[] arr = IUtils.getArrayFromJsonString(sb.toString());
			listEntities = new ArrayList<>();
			for (String obj : arr) {
				listEntities.add(new JSONObject(obj));
			}
		} catch(Throwable th) {
			th.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		//executeCheck();
		//CheckWrapper.parse("(root.node.key1 = value1 OR key2 = value2) AND (key3 = value3 OR key4 = value4)");
		/*String[] in = {"1/10/2001", "23/1/2200", "1.10.2001", "23-1-2002", "May 23, 2004", "2003/03/05", "2003-03-05", "2003.03.05"};
		for (String dt : in) {
			logger.info(dt + " is date?: " + dt.matches(
					"^(\\w{3}|\\d{1,4})[\\.\\-/\\s]\\d{1,2}[,\\-/\\.]\\s?\\d{1,4}"));
		}*/
		// Extract all keys object
		/*for (String in : QUERIES) {
			Key.parse(in);
		}*/
		// Extract all keys object
		/*for (String in : VALS) {
			Value.parse(in);
		}*/
		List<Expression> lstExps = new ArrayList<>();
		Long pid = 1l, rid = 1l;
		// Extract Expressions from input string query
		for (String in : QUERIES) {
			lstExps.add(Expression.parse(in, pid, rid ++));
		}
		// Execute the expressions to generate result.
		for (Expression exp : lstExps) {
			logger.info("Query: " + QUERIES[(int) (exp.getRuleId() - 1)]);
			logger.info("Exp: " + exp);
			for (JSONObject entity : listEntities) {
				EvalPolicyRuleResult result = evaluateExpressionForEntity(exp, entity);
				logger.info("Res: " + result);
			}
		}
	}

	private static EvalPolicyRuleResult evaluateExpressionForEntity(Expression exp,
			JSONObject entity) {
		return exp.evaluate(entity);
	}

	public static void executeCheck() {
		List<RuleResult> results = new ArrayList<>();
		if (!IUtils.isNull(listEntities)) {
			for (JSONObject entity : listEntities) {
				Long indx = 1l;
				for (Check check : checks) {
					try {
						RuleResult res = new RuleResult(entity.getLong("id"),
								indx ++, check.evaluate(entity));
						results.add(res);
						//logger.info(res);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				//logger.info("1. Result: " + results);
			}
			//logger.info("2. Result: " + results);
		}
		logger.info("3. Result: " + results);
	}

	public static class RuleResult {
		
		private Long docId;
		private Long ruleId;
		private boolean result;

		public RuleResult(Long docId, Long ruleId, boolean result) {
			//logger.info(docId + ", " + ruleId + ", " + result);
			this.docId = docId;
			this.ruleId = ruleId;
			this.result = result;
		}

		public Long getDocId() {
			return docId;
		}

		public void setDocId(Long docId) {
			this.docId = docId;
		}

		public Long getRuleId() {
			return ruleId;
		}

		public void setRuleId(Long ruleId) {
			this.ruleId = ruleId;
		}

		public boolean isResult() {
			return result;
		}

		public void setResult(boolean result) {
			this.result = result;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("{ ");
			if (docId != null)
				builder.append("\"docId\": ").append(docId).append(", ");
			if (ruleId != null)
				builder.append("\"ruleId\": ").append(ruleId).append(", ");
			builder.append("\"result\": ").append(result).append(" }");
			return builder.toString();
		}

	}

	public static class Check {

		private String key;
		private String function;
		private String operator;
		private Object value;

		public Check(String key, String func, String op, Object value) {
			this.key = key;
			this.function = func;
			this.operator = op;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getFunction() {
			return function;
		}

		public void setFunction(String function) {
			this.function = function;
		}

		public String getOperator() {
			return operator;
		}

		public void setOperator(String operator) {
			this.operator = operator;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public boolean evaluate(JSONObject entity) throws JSONException {
			Object val = entity.get(key);
			if (!IUtils.isNullOrEmpty(function)) {
				val = IUtilities.evalFunction(entity, key, function);
			}
			if (!IUtils.isNullOrEmpty(operator)) {
				return IUtilities.evalOperator(val, operator, value);
			}
			return false;
		}
	}

	/**
	 * Method to execute a policy for an entity and reproduce the evaluation results.
	 * @param policy
	 * @return
	 */
	public List<?> execute(Policy policy) {
		List<EvalPolicyRuleResult> lst = new ArrayList<>();
		if (!IUtils.isNull(policy) &&
				!IUtils.isNull(policy.getRules()) && policy.getRules().size() > 0) {
			String cls = policy.getEntity();
			if (IUtils.isNullOrEmpty(cls)) {
				logger.error("Class for policy is null or empty");
				return lst;
			}
			List<Expression> exprs = getExpressions(policy);
			if (!IUtils.isNull(exprs) && exprs.size() > 0) {
				List<String> lstEntities = getIndexDocs(
						IConstants.ELASTIC_INDX_DOCS, cls, null, null);
				if (!IUtils.isNull(lstEntities)) {
					for (String entity : lstEntities) {
						for (Expression exp : exprs) {
							lst.add(exp.evaluate(IUtils.getJSONObject(entity)));
						}
					}
				}
			}
		}
		return lst;
	}

	/**
	 * Method to check all policy rule's checks and convert into Expression object.
	 * @param policy
	 * @return
	 */
	private List<Expression> getExpressions(Policy policy) {
		List<Expression> exprs = getExpressions(policy);
		for (Long rId : policy.getRules()) {
			if (!IUtils.isNull(rId) && rId.longValue() > 0) {
				Optional<Rule> rule = ruleRepo.findById(rId);
				if (rule.isPresent() && !IUtils.isNull(rule.get().getChecks())) {
					List<String> chks = rule.get().getChecks();
					for (String chk : chks) {
						exprs.add(Expression.parse(chk, policy.getId(), rId));
					}
				}
			}
		}
		return exprs;
	}

	/**
	 * Method to get list of documents by url and params.
	 * @param url
	 * @param cls
	 * @param index
	 * @param type
	 * @return
	 */
	private List<String> getIndexDocs(String url, String cls, String index, String type) {
		List<String> lst = null;
		String srchUlr = IUtilities.getSearchUrl(env, url);
		logger.info("searchUrl: " + srchUlr);
		List<String> prms = new ArrayList<>();
		if (!IUtils.isNull(cls)) {
			prms.add(IConsts.PRM_CLASS);
			prms.add(cls);
		}
		if (!IUtils.isNull(index)) {
			prms.add(IConstants.PRM_INDEX);
			prms.add(index);
		}
		if (!IUtils.isNull(type)) {
			prms.add(IConstants.PRM_TYPE);
			prms.add(type);
		}
		Map<String, Object> params = IUtils.getRestParamMap(prms.toArray());
		logger.info("Request: " + params);
		try {
			lst = IUtils.sendGetRestReq(rest, srchUlr, null, params);
			logger.info("Indexing response size: " +
					(IUtils.isNull(lst) ? "0" : lst.size()));
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return lst;
	}

	/**
	 * Method to execute a query for specified class or index name.
	 * @param qry
	 * @param cls
	 * @param index
	 * @param type 
	 * @return
	 */
	public Object execute(String qry, String cls, String index, String type) {
		Expression exp = Expression.parse(qry, -1L, -1L);
		if (!IUtils.isNull(exp)) {
			List<EvalPolicyRuleResult> lst = new ArrayList<>();
			List<String> lstEntts = getIndexDocs(
					IConstants.ELASTIC_INDX_DOCS, cls, index, type);
			if (!IUtils.isNull(lstEntts)) {
				for (String ent : lstEntts) {
					lst.add(exp.evaluate(IUtils.getJSONObject(ent),
							index, cls));
				}
			}
			return lst;
		}
		return null;
	}

}
