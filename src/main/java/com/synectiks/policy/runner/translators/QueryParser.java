/**
 * 
 */
package com.synectiks.policy.runner.translators;

import java.util.List;

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

			} else if (isStartWithConjuction(qry)) {
				Keywords conj = getConjuncOperator(qry);
				processConjucOperation();
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
			} else if (isStartWithHasKeyword(qry)) {
				qry = IUtilities.removeProcessedString(qry, Keywords.HAS.getKey());
				String key = IUtilities.getFirstString(qry);
				JSONObject json = IUtilities.createQuery(EXISTS, FIELD, key);
				exprs = IUtilities.createBoolQueryFor(conjType, json);
				// update query text after removing the processed part.
				qry = IUtilities.removeProcessedString(qry, key);
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
	 * Method to process function query to convert into elastic query
	 * @param conjType
	 * @param key
	 * @param operator
	 * @param value
	 * @return
	 */
	private JSONObject processOperatorQuery(Keywords conjType,
			String key, Keywords operator, String value) {
		if (!IUtils.isNullOrEmpty(key)) {
			boolean hasMust = false;
			if (!IUtils.isNullOrEmpty(value) &&
					value.startsWith(Keywords.MUST.getKey())) {
				hasMust = true;
				value = IUtilities.removeProcessedString(value, Keywords.MUST.getKey());
			}
			// remove start and end of group operator if any
			if (isStartWithGroup(value)) {
				Keywords grpOp = getStartWithGroup(value);
				value = IUtilities.getGroupValue(value, grpOp, false);
			}
			logger.info("key: {0}, operator: {1}\n value: {2}", key, operator, value);
			boolean isNot = false;
			switch(operator) {
			case EQ:
			case GT:
			case GTE:
			case LT:
			case LTE:
			case NE:
			default:
				logger.warn("Unsupported operator '{0}' found.", operator.getKey());
				break;
			}
		}
		return null;
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
				value = IUtilities.getGroupValue(
						qry, getStartWithGroup(qry), true);
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
				if (qry.startsWith(kw.getKey() + IConsts.SPACE)) {
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