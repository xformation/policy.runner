package com.synectiks.policy.runner.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants.Keywords;

/**
 * @author Rajesh
 */
public interface IUtilities {

	/**
	 * Method to extract first non space char sequence form input.
	 * @param input
	 * @return
	 */
	static String getFirstString(String input) {
		return getRefinedFirstString(input, false);
	}

	/**
	 * Method to find non space char sequence and refine it, if rmList is true,
	 * it removes enclosing <> from output string.
	 * @param input
	 * @param rmList
	 * @return
	 */
	static String getRefinedFirstString(String input, boolean rmList) {
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

	/**
	 * Method to removes enclosing <> from input string
	 * @param input
	 * @return
	 */
	static String refineEntityName(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			int indx = input.indexOf("<");
			if (indx != -1) {
				input = input.substring(indx, input.indexOf(">"));
			}
		}
		return input;
	}

	/**
	 * Method to remove key from input query.
	 * @param query
	 * @param key
	 * @return
	 */
	static String removeProcessedString(String query, String key) {
		if (!IUtils.isNullOrEmpty(query) && !IUtils.isNullOrEmpty(key)) {
			query = query.substring(key.length());
		}
		return (IUtils.isNullOrEmpty(query) ? "" : query.trim());
	}

	/**
	 * Method to create nested string from qry and return the first group.
	 * @param qry
	 * @param grpOp
	 * @param includDelim
	 * @return
	 */
	static String getGroupValue(String qry, Keywords grpOp, boolean includDelim) {
		if (!IUtils.isNullOrEmpty(qry)) {
			try {
				NestedString nstStr = NestedString.parse(qry, grpOp.getGroupStart(),
						grpOp.getGroupEnd(), includDelim);
				return NestedString.getUpperGroupString(nstStr);
			} catch (SynectiksException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Method to create a json object i.e.
	 * <pre>
	 * {
	 * 	qryType: {
	 * 		key: value
	 * 	}
	 * }
	 * 	OR (if qryType is null or empty)
	 * {
	 * 	key: value
	 * }
	 * </pre>
	 * @param qryType
	 * @param key
	 * @param value
	 * @return
	 */
	static JSONObject createQuery(String qryType, String key, Object value) {
		JSONObject json = new JSONObject();
		try {
			if (IUtils.isNullOrEmpty(qryType)) {
				json.put(key, value);
			} else {
				json.put(qryType, new JSONObject().put(key, value));
			}
		} catch (JSONException e) {
			IConstants.logger.error(e.getMessage(), e);
		}
		return json;
	}

	/**
	 * Method to create elastic search queryi.e.
	 * <pre>
	 * {
	 * 	query: {
	 * 		bool: {
	 * 			conjType:[
	 * 				{...}
	 * 			]
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 * @param conjType
	 * @param json
	 * @return
	 */
	static JSONObject createBoolQueryFor(Keywords conjType, JSONObject json) {
		return createBoolQueryFor(conjType, false, json);
	}

	/**
	 * Method to create elastic search queryi.e.
	 * <pre>
	 * {
	 * 	query: {
	 * 		bool: {
	 * 			conjType:[
	 * 				{...}
	 * 			]
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 * @param conjType
	 * @param isNot
	 * @param json
	 * @return
	 */
	static JSONObject createBoolQueryFor(Keywords conjType, boolean isNot, JSONObject json) {
		JSONObject qry = new JSONObject();
		try {
			String key = null;
			JSONObject mtype = new JSONObject();
			switch(conjType) {
			case AND:
				if (isNot) {
					key = IConstants.MUST_NOT;
				} else {
					key = IConstants.MUST;
				}
				break;
			case OR:
				if (isNot) {
					key = IConstants.SHOULD_NOT;
				} else {
					key = IConstants.SHOULD;
				}
				break;
			default:
				// leave it unsupported type
				break;
			}
			if (!IUtils.isNullOrEmpty(key)) {
				mtype.put(key, new JSONArray().put(json));
			}
			qry.put(IConstants.QUERY,
					new JSONObject().put(IConstants.BOOL, mtype));
		} catch (JSONException e) {
			IConstants.logger.error(e.getMessage(), e);
		}
		return json;
	}

	/**
	 * Method to create regex match query i.e.
	 * <pre>
	 * {
	 * 	regexp: {
	 * 		key: value
	 * 	}
	 * }
	 * @param key
	 * @param groupValue
	 * @return
	 */
	static JSONObject createRegexQuery(String key, String groupValue) {
		if (!IUtils.isNullOrEmpty(key)) {
			String val = null;
			if (!IUtils.isNullOrEmpty(groupValue)) {
				try {
					val = NestedString.getUpperGroupString(
							NestedString.parse(groupValue,
									Keywords.SnglQuote.getGroupStart(),
									Keywords.SnglQuote.getGroupEnd(), false));
				} catch (SynectiksException e) {
					IConstants.logger.error(e.getMessage(), e);
				}
			}
			return createQuery(IConstants.REGEXP, key, val);
		}
		return null;
	}
}
