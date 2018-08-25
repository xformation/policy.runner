/**
 * 
 */
package com.synectiks.policy.runner.utils;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.utils.IUtils;

/**
 * @author Rajesh
 */
public interface IConstants {

	Logger logger = LoggerFactory.getLogger(IConstants.class);

	/**
	 * Query keywords type definitions
	 * @author Rajesh
	 */
	enum KWTypes {
		CONJUNCTION,
		FUNCTION,
		GROUP,
		KEYWORD,
		OPERATOR,
		WILDCARD
	};

	/**
	 * Query keyword constants
	 * @author Rajesh
	 */
	enum Keywords {
		// Conjuctions
		AND("AND", KWTypes.CONJUNCTION),
		OR("OR", KWTypes.CONJUNCTION),
		// Functions
		ISEMPTY("isEmpty", KWTypes.FUNCTION),
		ISNOTEMPTY("isNotEmpty", KWTypes.FUNCTION),
		ISNOTNULL("isNotNull", KWTypes.FUNCTION),
		ISNULL("isNull", KWTypes.FUNCTION),
		REGEX("regex", KWTypes.FUNCTION),
		TODATE("toDate", KWTypes.FUNCTION),
		// Group
		CptlBrkt("[]", KWTypes.GROUP),
		DblQuote("\"", KWTypes.GROUP),
		SmlBrkt("()", KWTypes.GROUP),
		SnglQuote("'", KWTypes.GROUP),
		// Keyword
		HAS("has", KWTypes.KEYWORD),
		// Operators
		EQ("=", KWTypes.OPERATOR),
		GE(">", KWTypes.OPERATOR),
		GTE(">=", KWTypes.OPERATOR),
		LT("<", KWTypes.OPERATOR),
		LTE("<=", KWTypes.OPERATOR),
		MUST("+", KWTypes.OPERATOR),
		NE("!=", KWTypes.OPERATOR),
		// Wildcards
		QST("?", KWTypes.WILDCARD),
		STAR("*", KWTypes.WILDCARD);

		private String key;
		private KWTypes type;

		Keywords(String key, KWTypes type) {
			this.key = key;
			this.type = type;
		}

		public String getName() {
			return key;
		}

		public KWTypes getType() {
			return type;
		}

		public String getKey() {
			return key;
		}

		public boolean isConjuctionOp() {
			return type == KWTypes.CONJUNCTION;
		}

		public boolean isFunction() {
			return type == KWTypes.FUNCTION;
		}

		public boolean isGroupOperator() {
			return type == KWTypes.GROUP;
		}

		public boolean isKeyword() {
			return type == KWTypes.KEYWORD;
		}

		public boolean isOperator() {
			return type == KWTypes.OPERATOR;
		}

		public String getGroupStart() {
			char c = 0;
			if (isGroupOperator()) {
				c = key.charAt(0);
			}
			return String.valueOf(c);
		}

		public String getGroupEnd() {
			char c = 0;
			if (isGroupOperator()) {
				if (key.length() > 1) {
					c = key.charAt(1);
				} else {
					c = key.charAt(0);
				}
			}
			return String.valueOf(c);
		}

		/**
		 * Method to list all the keywords of specified type.
		 * @param type
		 * @return
		 */
		public static List<Keywords> list(KWTypes type) {
			List<Keywords> lst = new ArrayList<>();
			for (Keywords kw : Keywords.values()) {
				if (kw.type == type) {
					lst.add(kw);
				}
			}
			return lst;
		}
	};

	String _All = "_all";
	String FIELD = "field";
	String FIELDS = "fields";
	String EXISTS = "exists";
	String MATCH = "match";
	String MULTI_MATCH = "multi_match";
	String OPERATOR = "operator";
	String QUERY = "query";

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
			logger.error(e.getMessage(), e);
		}
		return json;
	}
}
