/**
 * 
 */
package com.synectiks.policy.runner.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		MUST("+", KWTypes.KEYWORD),
		// Operators
		EQ("=", KWTypes.OPERATOR),
		GT(">", KWTypes.OPERATOR),
		GTE(">=", KWTypes.OPERATOR),
		LT("<", KWTypes.OPERATOR),
		LTE("<=", KWTypes.OPERATOR),
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
	String AND = "and";
	String BEST_FRIENDS = "best_fields";
	String BOOL = "bool";
	String FIELD = "field";
	String FIELDS = "fields";
	String FORMAT = "format";
	String EXISTS = "exists";
	String LENGTH = "psdLen";
	String MATCH = "match";
	String MUST = "must";
	String MUST_NOT = "must_not";
	String MULTI_MATCH = "multi_match";
	String NESTED = "nested";
	String NOT_QRY = "isNotQry";
	String NST_PTH_QRY = "nstPthQry";
	String OPERATOR = "operator";
	String PATH = "path";
	String QUERY = "query";
	String RANGE = "range";
	String REGEXP = "regexp";
	String SHOULD = "should";
	String SHOULD_NOT = "should_not";
	String TYPE = "type";
	String WILDCARD = "wildcard";

	String API_PARSE_QUERY = "/queryParser";
	String SET_INDX_MAPPING_URI = "/setIndexMapping";
}
