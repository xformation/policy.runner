/**
 * 
 */
package com.synectiks.policy.runner.translators;

import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;
import com.synectiks.policy.runner.utils.NestedString;

/**
 * @author Rajesh
 */
public class QueryParser implements IConstants {

	private static Logger logger = LoggerFactory.getLogger(QueryParser.class);

	private String query = null;

	public QueryParser(String qry) {
		query = IUtils.refineQueryString(qry);
	}

	/**
	 * Method to start parsing of query string.
	 * @return
	 */
	public JSONObject parse() {
		return processQuery(query, Keywords.AND);
	}

	/**
	 * Method to process query string
	 * @param qry
	 * @param conjType
	 * @return
	 */
	private JSONObject processQuery(String qry, Keywords conjType) {
		if (IUtils.isNullOrEmpty(qry))
			return null;
		JSONObject result = new JSONObject();
		logger.info("Started processing: " + query);
		while (qry.length() > 0) {
			JSONObject exprs = null;
			if (isStartWithGroup(qry)) {
				exprs = handleGroupQuery(qry);
				// TODO
			} else if (isStartWithConjuction(qry)) {
				Keywords conj = getConjuncOperator(qry);
				processConjucOperation();
				// TODO
			} else if (isStartWithHasKeyword(qry)) {
				qry = IUtilities.removeProcessedString(qry, Keywords.HAS.getKey());
				String key = IUtilities.getFirstString(qry);
				JSONObject json = IUtilities.createQuery(EXISTS, FIELD, key);
				exprs = IUtilities.createBoolQueryFor(conjType, json);
				// update query text after removing the processed part.
				qry = IUtilities.removeProcessedString(qry, key);
			} else if (haveOperator(qry)) {
				String tkey = IUtilities.getFirstString(qry);
				// update query to remove the processed part.
				qry = IUtilities.removeProcessedString(qry, tkey);
				Keywords operator = getOperator(qry, false);
				qry = IUtilities.removeProcessedString(qry, operator.getKey());
				String value = extractValue(qry, false);
				exprs = processOperatorQuery(conjType, tkey, operator, value);
			} else if (haveFunction(qry)) {
				String tkey = IUtilities.getFirstString(qry);
				// update query to remove the processed part.
				qry = IUtilities.removeProcessedString(qry, tkey);
				Keywords func = getFunction(qry, false);
				qry = IUtilities.removeProcessedString(qry, func.getKey());
				String groupValue = extractValue(qry, true);
				qry = IUtilities.removeProcessedString(qry, groupValue);
				exprs = processFunctionQuery(conjType, tkey, func, groupValue);
			} else {
				// We have got direct value so make match all query
				String key = qry;
				JSONObject json = IUtilities.createQuery(MATCH, _All, key);
				exprs = IUtilities.createBoolQueryFor(conjType, json);
				// update query text after removing the processed part.
				qry = IUtilities.removeProcessedString(qry, key);
			}
			result = IUtils.deepMerge(exprs, result);
		}
		logger.info("End processing with result: " + result.toString());
		return result;
	}

	/**
	 * Method to process group string.
	 * @param qry
	 * @return
	 */
	private JSONObject handleGroupQuery(String qry) {
		if (!IUtils.isNull(qry) && isStartWithGroup(qry)) {
			Keywords grp = getStartWithGroup(qry);
			if (grp == Keywords.CptlBrkt) { // This is case of multi_search
				// process multi-field search;
				return processMultiMatchSearch(qry);
			} else if (grp == Keywords.SmlBrkt) {
				String grpStr = IUtilities.getGroupValue(qry, grp, true);
				int indx = grpStr.length();
				qry = IUtilities.removeProcessedString(qry, grpStr);
				grpStr = IUtilities.getGroupValue(grpStr, grp, false);
				JSONObject json = processQuery(grpStr, conjType);
			} else {
				logger.warn("Unsupported Group operator '{0}' found.", grp);
			}
		}
		return null;
	}

	private JSONObject processMultiMatchSearch(String query) {
		if (!IUtils.isNull(query)) {
			String qry = query;
			int indx = 0;
			String grpKey = IUtilities.getGroupValue(qry, Keywords.CptlBrkt, true);
			indx = grpKey.length();
			qry = IUtilities.removeProcessedString(qry, grpKey);
			grpKey = IUtilities.getGroupValue(grpKey, Keywords.CptlBrkt, false);
			JSONArray jarr = IUtilities.getJArrFromString(grpKey, false);
			boolean isMust = false;
			if (qry.startsWith(Keywords.MUST.getKey())) {
				isMust = true;
				indx += query.indexOf(Keywords.MUST.getKey()) + 1;
				qry = IUtilities.removeProcessedString(qry, Keywords.MUST.getKey());
			}
			String grpVal = null;
			if (isStartWithGroup(qry)) {
				Keywords grp = getStartWithGroup(qry);
				if (!IUtils.isNull(grp) && grp == Keywords.SmlBrkt) {
					grpVal = IUtilities.getGroupValue(qry, grp, true);
					indx += grpVal.length();
					qry = IUtilities.removeProcessedString(qry, grpVal);
					grpVal = IUtilities.getGroupValue(qry, grp, false);
				}
			} else {
				grpVal = IUtilities.getFirstString(qry);
				indx += grpVal.length();
			}
			JSONObject json = IUtilities.createMultiSearchQuery(jarr, grpVal, isMust);
			IUtilities.addLength(json, indx);
			return json;
		}
		return null;
	}

	/**
	 * Method to process function query to convert into elastic query
	 * @param conjType
	 * @param key
	 * @param operator
	 * @param value
	 * @return
	 */
	private JSONObject processOperatorQuery(Keywords conjType, String key,
			Keywords operator, String value) {
		if (!IUtils.isNullOrEmpty(key)) {
			boolean isGrp = false;
			// remove start and end of group operator if any
			if (isStartWithGroup(value)) {
				Keywords grpOp = getStartWithGroup(value);
				value = IUtilities.getGroupValue(value, grpOp, false);
				isGrp = IUtilities.isInQquery(operator, grpOp);
			}
			// get function if value has any
			Keywords func = getFunction(value, false);
			// Check if value is numeric
			boolean isNum = IUtilities.isNumeric(value, isGrp);
			logger.info("key: {0}, operator: {1}\n value: {2}\nNumber: {3}\nGroup: {4}",
					key, operator, value, isNum, isGrp);
			JSONObject json = null;
			boolean isNot = false;
			switch (operator) {
			case EQ:
			case GT:
			case GTE:
			case LT:
			case LTE:
				json = createOperatorQuery(operator, key, value, func, isNum, isGrp);
				break;
			case NE:
				isNot = true;
				json = createOperatorQuery(operator, key, value, func, isNum, isGrp);
				break;
			default:
				logger.warn("Unsupported operator '{0}' found.", operator.getKey());
				break;
			}
			return IUtilities.createBoolQueryFor(conjType, isNot, json);
		}
		return null;
	}

	/**
	 * Method to create elastic query for equals operator.
	 * @param op
	 * @param key
	 * @param value
	 * @param func
	 * @param isNum
	 * @param isGrp 
	 * @return
	 */
	private JSONObject createOperatorQuery(Keywords op, String key, String value,
			Keywords func, boolean isNum, boolean isGrp) {
		JSONObject json = null;
		if (!IUtils.isNullOrEmpty(value)) {
			if (!IUtils.isNull(func)) {
				json = createDateMatchQuery(op, func, key, value);
			} else if (isNum) {
				json = createNumberQuery(op, key, value, isGrp);
			} else if (hasWildcard(value)) {
				json = IUtilities.createQuery(IConstants.WILDCARD, key, value);
			} else {
				Object val = null;
				if (isGrp) {
					// It will be a IN query
					val = IUtilities.getJArrFromString(value, false);
				} else {
					val = value;
				}
				json = IUtilities.createQuery(IConstants.MATCH, key, val);
			}
		}
		return json;
	}

	/**
	 * Method to create elastic query for numeric value
	 * @param op
	 * @param key
	 * @param isGrp 
	 * @param num
	 * @return
	 */
	private JSONObject createNumberQuery(
			Keywords op, String key, String value, boolean isGrp) {
		JSONObject json = null;
		if (op == Keywords.EQ || op == Keywords.NE) {
			Object num = null;
			if (isGrp) {
				// It will be a IN query
				num = IUtilities.getJArrFromString(value, true);
			} else {
				num = value;
			}
			json = IUtilities.createQuery(IConstants.MATCH, key, num);
		}
		String dtOp = IUtilities.getESOperatorKey(op);
		if (IUtils.isNull(json) && !IUtils.isNull(dtOp)) {
			json = IUtilities.createRangeQuery(key, value, null, dtOp);
		}
		return json;
	}

	/**
	 * Method to check if value has any wild card
	 * @param value
	 * @return
	 */
	private boolean hasWildcard(final String value) {
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

	/**
	 * Method to get date or range query json
	 * @param op
	 * @param func
	 * @param key
	 * @param value
	 * @return
	 */
	private JSONObject createDateMatchQuery(Keywords op, Keywords func, String key,
			String value) {
		JSONObject json = null;
		if (!IUtils.isNullOrEmpty(value)) {
			String val = null, format = null;
			// We will entertain only todate function here.
			if (func == Keywords.TODATE) {
				String grpVal = IUtilities.getGroupValue(value, Keywords.SmlBrkt, false);
				// check if user has provided format
				if (!IUtils.isNullOrEmpty(grpVal) && grpVal.contains(",")) {
					// assume first group as date and second is date format.
					String[] arr = grpVal.split(",");
					if (!IUtils.isNull(arr) && arr.length >= 2) {
						val = IUtilities.getGroupValue(arr[0], Keywords.SnglQuote, false);
						format = IUtilities.getGroupValue(arr[1], Keywords.SnglQuote,
								false);
					}
				} else {
					val = grpVal;
				}
			} else {
				logger.warn("Function '" + func + "' is not supported here.");
			}
			json = createDateQuery(key, value, format,
					IUtils.parseDate(val, null, format), op);
		}
		return json;
	}

	/**
	 * Method to get date match or range query.
	 * @param key
	 * @param value
	 * @param format
	 * @param date
	 * @param op
	 * @return
	 */
	private JSONObject createDateQuery(
			String key, String value, String format, Date date, Keywords op) {
		JSONObject json = null;
		if (op == Keywords.EQ || op == Keywords.NE) {
			if (!IUtils.isNull(date)) {
				json = IUtilities.createQuery(IConstants.MATCH, key, date.getTime());
			} else {
				// We failed to parse it just add it what it is
				// and let elastic search handle it.
				json = IUtilities.createQuery(IConstants.MATCH, key, value);
			}
		}
		String dtOp = IUtilities.getESOperatorKey(op);
		if (IUtils.isNull(json) && !IUtils.isNull(dtOp)) {
			json = IUtilities.createRangeQuery(key, value, null, dtOp);
		}
		return json;
	}

	/**
	 * Method to extract value from qry.
	 * @param qry
	 * @param isGroup send true if only group value expected.
	 * @return
	 */
	private String extractValue(String qry, boolean isGroup) {
		String value = null;
		if (!IUtils.isNullOrEmpty(qry)) {
			boolean hasMust = false;
			if (qry.startsWith(Keywords.MUST.getKey())) {
				hasMust = true;
				qry = IUtilities.removeProcessedString(qry, Keywords.MUST.getKey());
			}
			Keywords func = null;
			if (isStartWithFunction(qry)) {
				func = getFunction(qry, false);
				qry = IUtilities.removeProcessedString(qry, func.getKey());
			}
			// Extract value now
			if (isStartWithGroup(qry)) {
				value = IUtilities.getGroupValue(qry, getStartWithGroup(qry), true);
				if (!IUtils.isNull(func)) {
					value = func.getKey() + value;
				}
			} else if (!isGroup) {
				value = IUtilities.getFirstString(qry);
			}
			// add must clause in value again
			if (!IUtils.isNullOrEmpty(value) && hasMust) {
				value = Keywords.MUST.getKey() + value;
			}
		}
		return value;
	}

	/**
	 * Check if qry starts with an operator
	 * @param qry
	 * @return
	 */
	private boolean haveOperator(String qry) {
		return !IUtils.isNull(getOperator(qry, true));
	}

	/**
	 * Check if qry starts with an operator
	 * @param qry
	 * @return
	 */
	private boolean isStartWithOperator(String qry) {
		return !IUtils.isNull(getOperator(qry, false));
	}

	/**
	 * Method to return operator if qry starts with any
	 * @param qry
	 * @param hasKey
	 * @return
	 */
	private Keywords getOperator(String qry, boolean hasKey) {
		if (!IUtils.isNullOrEmpty(qry)) {
			if (hasKey) {
				// remove first string assume its query.
				String key = IUtilities.getFirstString(qry);
				if (!IUtils.isNullOrEmpty(key)) {
					qry = qry.substring(key.length() + 1);
				}
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
	 * Check if qry starts with group operator
	 * @param qry
	 * @return
	 */
	private boolean isStartWithGroup(String qry) {
		return !IUtils.isNull(getStartWithGroup(qry));
	}

	/**
	 * Method to return group operator if qry starts with any
	 * @param qry
	 * @return
	 */
	private Keywords getStartWithGroup(String qry) {
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
	 * Method to process function query to get elastic query.
	 * @param conjType
	 * @param key
	 * @param func
	 * @param groupValue
	 * @return
	 */
	private JSONObject processFunctionQuery(Keywords conjType, String key, Keywords func,
			String groupValue) {
		JSONObject json = null;
		if (!IUtils.isNullOrEmpty(key))
			return json;
		if (!IUtils.isNull(func))
			return json;
		// remove start and end small brakets.
		groupValue = IUtilities.getGroupValue(groupValue, Keywords.SmlBrkt, false);
		logger.info("key: {0}, function: {1}\n groupValue: {2}", key, func, groupValue);
		boolean isNot = false;
		switch (func) {
		case ISNULL:
		case ISEMPTY:
			isNot = true;
			json = IUtilities.createQuery(EXISTS, FIELD, key);
			break;
		case ISNOTEMPTY:
		case ISNOTNULL:
			json = IUtilities.createQuery(EXISTS, FIELD, key);
			break;
		case REGEX:
			json = IUtilities.createRegexQuery(key, groupValue);
			break;
		case TODATE:
			// We should not expect todate method without operator.
			break;
		default:
			logger.warn("Unsupported function '{0}' found.", func.getKey());
			break;
		}
		return IUtilities.createBoolQueryFor(conjType, isNot, json);
	}

	/**
	 * Method to check if query has a function just after key
	 * @param qry
	 * @return
	 */
	private boolean isStartWithFunction(String qry) {
		return !IUtils.isNull(getFunction(qry, false));
	}

	/**
	 * Method to check if query has a function just after key
	 * @param qry
	 * @return
	 */
	private boolean haveFunction(String qry) {
		return !IUtils.isNull(getFunction(qry, true));
	}

	/**
	 * Method to find if query has an operator after key, if matches then
	 * returns the operator or returns null.
	 * @param qry
	 * @param hasKey
	 * @return
	 */
	private Keywords getFunction(String qry, boolean hasKey) {
		if (!IUtils.isNullOrEmpty(qry)) {
			if (hasKey) {
				// remove first string assume its query.
				String key = IUtilities.getFirstString(qry);
				if (!IUtils.isNullOrEmpty(key)) {
					qry = qry.substring(key.length() + 1);
				}
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
	 * Check if query starts with has keyword.
	 * @param qry
	 * @return
	 */
	private boolean isStartWithHasKeyword(String qry) {
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
	 * Check if query starts with conjunction operator.
	 * @param qry
	 * @return
	 */
	private boolean isStartWithConjuction(String qry) {
		return !IUtils.isNull(getConjuncOperator(qry));
	}

	/**
	 * Method to find if query starts with any conjunction operator, if matches
	 * then returns the operator or returns null.
	 * @param qry
	 * @return
	 */
	private Keywords getConjuncOperator(String qry) {
		if (!IUtils.isNullOrEmpty(qry)) {
			List<Keywords> list = Keywords.list(KWTypes.CONJUNCTION);
			for (Keywords kw : list) {
				if (query.startsWith(kw.getKey() + IConsts.SPACE)) {
					return kw;
				}
			}
		}
		return null;
	}

}
