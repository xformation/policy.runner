/**
 * 
 */
package com.synectiks.policy.runner.translators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.entities.Rule;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.EvalCheck;

/**
 * Class to convert Rule to EvalRule
 * @author Rajesh Upadhyay
 */
public class RuleTranslator {

	private static final Logger logger = LoggerFactory.getLogger(RuleTranslator.class);

	public List<EvalCheck> translate(Rule rule) {
		List<EvalCheck> lstChks = null;
		if (!IUtils.isNull(rule) && !IUtils.isNull(rule.getChecks())
				&& rule.getChecks().size() > 0) {
			lstChks = new ArrayList<>();
			for (String chk : rule.getChecks()) {
				lstChks.add(translateCheck(chk));
			}
		}
		return lstChks;
	}

	private EvalCheck translateCheck(String chk) {
		QueryParser parser = new QueryParser(chk, false);
		JSONObject json = parser.parse();
		logger.info("Res: " + json);
		EvalCheck check = null;//IUtils.getObjectFromValue(json.toString(), EvalCheck.class);
		return check;
	}

	public static void main(String[] args) {
		List<EvalCheck> chks = new RuleTranslator().translate(getRule());
		for (EvalCheck chk : chks) {
			logger.info(chk.toString());
		}
	}

	private static Rule getRule() {
		Rule rule = new Rule();
		rule.setId("1");
		rule.setName("TestRule");
		rule.setDescription("Test");
		rule.setSearchable(false);
		rule.setChecks(Arrays.asList(
				"value", // Full text search in all fields- { "query_string": { "query": "abc" } } or { "match": { "_all": "abc" } }
				"has root.node.key", // Check if field exists
				"root.node.key = 'value'",
				"root.node.key > 10",
				"root.node.key < 10",
				"root.node.key != 10",
				"root.node.key >= 10",
				"root.node.key <= 10",
				"root.node.key regex('^R.*esh$')",
				"root.node.key isNull",
				"root.node.key isEmpty",
				"root.node.key isNotNull",
				"root.node.key isNotEmpty",
				"[key1, key2, *Id] value", // Search value in key fields with wildcard key name
				"[key1, key2] +\"Rajesh Kumar\"", // Search Rajesh AND Kumar  in key fields multi_match
				"root.node.key = 'A?c*fg'", // LIKE
				"root.node.sub.key != 'A?c*fg'", // NOT LIKE
				"key = (value1, value2, value3)", // IN
				"root.key != (value1, value2, value3)", // NOT IN
				"root.node.key >= toDate('2018-08-15 13:20:30')", // Default format: yyyy-MM-dd HH:mm:ss
				"root.node.key >= toDate('2018-08-15 13:20:30', 'yyyy-MM-dd HH:mm:ss')",
				// We can also use elastic date math strings i.e.
				// https://www.elastic.co/guide/en/elasticsearch/reference/2.4/common-options.html#date-math
				"key = toDate('15/08/2018 13:20:30.000', 'dd/MM/yyyy hh:mm:ss.SSS')",
				"root.node.key = value OR root.node.key1 = value",
				"key = value AND key = value",
				"(root.node.key1 = value1 OR key2 = value2) AND (root.node.key3 = value3 OR key4 = value4)"));
		return rule;
	}

}
