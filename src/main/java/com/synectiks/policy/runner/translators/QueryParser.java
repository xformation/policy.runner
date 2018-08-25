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
			String key = null;
			JSONObject exprs = null;
			if (isStartWithGroup(qry)) {
				
			} else if (isStartWithConjuction(qry)) {
				Keywords conj = getConjuncOperator(qry);
				processConjucOperation();
			} else if (haveOperator(qry)) {
				
			} else if (haveFunction(qry)) {
				String tkey = IUtilities.getFirstString(qry);
				Keywords func = getFunction(qry);
				// update query to remove the processed part.
				qry = IUtilities.removeProcessedString(qry, tkey);
				qry = IUtilities.removeProcessedString(qry, func.getKey());
				String groupValue = getGroupStringValue(qry, Keywords.SmlBrkt);
				qry = IUtilities.removeProcessedString(qry, groupValue);
				
				exprs = processFunctionQuery(qry, tkey, func, groupValue);
			} else if (isStartWithHasKeyword(qry)) {
				qry = IUtilities.removeProcessedString(qry, Keywords.HAS.getKey());
				key = IUtilities.getFirstString(qry);
				exprs = IConstants.createQuery(EXISTS, FIELD, key);
			} else {
				// We have got direct value so make match all query
				key = qry;
				exprs = IConstants.createQuery(MATCH, _All, key);
			}
			// update query text after removing the processed part.
			qry = IUtilities.removeProcessedString(qry, key);
			result = IUtils.deepMerge(exprs, result);
		}
		logger.info("End processing with result: " + result.toString());
		return result;
	}

	/**
	 * Method to process function query to get elastic query.
	 * @param key 
	 * @param func 
	 * @param groupValue 
	 * @return
	 */
	private JSONObject processFunctionQuery(
			String key, Keywords func, String groupValue) {
		JSONObject json = null;
		if (!IUtils.isNullOrEmpty(key)) return json;
		if (!IUtils.isNull(func)) return json;
		logger.info("key: {0}, function: {1}\n groupValue: {2}", key, func, groupValue);
		switch(func) {
		case ISEMPTY:
		case ISNOTEMPTY:
		case ISNOTNULL:
		case ISNULL:
		case REGEX:
		case TODATE:
		default:
			logger.warn("Unsupported function '{0}' found.", func.getKey());
			break;
		}
		return json;
	}

	/**
	 * Method to extract group value from string
	 * @param qry
	 * @param grpOp
	 * @return
	 */
	private String getGroupStringValue(String qry, Keywords grpOp) {
		if (!IUtils.isNullOrEmpty(qry)) {
			try {
				NestedString nstStr = NestedString.parse(
						qry, grpOp.getGroupStart(), grpOp.getGroupEnd(), true);
				logger.warn(nstStr.getError());
				return NestedString.getUpperGroupString(nstStr);
			} catch (SynectiksException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Method to check if query has a function just after key
	 * @param qry 
	 * @return
	 */
	private boolean haveFunction(String qry) {
		return !IUtils.isNull(getFunction(qry));
	}

	/**
	 * Method to find if query has an operator after key,
	 * if matches then returns the operator or returns null.
	 * @param qry 
	 * @return
	 */
	private Keywords getFunction(String qry) {
		// remove first string assume its query.
		String key = IUtilities.getFirstString(qry);
		if (!IUtils.isNullOrEmpty(key)) {
			qry = qry.substring(key.length() + 1);
			if (!IUtils.isNullOrEmpty(key)) {
				List<Keywords> list = Keywords.list(KWTypes.FUNCTION);
				for (Keywords kw : list) {
					if (qry.startsWith(kw.getKey())) {
						return kw;
					}
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
		List<Keywords> list = Keywords.list(KWTypes.KEYWORD);
		for (Keywords kw : list) {
			if (qry.startsWith(kw.getKey() + IConsts.SPACE)) {
				return true;
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
	 * Method to find if query starts with any conjunction operator,
	 * if matches then returns the operator or returns null.
	 * @param qry
	 * @return
	 */
	private Keywords getConjuncOperator(String qry) {
		List<Keywords> list = Keywords.list(KWTypes.CONJUNCTION);
		for (Keywords kw : list) {
			if (query.startsWith(kw.getKey() + IConsts.SPACE)) {
				return kw;
			}
		}
		return null;
	}

}
