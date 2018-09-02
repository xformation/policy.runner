package com.synectiks.policy.runner.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants.KWTypes;
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
	 * 	bool: {
	 * 		conjType:[
	 * 			{...}
	 * 		]
	 * 	}
	 * }
	 * </pre>
	 * @param conjType
	 * @param json
	 * @return
	 */
	static JSONObject createBoolQueryFor(Keywords conjType, JSONObject json) {
		if (IUtils.isNull(json)) {
			return null;
		}
		JSONObject qry = new JSONObject();
		try {
			String key = null;
			boolean isNot = false;
			if (json.has(IConstants.NOT_QRY)) {
				isNot = json.optBoolean(IConstants.NOT_QRY);
				json.remove(IConstants.NOT_QRY);
			}
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
				if (json.has(IConstants.BOOL)) {
					mtype.put(key, json);
				} else {
					mtype.put(key, new JSONArray().put(json));
				}
			}
			qry.put(IConstants.BOOL, mtype);
		} catch (JSONException e) {
			IConstants.logger.error(e.getMessage(), e);
		}
		return qry;
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
			String val = groupValue;
			if (!IUtils.isNullOrEmpty(groupValue) &&
					groupValue.startsWith(
							Keywords.SnglQuote.getGroupStart())) {
				val = getGroupValue(groupValue, Keywords.SnglQuote, false);
			}
			return createQuery(IConstants.REGEXP, key, val);
		}
		return null;
	}

	/**
	 * Method to create range query json i.e.
	 * <pre>{
	 *     "range" : {
	 *         "born" : {
	 *             "gte": "01/01/2012",
	 *             "lte": "2013",
	 *             "format": "dd/MM/yyyy||yyyy"
	 *         }
	 *     }
	 * }
	 * </pre>
	 * @param key
	 * @param value
	 * @param format
	 * @param dtOp
	 * @return null or range query json
	 */
	static JSONObject createRangeQuery(String key, Object value, String format,
			String dtOp) {
		if (IUtils.isNullOrEmpty(key) || IUtils.isNull(value)) {
			return null;
		}
		JSONObject json = new JSONObject();
		try {
			JSONObject rngVal = new JSONObject();
			rngVal.put(dtOp, value);
			if (!IUtils.isNullOrEmpty(format)) {
				rngVal.put(IConstants.FORMAT, format);
			}
			json.put(IConstants.RANGE, new JSONObject().put(key, rngVal));
		} catch (JSONException e) {
			IConstants.logger.error(e.getMessage(), e);
		}
		return json;
	}

	/**
	 * Method to check if value is numeric
	 * @param value
	 * @param isGrp 
	 * @return
	 */
	static boolean isNumeric(String value, boolean isGrp) {
		if (isGrp) {
			List<String> list = IUtils.getListFromString(value, IConsts.DELIM_COMMA);
			for (String num : list) {
				// make sure every item is numeric
				if (IUtils.isNull(parseNumber(num))) {
					return false;
				}
			}
			return true;
		} else {
			return !IUtils.isNull(parseNumber(value));
		}
	}

	/**
	 * Method to get a JSONArray from string
	 * @param value
	 * @param isNum
	 * @return
	 */
	static JSONArray getJArrFromString(String value, boolean isNum) {
		JSONArray arr = new JSONArray();
		List<String> list = IUtils.getListFromString(value, IConsts.DELIM_COMMA);
		for (String num : list) {
			// make sure every item is numeric if isNum true
			if (isNum) {
				Number val = parseNumber(num);
				if (IUtils.isNull(val)) {
					return null;
				} else {
					arr.put(val);
				}
			} else {
				arr.put(num);
			}
		}
		return arr;
	}

	/**
	 * Method to parse value as number
	 * @param value
	 * @return
	 */
	static Number parseNumber(String value) {
		if (!IUtils.isNullOrEmpty(value)) {
			try {
				return NumberFormat.getInstance().parse(value);
			} catch (ParseException pe) {
				// ignore it.
			}
		}
		return null;
	}

	/**
	 * Method to get elastic search range query key for operator
	 * @param op
	 * @return
	 */
	static String getESOperatorKey(Keywords op) {
		String key = null;
		switch(op) {
		case GT:
			key  = "gt";
			break;
		case GTE:
			key = "gte";
			break;
		case LT:
			key = "lt";
			break;
		case LTE:
			key = "lte";
			break;
		default:
			IUtils.logger.warn("Unsupported operator '{}' found.", op.getKey());
			break;
		}
		return key;
	}

	/**
	 * Method to check if this is an IN group match query
	 * @param op
	 * @param grpOp
	 * @return
	 */
	static boolean isInQquery(Keywords op, Keywords grpOp) {
		if ((op == Keywords.EQ || op == Keywords.NE) &&
				grpOp == Keywords.SmlBrkt) {
			return true;
		}
		return false;
	}

	/**
	 * Method to create multi_match search query i.e.
	 * <pre>
	 * {
	 * 	multi_match: {
	 * 		query: grpVal,
	 * 		type: "best_fields",
	 * 		fields: jarr,
	 * 		operator: "and"
	 * 	}
	 * }
	 * </pre>
	 * @param jarr
	 * @param grpVal
	 * @param isMust
	 * @return
	 */
	static JSONObject createMultiSearchQuery(
			JSONArray jarr, String grpVal, boolean isMust) {
		if (!IUtils.isNull(jarr) && !IUtils.isNullOrEmpty(grpVal)) {
			JSONObject json = new JSONObject();
			try {
				json.put(IConstants.QUERY, grpVal);
				json.put(IConstants.TYPE, IConstants.BEST_FRIENDS);
				json.put(IConstants.FIELDS, jarr);
				if (isMust) {
					json.put(IConstants.OPERATOR, IConstants.AND);
				}
				return new JSONObject().put(IConstants.MULTI_MATCH, json);
			} catch (JSONException e) {
				IConstants.logger.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Check if qry starts with group operator
	 * @param qry
	 * @return
	 */
	static boolean isStartWithGroup(String qry) {
		return !IUtils.isNull(getStartWithGroup(qry));
	}

	/**
	 * Method to return group operator if qry starts with any
	 * @param qry
	 * @return
	 */
	static Keywords getStartWithGroup(String qry) {
		if (!IUtils.isNullOrEmpty(qry)) {
			List<Keywords> list = Keywords.list(KWTypes.GROUP);
			for (Keywords kw : list) {
				if (qry.startsWith(kw.getGroupStart())) {
					return kw;
				}
			}
		}
		return null;
	}

	/**
	 * Method to add a length field in json.
	 * @param json
	 * @param key
	 */
	static void addProcessedKey(JSONObject json, String key) {
		if (IUtils.isNullOrEmpty(key))
			return;
		try {
			if (json.has(IConstants.LENGTH)) {
				String val = json.getString(IConstants.LENGTH);
				if (addSpace(key) || endWithConjOp(val)) {
					key = val + IConsts.SPACE + key;
				} else {
					key = val + key;
				}
			}
			json.put(IConstants.LENGTH, key);
		} catch(JSONException je) {
			// ignore it.
		}
	}

	/**
	 * Method to check if value ends with AND or OR
	 * @param val
	 * @return
	 */
	static boolean endWithConjOp(String val) {
		if (!IUtils.isNullOrEmpty(val)) {
			List<Keywords> list = Keywords.list(KWTypes.CONJUNCTION);
			for (Keywords kw : list) {
				if (val.endsWith(kw.getKey())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method to check if needs to add space in prefix.
	 * @param key
	 * @return
	 */
	static boolean addSpace(String key) {
		if (!IUtils.isNullOrEmpty(key)) {
			try {
				Keywords kw = Keywords.valueOf(key);
				if (IUtils.isNull(kw) &&
						kw != Keywords.SnglQuote &&
						kw != Keywords.DblQuote) {
					return true;
				}
			} catch (IllegalArgumentException ie) {
				//ignore it
			}
			if (isStartWithGroup(key)) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Check if qry starts with an operator
	 * @param qry
	 * @return
	 */
	static boolean haveOperator(String qry) {
		return !IUtils.isNull(getOperator(qry, true));
	}

	/**
	 * Check if qry starts with an operator
	 * @param qry
	 * @return
	 */
	static boolean isStartWithOperator(String qry) {
		return !IUtils.isNull(getOperator(qry, false));
	}

	/**
	 * Method to return operator if qry starts with any
	 * @param qry
	 * @param hasKey
	 * @return
	 */
	static Keywords getOperator(String qry, boolean hasKey) {
		if (!IUtils.isNullOrEmpty(qry)) {
			if (hasKey) {
				// remove first string assume its query.
				String key = getFirstString(qry);
				qry = removeProcessedString(qry, key);
			}
			Keywords operator = null;
			if (!IUtils.isNullOrEmpty(qry)) {
				List<Keywords> list = Keywords.list(KWTypes.OPERATOR);
				for (Keywords kw : list) {
					if (qry.startsWith(kw.getKey())) {
						if (kw.getKey().length() == 1) {
							// return only if
							// there is no double length operator.
							operator = kw;
						} else {
							return kw;
						}
					}
				}
			}
			return operator;
		}
		return null;
	}

	/**
	 * Method to find if query starts with any conjunction operator, if matches
	 * then returns the operator or returns null.
	 * @param qry
	 * @return
	 */
	static Keywords getConjuncOperator(String qry) {
		if (!IUtils.isNullOrEmpty(qry)) {
			List<Keywords> list = Keywords.list(KWTypes.CONJUNCTION);
			for (Keywords kw : list) {
				if (qry.startsWith(kw.getKey() + IConsts.SPACE)) {
					return kw;
				}
			}
		}
		return null;
	}

	/**
	 * Check if query starts with conjunction operator.
	 * @param qry
	 * @return
	 */
	static boolean isStartWithConjuction(String qry) {
		return !IUtils.isNull(getConjuncOperator(qry));
	}

	/**
	 * Check if query starts with has keyword.
	 * @param qry
	 * @return
	 */
	static boolean isStartWithHasKeyword(String qry) {
		if (!IUtils.isNullOrEmpty(qry)) {
			List<Keywords> list = Keywords.list(KWTypes.KEYWORD);
			for (Keywords kw : list) {
				if (kw == Keywords.HAS &&
						qry.startsWith(kw.getKey() + IConsts.SPACE)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method to find if query has an operator after key, if matches then
	 * returns the operator or returns null.
	 * @param qry
	 * @param hasKey
	 * @return
	 */
	static Keywords getFunction(String qry, boolean hasKey) {
		if (!IUtils.isNullOrEmpty(qry)) {
			if (hasKey) {
				// remove first string assume its query.
				String key = getFirstString(qry);
				qry = removeProcessedString(qry, key);
			}
			List<Keywords> list = Keywords.list(KWTypes.FUNCTION);
			for (Keywords kw : list) {
				if (qry.startsWith(kw.getKey())) {
					return kw;
				}
			}
		}
		return null;
	}

	/**
	 * Method to check if query has a function just after key
	 * @param qry
	 * @return
	 */
	static boolean haveFunction(String qry) {
		return !IUtils.isNull(getFunction(qry, true));
	}

	/**
	 * Method to check if query has a function just after key
	 * @param qry
	 * @return
	 */
	static boolean isStartWithFunction(String qry) {
		return !IUtils.isNull(getFunction(qry, false));
	}

	/**
	 * Method to check if value has any wild card
	 * @param value
	 * @return
	 */
	static boolean hasWildcard(final String value) {
		if (!IUtils.isNullOrEmpty(value)) {
			List<Keywords> list = Keywords.list(KWTypes.WILDCARD);
			for (Keywords kw : list) {
				if (value.contains(kw.getKey())) {
					return true;
				}
			}
		}
		return false;
	}
}
