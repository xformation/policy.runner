package com.synectiks.policy.runner.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.PSqlEntity;
import com.synectiks.commons.entities.SourceEntity;
import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.commons.utils.IUtils.CTypes;
import com.synectiks.policy.runner.parsers.Value;
import com.synectiks.policy.runner.utils.IConstants.KWTypes;
import com.synectiks.policy.runner.utils.IConstants.Keywords;

/**
 * @author Rajesh
 */
public interface IUtilities {

	Map<String, List<String>> entityFields = new HashMap<>();

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
	 * get String from array
	 * @param objs
	 * @return
	 */
	static String arrToString(Object[] objs) {
		StringBuilder sb = new StringBuilder();
		if (!IUtils.isNull(objs) && objs.length > 0) {
			sb.append("[");
			for (Object o : objs) {
				sb.append(sb.length() > 1 ? ", " : "");
				sb.append("\"" + o.toString() + "\"");
			}
			sb.append("]");
			return sb.toString();
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
			if (IUtils.isNull(key)) {
				return null;
			} else {
				// check and add nested path if exists
				setNestedPath(json, qryType, key, value);
			}
			// add key values into json.
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
	 * Method to extract nested path from key or from value
	 * if key is field and query type is exists
	 * @param json
	 * @param qryType
	 * @param key
	 * @param value
	 */
	static void setNestedPath(JSONObject json, String qryType, String key, Object value) {
		String nstPath = null;
		if (!IUtils.isNullOrEmpty(key) && key.contains(".")) {
			// handle nested type queries
			int indx = key.lastIndexOf(".");
			nstPath = key.substring(0, indx);
		} else if (!IUtils.isNullOrEmpty(qryType) && IConstants.EXISTS.equals(qryType)) {
			if (!IUtils.isNullOrEmpty(key) && IConstants.FIELD.equals(key)) {
				if (!IUtils.isNull(value) && value.toString().contains(".")) {
					int indx = value.toString().lastIndexOf(".");
					nstPath = value.toString().substring(0, indx);
				}
			}
		}
		// set path if its not empty.
		if (!IUtils.isNullOrEmpty(nstPath)) {
			try {
				json.put(IConstants.NST_PTH_QRY, nstPath);
			} catch (JSONException e) {
				IConstants.logger.error(e.getMessage(), e);
			}
		}
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
			JSONObject bool = new JSONObject();
			boolean isNot = isNotQuery(json);
			String nstQryPath = null;
			String key = getQueryConjTypeKey(conjType, isNot);
			JSONObject mtype = new JSONObject();
			if (!IUtils.isNullOrEmpty(key)) {
				if (json.has(IConstants.BOOL)) {
					mtype.put(key, json);
				} else {
					// json can be nested get it here.
					if(json.has(IConstants.NST_PTH_QRY)) {
						nstQryPath = json.optString(IConstants.NST_PTH_QRY);
						json.remove(IConstants.NST_PTH_QRY);
					}
					mtype.put(key, new JSONArray().put(json));
				}
			}
			bool.put(IConstants.BOOL, mtype);
			// handle nested query here.
			if (!IUtils.isNullOrEmpty(nstQryPath)) {
				qry = createNestedBoolQuery(nstQryPath, bool);
			} else {
				qry = bool;
			}
		} catch (JSONException e) {
			IConstants.logger.error(e.getMessage(), e);
		}
		return qry;
	}

	/**
	 * Utility method to add key, value in json
	 * @param json
	 * @param key
	 * @param value
	 * @return
	 */
	static JSONObject addKeyValInJson(JSONObject json, String key, Object value) {
		if (IUtils.isNull(json)) {
			json = new JSONObject();
		}
		try {
			json.put(key, value);
		} catch (JSONException e) {
			// ignore it.
		}
		return json;
	}

	/**
	 * Method to create a boolean query with json array.
	 * @param kw
	 * @param list
	 * @return
	 */
	static JSONObject createBoolQueryFor(Keywords kw, List<JSONObject> list) {
		if (!IUtils.isNull(list) && !list.isEmpty()) {
			JSONArray arr = new JSONArray();
			for (JSONObject obj : list) {
				arr.put(obj);
			}
			return createBoolQueryFor(kw, arr);
		}
		return null;
	}

	/**
	 * Method to create a boolean query with json array.
	 * @param kw
	 * @param arr
	 * @return
	 */
	static JSONObject createBoolQueryFor(Keywords kw, JSONArray arr) {
		if (IUtils.isNull(arr) || arr.length() == 0) {
			return null;
		}
		JSONObject bool = new JSONObject();
		try {
			String key = getQueryConjTypeKey(kw, false);
			JSONObject mtype = new JSONObject();
			mtype.put(key, arr);
			bool.put(IConstants.BOOL, mtype);
		} catch (JSONException e) {
			IConstants.logger.error(e.getMessage(), e);
		}
		return bool;
	}

	/**
	 * Method to create a nested query for bool query.
	 * @param nstPath
	 * @param bool
	 * @return
	 */
	static JSONObject createNestedBoolQuery(String nstPath, JSONObject bool) {
		if (!IUtils.isNullOrEmpty(nstPath)) {
			List<String> list = IUtils.getListFromString(nstPath, ".");
			JSONObject nst = null;
			for (int i = list.size(); i > 0; i--) {
				String path = getNestedPath(list, i);
				nst = createNestedObj(path, (IUtils.isNull(nst) ? bool : nst));
			}
			return nst;
		}
		return bool;
	}

	/**
	 * Method to create nested path object.
	 * @param list
	 * @param max
	 * @return
	 */
	static String getNestedPath(List<String> list, int max) {
		String path = "";
		for (int i = 0; i < max && i < list.size(); i ++) {
			if (IUtils.isNullOrEmpty(path)) {
				path += list.get(i);
			} else {
				path += "." + list.get(i);
			}
		}
		return path;
	}

	/**
	 * Method to create a nested query object
	 * @param path
	 * @param object
	 * @return
	 */
	static JSONObject createNestedObj(String path, JSONObject obj) {
		if (!IUtils.isNullOrEmpty(path) && !IUtils.isNull(obj)) {
			JSONObject json = new JSONObject();
			try {
				JSONObject nst = new JSONObject();
				nst.put(IConsts.PATH, path);
				nst.put(IConstants.QUERY, obj);
				// Finally add nested into json
				json.put(IConsts.NESTED, nst);
			} catch (JSONException e) {
				IConstants.logger.error(e.getMessage(), e);
			}
			return json;
		}
		return null;
	}

	/**
	 * Method to get query key for conjunction type
	 * @param conjType
	 * @param isNot
	 * @return
	 */
	static String getQueryConjTypeKey(Keywords conjType, boolean isNot) {
		String key = null;
		switch(conjType) {
		case AND:
			if (isNot) {
				key  = IConstants.MUST_NOT;
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
		return key;
	}

	/**
	 * Method to check if query is not type
	 * @param json
	 * @return
	 */
	static boolean isNotQuery(JSONObject json) {
		boolean res = false;
		if (!IUtils.isNull(json) && json.has(IConstants.NOT_QRY)) {
			res = json.optBoolean(IConstants.NOT_QRY);
			json.remove(IConstants.NOT_QRY);
		}
		return res;
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
		JSONObject json = null;
		try {
			JSONObject rngVal = new JSONObject();
			rngVal.put(dtOp, value);
			if (!IUtils.isNullOrEmpty(format)) {
				rngVal.put(IConstants.FORMAT, format);
			}
			json = createQuery(IConstants.RANGE, key, rngVal);
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
		case GreaterThan:
			key  = "gt";
			break;
		case GreaterThanEquals:
			key = "gte";
			break;
		case LessThan:
			key = "lt";
			break;
		case LessThanEquals:
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
	static boolean isInQuery(Keywords op, Keywords grpOp) {
		if ((op == Keywords.Equals || op == Keywords.NotEquals) &&
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
			List<Keywords> list = Keywords.listKeywords(KWTypes.GROUP);
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
			if (json.has(IConstants.PSD_STR_LEN)) {
				String val = json.getString(IConstants.PSD_STR_LEN);
				if (addSpace(key) || endWithConjOp(val)) {
					key = val + IConsts.SPACE + key;
				} else {
					key = val + key;
				}
			}
			json.put(IConstants.PSD_STR_LEN, key);
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
			List<Keywords> list = Keywords.listKeywords(KWTypes.CONJUNCTION);
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
				List<Keywords> list = Keywords.listKeywords(KWTypes.OPERATOR);
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
			List<Keywords> list = Keywords.listKeywords(KWTypes.CONJUNCTION);
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
			List<Keywords> list = Keywords.listKeywords(KWTypes.KEYWORD);
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
			List<Keywords> list = Keywords.listKeywords(KWTypes.FUNCTION);
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
			List<Keywords> list = Keywords.listKeywords(KWTypes.WILDCARD);
			for (Keywords kw : list) {
				if (value.contains(kw.getKey())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method to load SourceEntities Mappings fields if exists.
	 * @param rest
	 * @param searchHost
	 * @param clsName
	 */
	static Object fillIndexedKeys(RestTemplate rest, Environment env, String clsName) {

		String searchUrl = getSearchUrl(env, IConstants.GET_INDX_MAPPING_URI);
		IUtils.logger.info("searchUrl: " + searchUrl);
		Map<String, Object> params = IUtils.getRestParamMap(IConsts.PRM_CLASS,
				clsName, IConsts.PRM_FLD_ONLY, String.valueOf(true));
		IUtils.logger.info("Request: " + params);
		try {
			Object res = IUtils.sendPostRestRequest(rest,
					searchUrl, null, List.class,
					params, MediaType.APPLICATION_FORM_URLENCODED);
			IUtils.logger.info("Mappings response: " + res);
			return res;
		} catch (Exception ex) {
			IUtils.logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Method to load SourceEntities Mappings fields if exists.
	 * @param rest
	 * @param searchHost
	 */
	static void fillIndexedKeys(RestTemplate rest, Environment env) {
		String srcEnt = SourceEntity.class.getName();
		Object res = fillIndexedKeys(rest, env, srcEnt);
		if (!IUtils.isNull(res) && res instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> lst = (List<String>) res;
			if (!entityFields.containsKey(srcEnt)) {
				entityFields.put(srcEnt, lst);
			}
		}
	}

	/**
	 * Method to check if key match with Policy(.*)Policy rule.
	 * @param key
	 * @return
	 */
	static boolean isPolicy(String key) {
		if (!IUtils.isNullOrEmpty(key) &&
				(key.startsWith("Policy") || key.endsWith("Policy"))) {
			return true;
		}
		return false;
	}

	/**
	 * Create a nested key value with parent and key names.
	 * @param parent
	 * @param key
	 * @return
	 */
	static String getNestedKey(String parent, String key) {
		if (!IUtils.isNullOrEmpty(parent) && !IUtils.isNullOrEmpty(key)) {
			return parent + "." + key;
		}
		return key;
	}

	/**
	 * Method to generate and get search api urls.
	 * @param env
	 * @param srcApiPath
	 * @return
	 */
	static String getSearchUrl(Environment env, String srcApiPath) {
		String searchHost = env.getProperty(IConsts.KEY_SEARCH_URL, "");
		return searchHost + IApiController.URL_SEARCH + srcApiPath;
	}

	/**
	 * Method to execute function on entity
	 * @param entity
	 * @param key
	 * @param function
	 * @return
	 */
	static Object evalFunction(JSONObject entity, String key, String function) {
		Object res = null;
		if (!IUtils.isNullOrEmpty(function)) {
			switch(function) {
			case "length":
				// get length of key value
				String val = entity.optString(key);
				if (!IUtils.isNullOrEmpty(val)) {
					res = val.length();
				}
				break;
			}
		}
		return res;
	}

	/**
	 * Compare keyval with value and show result according to operator.
	 * @param keyVal
	 * @param operator
	 * @param value
	 * @return
	 */
	static boolean evalOperator(Object keyVal, String operator, Object value) {
		if (!IUtils.isNullOrEmpty(operator)) {
			CTypes tp = IUtils.getValueClassType(value);
			if (IUtils.isNull(tp) || CTypes.Boolean == tp) {
				tp = CTypes.String;
			}
			switch(operator) {
			case "<":
				return compareLessThan(tp, keyVal, value);
			case ">":
				return compareGreaterThan(tp, keyVal, value);
			case "=":
				return compareEquals(tp, keyVal, value);
			case "<=":
				return compareLessThanEqual(tp, keyVal, value);
			case ">=":
				return compareGreaterThanEqual(tp, keyVal, value);
			case "!=":
				return compareNotEquals(tp, keyVal, value);
			default:
				IUtils.logger.warn("Unsupported operator: '" + operator + "'");
			}
		}
		return false;
	}

	/**
	 * Method to compare value is less than or not, base on class type
	 * @param tp
	 * @param keyVal
	 * @param value
	 */
	static boolean compareLessThan(CTypes tp, Object keyVal, Object value) {
		switch(tp) {
		case Double:
			return (getDoubleVal(value).doubleValue() < getDoubleVal(keyVal).doubleValue());
		case Long:
		case Integer:
			return (getLongVal(value).longValue() < getLongVal(keyVal).longValue());
		default:
			break;
		}
		return false;
	}

	/**
	 * Method to compare value is greater than or not, base on class type
	 * @param tp
	 * @param keyVal
	 * @param value
	 */
	static boolean compareGreaterThan(CTypes tp, Object keyVal, Object value) {
		switch(tp) {
		case Double:
			return (getDoubleVal(value).doubleValue() > getDoubleVal(keyVal).doubleValue());
		case Long:
		case Integer:
			return (getLongVal(value).longValue() > getLongVal(keyVal).longValue());
		default:
			break;
		}
		return false;
	}

	/**
	 * Method to compare value is less than or equals, base on class type
	 * @param tp
	 * @param keyVal
	 * @param value
	 */
	static boolean compareLessThanEqual(CTypes tp, Object keyVal, Object value) {
		switch(tp) {
		case Double:
			return (getDoubleVal(value).doubleValue() <= getDoubleVal(keyVal).doubleValue());
		case Long:
		case Integer:
			return (getLongVal(value).longValue() <= getLongVal(keyVal).longValue());
		default:
			break;
		}
		return false;
	}

	/**
	 * Method to compare value is Greater than or equals, base on class type
	 * @param tp
	 * @param keyVal
	 * @param value
	 */
	static boolean compareGreaterThanEqual(CTypes tp, Object keyVal, Object value) {
		switch(tp) {
		case Double:
			return (getDoubleVal(value).doubleValue() >= getDoubleVal(keyVal).doubleValue());
		case Long:
		case Integer:
			return (getLongVal(value).longValue() >= getLongVal(keyVal).longValue());
		default:
			break;
		}
		return false;
	}

	/**
	 * Method to compare value is equals to keyval, base on class type
	 * @param tp
	 * @param keyVal
	 * @param value
	 */
	static boolean compareEquals(CTypes tp, Object keyVal, Object value) {
		switch(tp) {
		case Double:
			return (getDoubleVal(value).doubleValue() == getDoubleVal(keyVal).doubleValue());
		case Long:
		case Integer:
			return (getLongVal(value).longValue() == getLongVal(keyVal).longValue());
		case String:
			if (!IUtils.isNull(value)) {
				if (!IUtils.isNull(keyVal)) {
					return (String.valueOf(value)).contains(String.valueOf(keyVal));
				} else {
					return (String.valueOf(value)).equals(keyVal);
				}
			}
			break;
		default:
			break;
		}
		return false;
	}

	/**
	 * Parse value as double
	 * @param value
	 * @return
	 */
	static Double getDoubleVal(Object value) {
		if (!IUtils.isNull(value)) {
			try {
				return Double.valueOf(String.valueOf(value));
			} catch(Throwable th) {
				// ignore it.
			}
		}
		return null;
	}

	/**
	 * Parse value as long
	 * @param value
	 * @return
	 */
	static Long getLongVal(Object value) {
		if (!IUtils.isNull(value)) {
			try {
				return Long.valueOf(String.valueOf(value));
			} catch(Throwable th) {
				// ignore it.
			}
		}
		return null;
	}

	/**
	 * Method to compare value is NOT equals to keyval, base on class type
	 * @param tp
	 * @param keyVal
	 * @param value
	 */
	static boolean compareNotEquals(CTypes tp, Object keyVal, Object value) {
		switch(tp) {
		case Double:
			return (getDoubleVal(value).doubleValue() != getDoubleVal(keyVal).doubleValue());
		case Long:
		case Integer:
			return (getLongVal(value).longValue() != getLongVal(keyVal).longValue());
		case String:
			if (!IUtils.isNull(value)) {
				if (!IUtils.isNull(keyVal)) {
					return ! (String.valueOf(value)).contains(String.valueOf(keyVal));
				} else {
					return ! (String.valueOf(value)).equals(keyVal);
				}
			}
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * Method to evaluate if value exists in entity.
	 * @param value
	 * @param entity
	 * @return
	 */
	static boolean evalFullText(Value value, JSONObject entity) {
		boolean exists = false;
		Map<String, Object> map = IUtils.getMapFromJson(entity);
		if (!IUtils.isNull(map) && !IUtils.isNull(map.values())) {
			//exists = map.values().contains(value.getVal());
			for (Object val : map.values()) {
				if (!IUtils.isNull(val)) {
					String v = String.valueOf(val);
					if (!IUtils.isNullOrEmpty(v) &&
							v.contains(value.getVal())) {
						return true;
					}
				}
			}
		}
		return exists;
	}

	/**
	 * Method to get value of specified key to be sure 
	 * @param entity
	 * @param key
	 * @param nested
	 * @return
	 */
	static Object getValueOfKey(JSONObject entity, String key, boolean nested) {
		Object val = null;
		if (nested) {
			JSONObject jobj = entity;
			List<String> lst = IUtils.getListFromString(key, ".");
			for (int i = 0; i < (lst.size() - 1); i ++) {
				jobj = jobj.optJSONObject(lst.get(i));
			}
			val = jobj.opt(lst.get(lst.size() - 1));
		} else {
			val = entity.opt(key);
		}
		return val;
	}

	/**
	 * Method to get long time for toDate method.
	 * @param val
	 * @param format
	 * @return
	 */
	static long getLongTime(String val, String format) {
		Long lngDt = null;
		if (!IUtils.isNullOrEmpty(val)) {
			Date dt = IUtils.parseDate(val, null, format);
			if (!IUtils.isNull(dt)) {
				lngDt = dt.getTime();
			}
		}
		return lngDt;
	}

	/**
	 * Method to set evaluation message for operators.
	 * @param msgs
	 * @param key
	 * @param isLen
	 * @param isPass
	 * @param isNeg
	 * @param opName
	 * @return 
	 */
	static StringBuilder setEvalMsg(StringBuilder msgs, String key, boolean isLen,
			boolean isPass, boolean isNeg, String opName) {
		msgs.append(msgs.length() > 0 ? ", " : "");
		msgs.append("'" + key + (isLen ? "' length" : "'") + " is ");
		if (!isPass && isNeg) {
			msgs.append(opName.replace("Not", ""));
		} else {
			msgs.append((isPass ? "" : "NOT ") + opName);
		}
		msgs.append(".");
		return msgs;
	}

	/**
	 * Method to evaluate the regex key value for value
	 * @param kVal
	 * @param val
	 * @return
	 */
	static boolean evalRegex(String kVal, String val) {
		if (!IUtils.isNullOrEmpty(val) && !IUtils.isNull(val)) {
			return val.matches(kVal);
		}
		return false;
	}

	/**
	 * Method to create a basic authentication header.
	 * @param user
	 * @param pass
	 * @return
	 */
	static Map<String, String> getAuthHeader(String user, String pass) {
		String encoding = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
		IUtils.logger.info("Auth: " + encoding);
		return IUtils.getRestParamMap(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
	}

	/**
	 * Method to create a Gelf Entity object using post request.
	 * @param <T>
	 * @param url
	 * @param reqObj
	 * @param user
	 * @param pass
	 * @param repo
	 * @param cls
	 * @return
	 */
	static <T extends PSqlEntity> String createGelfEntity(
			String url, String reqObj, String user, String pass, CrudRepository<T, Long> repo, Class<T> cls) {
		String id = null;
		try {
			Map<String, String> hdrs = getAuthHeader(user, pass);
			hdrs.put(HttpHeaders.CONTENT_TYPE, "application/json");
			hdrs.put("X-Requested-By", "localhost");
			Object res = IUtils.sendPostJsonDataReq(url, hdrs, reqObj);
			IUtils.logger.info("Res: " + res);
			if (!IUtils.isNull(res)) {
				JSONObject json = IUtils.getJSONObject(res.toString());
				id = json.optString("id");
				json.remove("id");
				json.put("gelfId", id);
				T obj = IUtils.OBJECT_MAPPER.readValue(json.toString(), cls);
				repo.save(obj);
			}
		} catch (Exception e) {
			IUtils.logger.error(e.getMessage(), e);
		}
		return id;
	}
}
