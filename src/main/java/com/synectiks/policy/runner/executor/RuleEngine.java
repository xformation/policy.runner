/**
 * 
 */
package com.synectiks.policy.runner.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.entities.EvalPolicyRuleResult;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IUtilities;
import com.synectiks.policy.runner.parsers.Expression;
import com.synectiks.policy.runner.parsers.Key;
import com.synectiks.policy.runner.parsers.Value;
import com.synectiks.policy.runner.utils.IConstants.Keywords;

/**
 * Class to evaluate eval rule checks on data provided.
 * @author Rajesh Upadhyay
 */
public class RuleEngine {

	private static Logger logger = LoggerFactory.getLogger(RuleEngine.class);

	private static String[] QUERIES = {
			"Rajesh", // Full text search in all fields- { "query_string": { "query": "abc" } } or { "match": { "_all": "abc" } }
			"has onRoll", // Check if field exists
			"gender = 'F'",
			"age > 10",
			"salary < 2500",
			"age != 40",
			"address.pin >= 302030",
			"address.pin <= 302030",
			"name regex('^R.*esh')",
			"onRoll isNull",
			"address.street isEmpty",
			"onRoll isNotNull",
			"address.street isNotEmpty",
			"*ame Raj", // Search value in key fields with wildcard key name
			"[name, fatherName] +\"Rajesh\"", // Search Rajesh AND Kumar  in key fields multi_match
			"name = 'R?j*'", // Like
			"address.city != 'J??p*r'", // NOT Like
			"name = (Rajesh, Rajani, Ram)", // IN
			"name != (Rajesh, Rajani, Ram)", // NOT IN
			"doj <= toDate('2018-08-15 13:20:30')", // Default format: yyyy-MM-dd HH:mm:ss
			"dob >= toDate('2000-01-01 00:00:00', 'yyyy-MM-dd HH:mm:ss')",
			"doj = toDate('01/09/2009 13:20:30.000', 'dd/MM/yyyy hh:mm:ss.SSS')",
			"name = Rajesh OR address.city = Jaipur",
			"name = 'Rajesh Upadhyay' AND fatherName = 'Ramesh Chand Sharma'",
			"name = Raj AND (gender = M OR gender = F)",
			"(name = Rajesh OR name = Rajani) AND (gender = M OR onRoll = true)",
			"gender.length = 1"
	};
	private static String[] VALS = {
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

	private static void executeCheck() {
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

	private static class RuleResult {
		
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

	private static class CheckWrapper {

		private List<CheckWrapper> chkWrappers;
		private List<Check> andChks;
		private List<Check> orChks;
		private Check chk;

		public static CheckWrapper parse(String input) {
			return CheckWrapperParser.parse(input);
		}

	}

	private static class CheckWrapperParser {

		public static CheckWrapper parse(String query) {
			CheckWrapper wrapper = new CheckWrapper();
			query = IUtils.refineQueryString(query);
			if (!IUtils.isNullOrEmpty(query)) {
				List<String> groups = getGroups(query);
			}
			return wrapper;
		}

		private static List<String> getGroups(String qry) {
			List<String> lst = null;
			while (IUtilities.isStartWithGroup(qry)) {
				Keywords grp = IUtilities.getStartWithGroup(qry);
				if (grp == Keywords.CptlBrkt) {
					// This is case of multi_search process multi-field search;
					//return processMultiMatchSearch(qry);
				} else if (grp == Keywords.SmlBrkt) {
					
				}
			}
			return lst;
		}

	}

}
