/**
 * 
 */
package com.synectiks.policy.runner.translators;

import java.util.Date;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.EvalCheck;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * @author Rajesh
 */
public class QueryParser implements IConstants {

	private static Logger logger = LoggerFactory.getLogger(QueryParser.class);

	private String query = null;
	private boolean translate = true;

	public QueryParser(String qry) {
		query = IUtils.refineQueryString(qry);
	}

	public QueryParser(String qry, boolean translate) {
		query = IUtils.refineQueryString(qry);
		this.translate = translate;
	}

	/**
	 * Method to start parsing of query string.
	 * @return
	 */
	public JSONObject parse() {
		return processQuery(query);
		/*
		JSONObject json = processQuery(query);
		// ELS 5.5+
		if (!IUtils.isNull(json)) {
			try {
				return new JSONObject().put(IConstants.QUERY, json);
			} catch (JSONException e) {
				// ignore it.
			}
		}
		return null;*/
	}

	/**
	 * Method to process query string
	 * @param qry
	 * @param conjType
	 * @return
	 */
	private JSONObject processQuery(String qry) {
		if (IUtils.isNullOrEmpty(qry))
			return null;
		Keywords conjType = null;
		JSONObject result = null;
		logger.info("Started processing: " + qry);
		while (qry.length() > 0) {
			JSONObject exprs = null;
			if (IUtilities.isStartWithGroup(qry)) {
				logger.info("Group: ");
				exprs = handleGroupQuery(qry);
				if (!IUtils.isNull(exprs) && exprs.has(IConstants.LENGTH)) {
					qry = IUtilities.removeProcessedString(
							qry, exprs.optString(IConstants.LENGTH));
					exprs.remove(IConstants.LENGTH);
				}
			} else if (IUtilities.isStartWithConjuction(qry)) {
				logger.info("Conjunction: ");
				Keywords conj = IUtilities.getConjuncOperator(qry);
				qry = IUtilities.removeProcessedString(qry, conj.getKey());
				// update existing queries to use same operator
				if (IUtils.isNull(conjType)) {
					result = IUtilities.createBoolQueryFor(conj, result);
				}
				// Set it back to conjType;
				conjType = conj;
			} else if (IUtilities.isStartWithHasKeyword(qry)) {
				logger.info("HAS: ");
				qry = IUtilities.removeProcessedString(qry, Keywords.HAS.getKey());
				String key = IUtilities.getFirstString(qry);
				exprs = IUtilities.createQuery(EXISTS, FIELD, key);
				// update query text after removing the processed part.
				qry = IUtilities.removeProcessedString(qry, key);
			} else if (IUtilities.haveOperator(qry)) {
				logger.info("Operator: ");
				String tkey = IUtilities.getFirstString(qry);
				// update query to remove the processed part.
				qry = IUtilities.removeProcessedString(qry, tkey);
				Keywords operator = IUtilities.getOperator(qry, false);
				qry = IUtilities.removeProcessedString(qry, operator.getKey());
				String value = extractValue(qry, false);
				qry = IUtilities.removeProcessedString(qry, value);
				exprs = processOperatorQuery(conjType, tkey, operator, value);
			} else if (IUtilities.haveFunction(qry)) {
				logger.info("Function: ");
				String tkey = IUtilities.getFirstString(qry);
				// update query to remove the processed part.
				qry = IUtilities.removeProcessedString(qry, tkey);
				Keywords func = IUtilities.getFunction(qry, false);
				String groupValue = extractValue(qry, true);
				if (!IUtils.isNullOrEmpty(groupValue)) {
					qry = IUtilities.removeProcessedString(qry, groupValue);
				} else {
					qry = IUtilities.removeProcessedString(qry, func.getKey());
				}
				if (translate) {
					exprs = processFunctionQuery(conjType, tkey, func, groupValue);
				} else {
					// remove start and end small brakets.
					groupValue = IUtilities.getGroupValue(
							groupValue, Keywords.SmlBrkt, false);
					exprs = IUtils.getJSONObject(
							EvalCheck.create(tkey, func, groupValue).toString());
				}
			} else {
				logger.info("Value: ");
				// We have got direct value so make match all query
				if (translate) {
					exprs = IUtilities.createQuery(MATCH, _All, qry);
				} else {
					exprs = IUtils.getJSONObject(
							EvalCheck.create("*", Keywords.EQ, qry).toString());
				}
				// we have processed whole qry text so set it empty.
				qry = IConsts.EMPTY;
			}
			if (translate) {
				result = IUtils.deepMerge(exprs, result);
			} else {
				result = exprs;
			}
		}
		if (translate) {
			// Make it must query if there is no conjunction type exists.
			if (IUtils.isNull(conjType) && !result.has(IConstants.BOOL)) {
				result = IUtilities.createBoolQueryFor(Keywords.AND, result);
			}
		}
		logger.info("End processing with result: " + result);
		return result;
	}

	/**
	 * Method to process group string.
	 * @param qry
	 * @return
	 */
	private JSONObject handleGroupQuery(String qry) {
		if (!IUtils.isNull(qry) && IUtilities.isStartWithGroup(qry)) {
			Keywords grp = IUtilities.getStartWithGroup(qry);
			if (grp == Keywords.CptlBrkt) { // This is case of multi_search
				// process multi-field search;
				return processMultiMatchSearch(qry);
			} else if (grp == Keywords.SmlBrkt) {
				JSONObject json = new JSONObject();
				String grpStr = IUtilities.getGroupValue(qry, grp, true);
				IUtilities.addProcessedKey(json, grpStr);
				qry = IUtilities.removeProcessedString(qry, grpStr);
				grpStr = IUtilities.getGroupValue(grpStr, grp, false);
				if (IUtilities.isStartWithConjuction(qry)) {
					Keywords conjType = IUtilities.getConjuncOperator(qry);
					qry = IUtilities.removeProcessedString(qry, conjType.getKey());
					IUtilities.addProcessedKey(json, conjType.getKey());
					JSONObject rhs = processQuery(grpStr);
					JSONObject lhs = null;
					if (IUtilities.isStartWithGroup(qry)) {
						lhs = handleGroupQuery(qry);
						if (!IUtils.isNull(lhs) && lhs.has(IConstants.LENGTH)) {
							IUtilities.addProcessedKey(json,
									lhs.optString(IConstants.LENGTH));
							lhs.remove(IConstants.LENGTH);
						}
					} else {
						lhs = processQuery(qry);
					}
					logger.info("\nrhs: {}\nlhs: {}", rhs, lhs);
					JSONObject temp = IUtils.deepMerge(rhs, lhs);
					logger.info("\nmerged: {}", temp);
					// add super level conjunction operator in query.
					json = IUtils.deepMerge(
							IUtilities.createBoolQueryFor(conjType, temp), json);
					logger.info("\nfinal: {}", json);
				} else {
					JSONObject expr = processQuery(grpStr);
					json = IUtils.deepMerge(expr, json);
				}
				return json;
			} else {
				logger.warn("Unsupported Group operator '{}' found.", grp);
			}
		}
		return null;
	}

	/**
	 * Method to generate multi-match query from string
	 * @param qry
	 * @return
	 */
	private JSONObject processMultiMatchSearch(String qry) {
		if (!IUtils.isNullOrEmpty(qry)) {
			JSONObject json = new JSONObject();
			String grpKey = IUtilities.getGroupValue(qry, Keywords.CptlBrkt, true);
			IUtilities.addProcessedKey(json, grpKey);
			qry = IUtilities.removeProcessedString(qry, grpKey);
			grpKey = IUtilities.getGroupValue(grpKey, Keywords.CptlBrkt, false);
			JSONArray jarr = IUtilities.getJArrFromString(grpKey, false);
			boolean isMust = false;
			if (qry.startsWith(Keywords.MUST.getKey())) {
				isMust = true;
				IUtilities.addProcessedKey(json, Keywords.MUST.getKey());
				qry = IUtilities.removeProcessedString(qry, Keywords.MUST.getKey());
			}
			String grpVal = null;
			if (IUtilities.isStartWithGroup(qry)) {
				Keywords grp = IUtilities.getStartWithGroup(qry);
				if (!IUtils.isNull(grp) && grp == Keywords.SmlBrkt) {
					grpVal = IUtilities.getGroupValue(qry, grp, true);
					IUtilities.addProcessedKey(json, grpVal);
					qry = IUtilities.removeProcessedString(qry, grpVal);
					grpVal = IUtilities.getGroupValue(qry, grp, false);
				} else {
					grpVal = IUtilities.getGroupValue(qry, grp, true);
					IUtilities.addProcessedKey(json, grpVal);
					grpVal = IUtilities.getGroupValue(qry, grp, false);
				}
			} else {
				grpVal = IUtilities.getFirstString(qry);
				IUtilities.addProcessedKey(json, grpVal);
			}
			json = IUtils.deepMerge(
					IUtilities.createMultiSearchQuery(
							jarr, grpVal, isMust), json);
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
			if (IUtilities.isStartWithGroup(value)) {
				Keywords grpOp = IUtilities.getStartWithGroup(value);
				value = IUtilities.getGroupValue(value, grpOp, false);
				isGrp = IUtilities.isInQuery(operator, grpOp);
			}
			// get function if value has any
			Keywords func = IUtilities.getFunction(value, false);
			// Check if value is numeric
			boolean isNum = IUtilities.isNumeric(value, isGrp);
			logger.info("key: {}, operator: {}\n value: {}\nNumber: {}\nGroup: {}",
					key, operator, value, isNum, isGrp);
			JSONObject json = null;
			switch (operator) {
			case EQ:
			case GT:
			case GTE:
			case LT:
			case LTE:
				json = createOperatorQuery(operator, key, value, func, isNum, isGrp);
				break;
			case NE:
				json = createOperatorQuery(operator, key, value, func, isNum, isGrp);
				try {
					json.put(IConstants.NOT_QRY, true);
				} catch (JSONException e) {
					// ignore it.
				}
				break;
			default:
				logger.warn("Unsupported operator '{}' found.", operator.getKey());
				break;
			}
			if (!IUtils.isNull(conjType)) {
				return IUtilities.createBoolQueryFor(conjType, json);
			} else {
				return json;
			}
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
			} else if (IUtilities.hasWildcard(value)) {
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
					if (IUtilities.isStartWithGroup(grpVal)) {
						Keywords grp = IUtilities.getStartWithGroup(grpVal);
						val = IUtilities.getGroupValue(grpVal, grp, false);
					} else {
						val = grpVal;
					}
				}
			} else {
				logger.warn("Function '" + func + "' is not supported here.");
			}
			json = createDateQuery(key, val, format,
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
			json = IUtilities.createRangeQuery(key, value, format, dtOp);
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
			if (IUtilities.isStartWithFunction(qry)) {
				func = IUtilities.getFunction(qry, false);
				qry = IUtilities.removeProcessedString(qry, func.getKey());
			}
			// Extract value now
			if (IUtilities.isStartWithGroup(qry)) {
				Keywords grp = IUtilities.getStartWithGroup(qry);
				value = IUtilities.getGroupValue(qry, grp, true);
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
		if (IUtils.isNullOrEmpty(key))
			return json;
		if (IUtils.isNull(func))
			return json;
		// remove start and end small brakets.
		groupValue = IUtilities.getGroupValue(groupValue, Keywords.SmlBrkt, false);
		logger.info("key: {}, function: {}\n groupValue: {}", key, func, groupValue);
		switch (func) {
		case ISNULL:
		case ISEMPTY:
			json = IUtilities.createQuery(EXISTS, FIELD, key);
			try {
				json.put(IConstants.NOT_QRY, true);
			} catch (JSONException e) {
				// ignore it.
			}
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
			logger.warn("Unsupported function '{}' found.", func.getKey());
			break;
		}
		if (!IUtils.isNull(conjType)) {
			return IUtilities.createBoolQueryFor(conjType, json);
		} else {
			return json;
		}
	}

}