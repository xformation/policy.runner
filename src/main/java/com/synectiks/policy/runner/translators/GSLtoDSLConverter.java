/**
 * 
 */
package com.synectiks.policy.runner.translators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;
import com.synectiks.policy.runner.utils.NestedString;

/**
 * Utility class to translate a GSL query into DSL json query for elasticsearch.
 * @author Rajesh
 */
public class GSLtoDSLConverter {

	private static Map<String, String> mapCriteriaKey = new HashMap<>();
	private static Map<String, String> mapGrpOpertr = new HashMap<>();
	private static List<String> conjuncOp = new ArrayList<>();
	private static List<String> criterias = new ArrayList<>();
	private static List<String> operators = new ArrayList<>();

	static {
		conjuncOp.add("and");
		conjuncOp.add("or");
		conjuncOp.add("not");
		conjuncOp.add("&&");
		conjuncOp.add("||");
		conjuncOp.add("!");

		mapGrpOpertr.put("[", "]");
		mapGrpOpertr.put("{", "}");
		mapGrpOpertr.put("(", ")");
		mapGrpOpertr.put("'", "'");
		mapGrpOpertr.put("\"", "\"");

		operators.add("contain-any");
		operators.add("contain-all");
		operators.add("regexMatch");
		operators.add("isEmpty()");
		operators.add("contain");
		operators.add("before");
		operators.add("exists");
		operators.add("after");
		operators.add("like");
		operators.add("with");
		operators.add("!=");
		operators.add(">=");
		operators.add("<=");
		operators.add("=");
		operators.add(">");
		operators.add("<");
		// operators.add("");

		criterias.add("should not have");
		criterias.add("should not");
		criterias.add("should have");
		criterias.add("should");
		criterias.add("where");
		// criteria query keys
		mapCriteriaKey.put("where", "must");
		mapCriteriaKey.put("should", "should");
		mapCriteriaKey.put("should have", "should");
		mapCriteriaKey.put("should not", "must_not");
		mapCriteriaKey.put("should not have", "must_not");
	}

	private static String translateInDSL(String input) throws JSONException {
		JSONObject json = new JSONObject();
		if (!IUtils.isNullOrEmpty(input)) {
			// First string is entity
			String entity = getFirstString(input, true);
			json.put("Entity", entity);
			input = input.substring(input.indexOf(" ") + 1);
			json.put("query", processCriterias(input, null));
		}
		return json.toString();
	}

	private static JSONObject processCriterias(String input, JSONObject json)
			throws JSONException {
		if (IUtils.isNull(json)) {
			json = new JSONObject();
		}
		System.out.println("processCriterias: " + input);
		if (!IUtils.isNullOrEmpty(input)) {
			Entry<String, String> opKey = getCriteriaEntry(input);
			if (!IUtils.isNull(opKey)) {
				// +1 to remove space after key
				input = input.substring(opKey.getKey().length() + 1);
				JSONObject expr = evaluateExpression(input, opKey.getValue());
				json = copyJsonItems(expr, json);
			}
			// re-check input for remaining checks as filter
			JSONObject filters = processFilters(input);
			if (!IUtils.isNull(filters)) {
				json.put("filter", filters);
			}
		}
		return json;
	}

	private static JSONObject copyJsonItems(JSONObject src, JSONObject dest)
			throws JSONException {
		if (!IUtils.isNull(src) && src.keys().hasNext()) {
			Iterator<?> keys = src.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				dest.put(key, src.get(key));
			}
		}
		System.out.println("copyJsonItems: " + dest.toString());
		return dest;
	}

	private static JSONObject evaluateExpression(String input, String opCrtria)
			throws JSONException {
		System.out.println("op: " + opCrtria + ", input: " + input);
		List<JSONObject> resList = null;
		String qryKey = null;
		JSONObject json = new JSONObject();
		if (!IUtils.isNullOrEmpty(input) && !IUtils.isNullOrEmpty(opCrtria)) {
			switch (opCrtria) {
			case "must":
				qryKey = "match";
				resList = createQuery(input, qryKey);
				break;
			case "should":
				qryKey = "term";
				resList = createQuery(input, qryKey);
				break;
			case "must_not":
				break;
			}
			if (!IUtils.isNull(resList)) {
				if (resList.size() > 1) {
					json.put(opCrtria, resList);
				} else if (resList.size() == 1) {
					json.put(opCrtria, resList.get(0));
				} else {
					System.out.println("No match found: " + resList);
				}
			}
		}
		return json;
	}

	private static List<JSONObject> createQuery(String input, String qryType) {
		System.out.println("createQuery: " + input);
		List<JSONObject> list = new ArrayList<>();
		if (!IUtils.isNullOrEmpty(input)) {
			String key = getFirstString(input);
			String op = getOperator(key);
			String val = null;
			try {
				if (!IUtils.isNullOrEmpty(op)) {
					// handle non space criteria expression i.e abc=def
					list.add(processQryStr(qryType, op, key));
				} else {
					input = input.substring(key.length()).trim();
					if (input.length() == 0) {
						// handle field exists case
						list.add(processQryStr(qryType, "exists", key));
					} else {
						op = getFirstString(input);
						if (isOperator(op)) {
							input = input.substring(op.length()).trim();
							val = extractValue(input);
							System.out.println("Extracted group value: " + val);
							input = input.substring(val.length()).trim();
							System.out.println("Value: " + val);
							list.add(processQryStr(qryType, op, key, val));
							// check Conjunction 
							String conjunc = getConjunctionOperator(input);
							if (!IUtils.isNullOrEmpty(conjunc)) {
								input = input.substring(conjunc.length()).trim();
								list.addAll(createQuery(input, qryType));
							}
						} else {
							System.out.println("Un-handled case of parsing: "
									+ input);
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	private static String getConjunctionOperator(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			for (String op : conjuncOp) {
				if (input.startsWith(op)) {
					return op;
				}
			}
		}
		return null;
	}

	private static String extractValue(String input) {
		System.out.println("Extract: " + input);
		if (!IUtils.isNullOrEmpty(input)) {
			if (isGroupValues(input)) {
				int indx = getGrpClosingIndex(input);
				if (indx != -1) {
					return input.substring(0, indx);
				}
			} else {
				return getFirstString(input);
			}
		}
		System.out.println("Unable to parse value: " + input);
		return null;
	}

	private static int getGrpClosingIndex(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			Entry<String, String> grpOp = getGroupOperator(input);
			if (!IUtils.isNull(grpOp)) {
				int indx = findClosingIndex(grpOp, input);
				System.out.println("Closing-index: " + indx);
				return indx;
			}
		}
		return -1;
	}

	private static int findClosingIndex(
			Entry<String, String> grpOp, String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			int indx = input.indexOf(grpOp.getValue());
			if (indx != -1) {
				indx += grpOp.getKey().length();
				// find and count the internal groups
				int cnt = getOccuranceCount(input, grpOp.getKey());
				while(cnt > 0) {
					int ind = input.indexOf(grpOp.getValue(), indx);
					if (ind != -1) {
						indx = ind + grpOp.getKey().length();
					}
					cnt--;
				}
				// no more occurrence of key in string so its the closing
				return indx;
			}
		}
		return -1;
	}

	private static int getOccuranceCount(String input, String key) {
		int cnt = 0;
		String str = input.substring(input.indexOf(key) + key.length());
		while (true) {
			int indx = str.indexOf(key);
			if (indx != -1) {
				str = str.substring(indx + key.length());
				cnt++;
			} else {
				break;
			}
		}
		return cnt;
	}

	private static boolean isGroupValues(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			for (String key : mapGrpOpertr.keySet()) {
				if (input.startsWith(key)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Entry<String, String> getGroupOperator(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			for (Entry<String, String> entry : mapGrpOpertr.entrySet()) {
				if (input.startsWith(entry.getKey())) {
					return entry;
				}
			}
		}
		return null;
	}

	private static JSONObject processQryStr(String qryType, String op, String key)
			throws JSONException {
		String value = null;
		if (!IUtils.isNullOrEmpty(key) && key.contains(op)) {
			int indx = key.indexOf(op);
			value = key.substring(indx + op.length());
			key = key.substring(0, indx);
		}
		return processQryStr(qryType, op, key, value);
	}

	private static JSONObject processQryStr(String qryType, String op, String key,
			String value) throws JSONException {
		key = formatKey(key);
		JSONObject json = new JSONObject();
		switch (op) {
		case "regexMatch":
			value = formatRegexValue(value, false);
			json.put("regexp", new JSONObject().put(key, value));
			break;
		case "contain-any":
			break;
		case "contain-all":
			break;
		case "isEmpty()":
			json.put("term", new JSONObject().put(key, ""));
			break;
		case "contain":
			break;
		case "exists":
			json.put("exists", new JSONObject().put("field", key));
			break;
		case "before":
			break;
		case "after":
			break;
		case "with":
			break;
		case "like":
			break;
		case "!=":
			break;
		case ">=":
			break;
		case "<=":
			break;
		case "=":
			json.put(qryType, new JSONObject().put(key, value));
			break;
		case ">":
			break;
		case "<":
			break;
		default:
			System.out.println("Unknown operator: " + op);
			break;
		}
		System.out.println("processQryStr: " + json.toString());
		return json;
	}

	private static String formatRegexValue(String value, boolean rmFwSlash) {
		if (!IUtils.isNullOrEmpty(value)) {
			if (value.startsWith("/") && rmFwSlash) {
				value = value.substring(1);
				int indx = value.indexOf("/");
				return value.substring(0, indx);
			} else {
				return value;
			}
		}
		return null;
	}

	private static String formatKey(String key) {
		if (!IUtils.isNullOrEmpty(key)) {
			if (key.startsWith("$")) {
				// TODO logic to handle $ in keys
			}
		}
		return key;
	}

	private static String getOperator(String key) {
		if (!IUtils.isNullOrEmpty(key)) {
			for (int i = 0; i < operators.size(); i++) {
				if (key.contains(operators.get(i))) {
					return operators.get(i);
				}
			}
		}
		return null;
	}

	private static boolean isOperator(String key) {
		if (!IUtils.isNullOrEmpty(key)) {
			for (int i = 0; i < operators.size(); i++) {
				if (operators.get(i).equals(key)) {
					return true;
				}
			}
		}
		return false;
	}

	private static JSONObject processFilters(String input) throws JSONException {
		System.out.println("processFilters: " + input);
		return null;//processCriterias(input, null);
	}

	private static Entry<String, String> getCriteriaEntry(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			for (int i = 0; i < criterias.size(); i++) {
				if (input.startsWith(criterias.get(i))) {
					for (Entry<String, String> entry : mapCriteriaKey.entrySet()) {
						if (entry.getKey().equals(criterias.get(i))) {
							return entry;
						}
					}
				}
			}
		}
		return null;
	}

	private static String getFirstString(String input) {
		return getFirstString(input, false);
	}

	private static String getFirstString(String input, boolean rmList) {
		System.out.println("first: " + input);
		if (!IUtils.isNullOrEmpty(input) && input.contains(" ")) {
			int indx = input.indexOf(" ");
			String res = null;
			if (indx != -1) {
				res = input.substring(0, indx);
			} else {
				res = input;
			}
			if (rmList) {
				return refineEntityName(res.trim());
			} else {
				return res.trim();
			}
		}
		return input;
	}

	private static String refineEntityName(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			int indx = input.indexOf("<");
			if (indx != -1) {
				input = input.substring(indx, input.indexOf(">"));
			}
		}
		System.out.println("refined: " + input);
		return input;
	}

	public static void main(String... args) throws SynectiksException {
		String str = "rajesh (abc (def(klm()) AND efg(hig(nop()))) ) kumar";
		System.out.println(NestedString.parse(str, "(", ")", true));
		int ind = IUtilities.findClosingIndex(str,
				IConstants.Keywords.SmlBrkt);
		//System.out.println("Close index: " + ind + ": " + str.substring(0, ind));
		str = "[abc (def(klm()) AND efg(hig(nop()))) ])]";
		System.out.println(NestedString.parse(str, "(", ")", true));
		ind = IUtilities.findClosingIndex(str,
				IConstants.Keywords.SmlBrkt);
		//System.out.println("Close index: " + ind + ": " + str.substring(0, ind));
		str = "(abc{ (def(klm()) AND efg(hig(nop()))) })";
		System.out.println(NestedString.parse(str, "(", ")", true));
		ind = IUtilities.findClosingIndex(str,
				IConstants.Keywords.SmlBrkt);
		//System.out.println("Close index: " + ind + ": " + str.substring(0, ind));
		System.exit(0);
		String[] inputGSL = new String[] {
				"IamUser where name regexMatch /^<root_account>$/i should not have passwordLastUsed after(-90, 'days')",
				"DynamoDbTable should have encrypted=true",
				"ELB should not have elbListeners with [ policies contain [ attributes contain-any [$ in ('Protocol-SSLv3', 'Protocol-TLSv1') ] ] ]",
				"ELB should have elbListeners contain [ sourceProtocol='TCP' and sourcePort=80 and instanceProtocol='TCP' and instancePort=80 or sourceProtocol='HTTPS' and sourcePort=443 and instanceProtocol='HTTPS' and instancePort=443]",
				"Iam should have passwordPolicy.enabledInAccount=true",
				"CloudTrail should have kmsKeyId",
				"S3Bucket should not have ( acl.grants contain [uri like 'http://acs.amazonaws.com/groups/global/%'] or policy.Statement contain [Effect='Allow' and (Principal='*' or Principal.AWS='*')])",
				"SecurityGroup should not have inboundRules with [scope =  '0.0.0.0/0' and port<=3389 and portTo>=3389]",
				"SecurityGroup where name like 'default' should have inboundRules isEmpty() and outboundRules isEmpty()",
				"List<CloudTrail> should have items with [hasSNSSubscriber='true' and metricFilters with [filterPattern isFilterPatternEqual('{ $.userIdentity.type = Root && $.userIdentity.invokedBy NOT EXISTS && $.eventType != AwsServiceEvent }')] length() > 0]",
				"List<CloudTrail> should have items with [hasSNSSubscriber='true' and metricFilters with [filterPattern isFilterPatternEqual('{($.eventSource = config.amazonaws.com) && (($.eventName=StopConfigurationRecorder)||($.eventName=DeleteDeliveryChannel)||($.eventName=PutDeliveryChannel)||($.eventName=PutConfigurationRecorder))}')] length() > 0]",
				"IamUser where firstAccessKey.isActive='true' and not firstAccessKey.lastRotated after(-90, 'days') should have firstAccessKey.lastUsedDate after(-90, 'days')",
				"SecurityGroup where name != 'default' should not have networkAssetsStats contain-all [ count = 0 ]",
				"ApplicationLoadBalancer should not have listeners contain [ certificates contain [ expiration before(30, 'days') ] ]" };
		for (String input : inputGSL) {
			System.out.println(input + "\n---------------");
			String result = null;
			try {
				result = translateInDSL(input);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("---------------\n" + result + "\n---------------");
		}
	}

}
