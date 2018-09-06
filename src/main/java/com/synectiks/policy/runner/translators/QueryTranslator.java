package com.synectiks.policy.runner.translators;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rajesh
 */
public class QueryTranslator {

	private static Logger logger = LoggerFactory.getLogger(QueryTranslator.class);

	private static JSONObject translateQuery(String input) {
		QueryParser parser = new QueryParser(input);
		JSONObject json = parser.parse();
		return json;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] inputs = new String[] {
			"value", // Full text search in all fields- { "query_string": { "query": "abc" } } or { "match": { "_all": "abc" } }
			"has root.node.key", // Check if field exists
			/*"root.node.key = 'value'",
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
			"root.node.key >= toDate('2018-08-15 13:20:30', 'yyyy-MM-dd HH:mm:ss')",*/
			// We can also use elastic date math strings i.e.
			// https://www.elastic.co/guide/en/elasticsearch/reference/2.4/common-options.html#date-math
			"key = toDate('15/08/2018 13:20:30.000', 'dd/MM/yyyy hh:mm:ss.SSS')",
			"root.node.key = value OR root.node.key1 = value",
			"key = value AND key = value",
			"(root.node.key1 = value1 OR key2 = value2) AND (root.node.key3 = value3 OR key4 = value4)"
		};
		for (String input : inputs) {
			JSONObject elsQuery = translateQuery(input);
			logger.info("\nINPUT: {}\nOUTPUT: {}", input, elsQuery.toString());
		}
	}

}
