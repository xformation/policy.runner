/**
 * 
 */
package com.synectiks.policy.runner.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.utils.DateUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.EvalPolicyRuleResult;
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.entities.Rule;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.GelfMessageConfiguration;
import com.synectiks.policy.runner.PolicyApplication;
import com.synectiks.policy.runner.entities.GelfIndexSet;
import com.synectiks.policy.runner.entities.GelfRules;
import com.synectiks.policy.runner.entities.GelfStreams;
import com.synectiks.policy.runner.parsers.Expression;
import com.synectiks.policy.runner.repositories.GelfIndexSetRepository;
import com.synectiks.policy.runner.repositories.GelfRulesRepository;
import com.synectiks.policy.runner.repositories.GelfStreamsRepository;
import com.synectiks.policy.runner.repositories.RuleRepository;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * Class to evaluate eval rule checks on data provided.
 * @author Rajesh Upadhyay
 */
public class RuleEngine {

	private static Logger logger = LoggerFactory.getLogger(RuleEngine.class);

	private Environment env;
	private RestTemplate rest;
	private RuleRepository ruleRepo;
	private String rsltIndxName;

	private boolean saveRes;

	public RuleEngine(Environment env, RestTemplate rest, RuleRepository ruleRepo) {
		this(env, rest, ruleRepo, false);
	}

	public RuleEngine(Environment env, RestTemplate rest, RuleRepository ruleRepo,
			boolean saveRes) {
		this.env = env;
		this.rest = rest;
		this.ruleRepo = ruleRepo;
		this.saveRes = saveRes;
	}

	/**
	 * Method to return result index name with execution time.
	 * @return
	 */
	private void setRsltIndxName(String index, String scanId) {
		if (!IUtils.isNullOrEmpty(index)) {
			if (IUtils.isNullOrEmpty(scanId)) {
				scanId = DateUtils.formatDate(
						new Date(), IConsts.PLAIN_DATE_FORMAT);
			}
			this.rsltIndxName = index + "_" + scanId;
		}
	}

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
			"name = [Rajesh, Rajani, Ram]", // IN
			"name != (Rajesh, Rajani, Ram)", // NOT IN
			"doj <= toDate('2018-08-15 13:20:30')", // Default format: yyyy-MM-dd HH:mm:ss
			"dob >= toDate('1999-01-01 00:00:00', 'yyyy-MM-dd HH:mm:ss')",
			"doj = toDate('01/09/2009 00:00:00.000', 'dd/MM/yyyy hh:mm:ss.SSS')",
			"name = Rajesh OR address.city = Jaipur",
			"name = 'Rajesh Upadhyay' AND fatherName = 'Ramesh Chand Sharma'",
			"name = Raj AND (gender = M OR gender = F)",
			"(name = Rajesh OR name = Rajani) AND (gender = M OR onRoll = true)",
			"onRoll = true",
			"name regex('^R.*sh.*')",
			"name != regex('^R.*sh.*')",
			"dob before(40, 'Years')",
			"doj after(10, 'Days')",
			"name = Raj NOT gender = M",
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
	 * @param custId 
	 * @param scanId 
	 * @return
	 */
	public List<?> execute(Policy policy, String custId, String scanId) {
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
						IConstants.ELASTIC_INDX_DOCS, cls, null, null, scanId);
				if (!IUtils.isNull(lstEntities)) {
					for (String entity : lstEntities) {
						for (Expression exp : exprs) {
							lst.add(exp.evaluate(IUtils.getJSONObject(entity), null, cls, custId, scanId));
						}
					}
					// If we are going to save result then we will get result doc ids
					if (saveRes) {
						return saveResult(lst, custId);
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
		List<Expression> exprs = new ArrayList<>();
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
	 * @param scanId 
	 * @return
	 */
	private List<String> getIndexDocs(String url, String cls, String index, String type, String scanId) {
		List<String> lst = null;
		String srchUlr = IUtilities.getSearchUrl(env, url);
		logger.info("searchUrl: " + srchUlr);
		List<String> prms = new ArrayList<>();
		if (!IUtils.isNull(cls)) {
			prms.add(IConsts.PRM_CLASS);
			prms.add(cls);
			this.setRsltIndxName(cls.substring(cls.lastIndexOf(".") + 1), scanId);
		}
		if (!IUtils.isNull(index)) {
			prms.add(IConstants.PRM_INDEX);
			prms.add(index);
			this.setRsltIndxName(index, scanId);
		}
		if (!IUtils.isNull(type)) {
			prms.add(IConstants.PRM_TYPE);
			prms.add(type);
		}
		Map<String, Object> params = IUtils.getRestParamMap(prms.toArray());
		logger.info("Request: " + params);
		try {
			lst = IUtils.sendGetRestReq(rest, srchUlr, params);
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
	 * @param custId 
	 * @param scanId 
	 * @return
	 */
	public Object execute(String qry, String cls, String index, String type, String custId, String scanId) {
		Expression exp = Expression.parse(qry, -1L, -1L);
		if (!IUtils.isNull(exp)) {
			List<EvalPolicyRuleResult> lst = new ArrayList<>();
			List<String> lstEntts = getIndexDocs(
					IConstants.ELASTIC_INDX_DOCS, cls, index, type, scanId);
			if (!IUtils.isNull(lstEntts)) {
				for (String ent : lstEntts) {
					lst.add(exp.evaluate(IUtils.getJSONObject(ent),
							index, cls, custId, scanId));
				}
			}
			// If we are going to save result then we will get result doc ids
			if (saveRes) {
				return saveResult(lst, custId);
			}
			return lst;
		}
		return null;
	}

	/**
	 * Method to save evaluation result.
	 * @param lst
	 * @param custId 
	 * @return 
	 */
	private List<String> saveResult(List<EvalPolicyRuleResult> lst, String custId) {
		List<String> res = null;
		if (saveRes && !IUtils.isNull(lst) && lst.size() > 0 &&
			!IUtils.isNullOrEmpty(rsltIndxName)) {
			// try to save into grey log first.
			try {
				throw new Exception("Failed To Save");
				//res = saveInGrayLog(lst, custId);
			} catch(Exception ex) {
				logger.error(ex.getMessage(), ex);
				if (!IUtils.isNullOrEmpty(ex.getMessage()) &&
						ex.getMessage().contains("Failed To Save")) {
					res = saveInElastic(lst);
				}
			}
		}
		return res;
	}

	/**
	 * Method to try to save records into greylog.
	 * @param lst
	 * @param custId
	 * @return
	 */
	private List<String> saveInGrayLog(
			List<EvalPolicyRuleResult> lst, String custId) {
		String host = env.getProperty(IConstants.GULF_HOST);
		String port = env.getProperty(IConstants.GULF_PORT);
		List<String> retLst = null;
		// Create indexSet
		String indxId = this.createIndexSet(custId, host, port);
		if (!IUtils.isNullOrEmpty(indxId)) {
			retLst = new ArrayList<>();
			// Create Stream
			String strmId = this.createStream(custId, indxId, host, port);
			retLst.add("CustId: " + custId);
			retLst.add("IndexSetId: " + indxId);
			retLst.add("StreamId: " + strmId);
			if (!IUtils.isNullOrEmpty(strmId)) {
				// Set Default rule to match custid
				String ruleId = this.createStreamRule(custId, strmId, host, port);
				retLst.add("RuleId: " + ruleId);
			}
			// Create TCP transport and send messages
			putGelfMessages(lst, host);
		}
		return retLst;
	}

	private void putGelfMessages(List<EvalPolicyRuleResult> lst, String host) {
		GelfMessageConfiguration gelfConfig = PolicyApplication.getBean(
				GelfMessageConfiguration.class);
		GelfTransport transport = null;
		try {
			transport = gelfConfig.getPolicyResultInputGelfTransport();
			for (EvalPolicyRuleResult evalRes : lst) {
				GelfMessage msg = new GelfMessage(evalRes.toString(), host);
				msg.setLevel(GelfMessageLevel.INFO);
				transport.send(msg);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			if (!IUtils.isNull(transport)) {
				transport.flushAndStopSynchronously(1, TimeUnit.SECONDS, 1);
			}
		}
	}

	/**
	 * Method to get or create rule in gelf streams.
	 * @param custId
	 * @param strmId
	 * @param host
	 * @param port
	 * @return
	 */
	private String createStreamRule(String custId, String strmId, String host,
			String port) {
		String url = String.format(IConstants.POST_GELF_STREAM_RULES, host, port, strmId);
		// {"field":"indxNam","type":"2","value":".*_\\d+$","inverted":false,"description":"Match all index with name has _number in end"}
		GelfRulesRepository repo = PolicyApplication.getBean(GelfRulesRepository.class);
		GelfRules rule = repo.findByValue(custId);
		if (IUtils.isNull(rule)) {
			String reqObj = "{"
					+ "\"type\": 1,"
					+ "\"value\": \"" + custId + "\","
					+ "\"field\": \"custId\","
					+ "\"inverted\": false,"
					+ "\"description\": \"Accept messages of specified customer id\""
					+ "}";
			return IUtilities.createGelfEntity(url, reqObj,
					env.getProperty(IConstants.GELF_USER), 
					env.getProperty(IConstants.GELF_PASS), repo, GelfRules.class);
		} else {
			return rule.getGelfId();
		}
	}

	/**
	 * Method to get or create a streams object.
	 * @param custId
	 * @param indxId
	 * @param host
	 * @param port
	 * @return
	 */
	private String createStream (
			String custId, String indxId, String host, String port) {
		String url = String.format(IConstants.POST_GELF_STREAMS, host, port);
		// {"title":"All events","description":"Stream containing all events created by compliancemanager","remove_matches_from_default_stream":true,"index_set_id":"60d36ecbf7913a6e63d33a05"}
		GelfStreamsRepository repo = PolicyApplication.getBean(GelfStreamsRepository.class);
		GelfStreams strms = repo.findByTitle(custId);
		if (IUtils.isNull(strms)) {
			String reqObj = "{"
					+ "\"title\": \"" + custId + "\","
					+ "\"description\": \"Stream to line " + custId + " customer policy execution result.\","
					//+ "\"rules\": [ {} ],"
					//+ "\"content_pack\": \"\","
					//+ "\"matching_type\": \"\","
					+ "\"remove_matches_from_default_stream\": false,"
					+ "\"index_set_id\": \"" + indxId + "\""
					+ "}";
			return IUtilities.createGelfEntity(url, reqObj,
					env.getProperty(IConstants.GELF_USER), 
					env.getProperty(IConstants.GELF_PASS), repo, GelfStreams.class);
		} else {
			return strms.getGelfId();
		}
	}

	/**
	 * Method to create an IndexSets into Gelf
	 * @param custId
	 * @param host
	 * @param port
	 * @return
	 */
	private String createIndexSet(String custId, String host, String port) {
		String url = String.format(IConstants.POST_GELF_INDEX_SETS, host, port);
		GelfIndexSetRepository repo = PolicyApplication.getBean(GelfIndexSetRepository.class);
		GelfIndexSet indxSet = repo.findByTitle(custId);
		if (IUtils.isNull(indxSet) ) {
			String reqObj = "{"
					+ "\"title\": \"" + custId + "\","
					+ "\"description\": \"Index to store policy execution result for customer: " + custId + "\","
					+ "\"default\": false,"
					+ "\"writable\": true,"
					+ "\"index_prefix\": \"polcyRuleResult\","
					+ "\"shrards\": 5,"
					+ "\"replicas\": 0,"
					+ "\"rotation_strategy\": {\"type\": \"com.synectiks.process.server.indexer.rotation.strategies.MessageCountRotationStrategyConfig\", \"max_docs_per_index\": 20000000},"
					+ "\"rotation_strategy_class\": \"com.synectiks.process.server.indexer.rotation.strategies.MessageCountRotationStrategy\","
					+ "\"retention_strategy\": {\"type\": \"com.synectiks.process.server.indexer.retention.strategies.NoopRetentionStrategyConfig\", \"max_number_of_indices\": 2147483647},"
					+ "\"retention_strategy_class\": \"com.synectiks.process.server.indexer.retention.strategies.NoopRetentionStrategy\","
					+ "\"index_analyzer\": \"standard\","
					+ "\"index_optimization_max_num_segments\": 1,"
					+ "\"index_optimization_disabled\": false,"
					+ "\"field_type_refresh_interval\": 5000,"
					+ "\"creationDate\": \"" + IUtils.getFormattedDate(new Date(), "yyyy-MM-dd HH:mm:ss.SSS"/*IConsts.FULL_DATE_FORMAT*/) + "\""
					//+ "\"index_template_type\": \"\""
					+ "}";
			return IUtilities.createGelfEntity(url, reqObj,
					env.getProperty(IConstants.GELF_USER), 
					env.getProperty(IConstants.GELF_PASS),
					repo, GelfIndexSet.class);
		} else {
			return indxSet.getGelfId();
		}
	}

	/**
	 * Method to save result list into elastic.
	 * @param lst
	 * @return
	 */
	private List<String> saveInElastic(List<EvalPolicyRuleResult> lst) {
		List<String> res = null;
		if (saveRes && !IUtils.isNull(lst) && lst.size() > 0 &&
				!IUtils.isNullOrEmpty(rsltIndxName)) {
			String url = IUtilities.getSearchUrl(env,
					env.getProperty(IConsts.KEY_SEARCH_SAVE_DOCS));
			logger.info("Url: " + url);
			Map<String, String> headers = IUtils.getRestParamMap(
					IConsts.PRM_INDX_NAME, rsltIndxName);
			try {
				String ret = (String) IUtils.sendPostJsonDataReq(url, headers, lst.toString());
				if (!IUtils.isNullOrEmpty(ret) && ret.startsWith("[")) {
					res = IUtils.getListFromJsonString(ret);
					res.add(0, rsltIndxName);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return res;
	}

}
