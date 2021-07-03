/**
 * 
 */
package com.synectiks.policy.runner.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.OperatorsDesc.Operator;

/**
 * @author Rajesh
 */
public interface IConstants {

	Logger logger = LoggerFactory.getLogger(IConstants.class);
	
	/**
	 * A regex which can validate folllowing date formats.
	 * 1/10/2001, 23/1/2200, 1.10.2001,
	 * 23-1-2002, May 23, 2004, 2003/03/05,
	 * 2003-03-05, 2003.03.05
	 */
	String REGEX_TO_MATCH_DATE = "^(\\w{3}|\\d{1,4})[\\.\\-/\\s]\\d{1,2}[,\\-/\\.]\\s?\\d{1,4}";

	/**
	 * Data types for elastic mapping supported.
	 */
	enum DataTypes {
		INT,
		LONG,
		DOUBLE,
		BOOLEAN,
		TEXT,
		DATE,
		ARRAY,
		OBJECT;

		/**
		 * Method to match string with regexes to identify its data type
		 * @param input
		 * @return
		 */
		public static DataTypes findType(String input) {
			DataTypes type = null;
			// regex to match if input is boolean, "true|false"
			// regex to match if input is long, "-?\\d+$"
			// regex to match if input is double "-?\\d+(\\.\\d+)?"
			if (!IUtils.isNullOrEmpty(input)) {
				if (input.matches("true|false")) {
					type = BOOLEAN;
				} else if (input.matches(REGEX_TO_MATCH_DATE)) {
					type = DATE;
				} else if (input.matches("-?\\d+$")) {
					type = LONG;
				} else if (input.matches("-?\\d+\\.\\d+$")) {
					type = DOUBLE;
				} else {
					type = TEXT;
				}
			}
			return type;
		}
	};

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
		AND("AND", KWTypes.CONJUNCTION,
				"A logical conjunction on two expressions, both to be matched. "
				+ "i.e. key = value AND key = value"),
		OR("OR", KWTypes.CONJUNCTION,
				"A logical conjunction on two expressions, any to be matched. "
				+ "i.e. key = value OR key = value"),
		// Functions
		ISEMPTY("isEmpty", KWTypes.FUNCTION,
				"Check or select rows if the key&#39;s value is empty i.e. <i>key isEmpty</i>",
				DataTypes.TEXT, DataTypes.DATE, DataTypes.ARRAY),
		ISNOTEMPTY("isNotEmpty", KWTypes.FUNCTION,
				"Check or select rows if the key&#39;s value is NOT empty i.e. <i>key isNotEmpty</i>",
				DataTypes.TEXT, DataTypes.DATE, DataTypes.ARRAY),
		ISNOTNULL("isNotNull", KWTypes.FUNCTION,
				"Check or select rows if the key&#39;s value is NOT null i.e. <i>key isNotNull</i>",
				DataTypes.TEXT, DataTypes.DATE, DataTypes.ARRAY),
		ISNULL("isNull", KWTypes.FUNCTION,
				"Check or select rows if the key&#39;s value is null i.e. <i>key isNull</i>",
				DataTypes.TEXT, DataTypes.DATE, DataTypes.ARRAY),
		REGEX("regex", KWTypes.FUNCTION,
				"Check or select rows if key&#39;s value match with regex"
				+ " i.e. <i>.key regex(&#39;^R.*esh$&#39;)</i>",
				DataTypes.TEXT),
		TODATE("toDate", KWTypes.FUNCTION,
				"This function help to convert string value to date for matching key&#39;s value."
				+ " i.e. <i>key >= toDate(&#39;2018-08-15 13:20:30&#39;, &#39;yyyy-MM-dd HH:mm:ss&#39;)</i>",
				DataTypes.DATE),
		// Group
		CptlBrkt("[]", KWTypes.GROUP,
				"We use capital brackets for enclosing multiple keys."
				+ " In following query, we can use wildcard chars to point multiple keys and"
				+ " the value is being searched into all listed key fields."
				+ " i.e. <i>[key1, key2, *Id] value</i>"),
		SmlBrkt("()", KWTypes.GROUP,
				"We use the small breacket to combine multiple values or make a separation."
				+ " Following query represent IN query, where we can define multiple values to be searched."
				+ " i.e. <i>key = (value1, value2, value3)</i>"),
		DblQuote("\"", KWTypes.GROUP,
				"The \" we use to combine a text with spaces. i.e. <i>key = \"sample value\"</i>"),
		SnglQuote("'", KWTypes.GROUP,
				"Another form of combining text with spaces. i.e. <i>key = &#39;sample value&#39;</i>"),
		// Keyword
		HAS("has", KWTypes.KEYWORD,
				"This keyword is used to identify if a row has value for key."
				+ " A sort of exists query. i.e. <i>has key</i>"),
		MUST("+", KWTypes.KEYWORD,
				"We can use + before any value to make it must match. i.e.<i>key = +\"Rajesh Kumar\"</i>"),
		// Operators
		Equals("=", KWTypes.OPERATOR,
				"Equals operator to make a value in key. i.e. <i>key = value</i>",
				DataTypes.values()),
		GreaterThan(">", KWTypes.OPERATOR,
				"Greater than operator to find rows which key has more value than specified value."
				+ " i.e. <i>key > value</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE),
		GreaterThanEquals(">=", KWTypes.OPERATOR,
				"Greater than equal to operator to find rows which key has more or equal "
				+ "value than specified value. i.e. <i>key >= value</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE),
		LessThan("<", KWTypes.OPERATOR,
				"Less than operator to find rows which key has less value than specified value."
				+ " i.e. <i>key < value</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE),
		LessThanEquals("<=", KWTypes.OPERATOR,
				"Less than equal to operator to find rows which key has less or equal "
				+ "value than specified value. i.e. <i>key <= value</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE),
		NotEquals("!=", KWTypes.OPERATOR,
				"Not equal to operator to find rows which key has not equal "
				+ "value than specified value. i.e. <i>key != value</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE),
		IN("In", KWTypes.OPERATOR,
				"IN operator to match multiple values in a key(s). "
				+ "i.e. <i>key = (value1, value2, value3)</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE, DataTypes.TEXT),
		NotIN("NotIn", KWTypes.OPERATOR,
				"NOT IN operator not to match multiple values in a key(s). "
				+ "i.e. <i>key != (value1, value2, value3)</i>",
				DataTypes.INT, DataTypes.LONG, DataTypes.DOUBLE, DataTypes.DATE, DataTypes.TEXT),
		Like("like", KWTypes.OPERATOR,
				"This is sort of wildcard query where we can use ? and * to match values.",
				DataTypes.TEXT),
		NotLike("NotLike", KWTypes.OPERATOR,
				"This is wildcard query where we can use ? and * to NOT match values.",
				DataTypes.TEXT),
		// Wildcards
		QST("?", KWTypes.WILDCARD),
		STAR("*", KWTypes.WILDCARD);

		private String key;
		private KWTypes type;
		private DataTypes[] supportedTypes;
		private String hint;

		Keywords(String key, KWTypes type) {
			this.key = key;
			this.type = type;
			//this.supportedTypes = DataTypes.values();
		}

		Keywords(String key, KWTypes type, String hint, DataTypes... supports) {
			this.key = key;
			this.type = type;
			this.hint = hint;
			this.supportedTypes = supports;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append("\"key\": \"" + key + "\"");
			sb.append(", \"type\": \"" + type + "\"");
			if (!IUtils.isNullOrEmpty(hint)) {
				sb.append(", \"hint\": \"" + hint + "\"");
			}
			if (!IUtils.isNull(supportedTypes) && supportedTypes.length > 0) {
				sb.append(", \"supportedTypes\": " + IUtilities.arrToString(supportedTypes));
			}
			sb.append("}");
			return sb.toString();
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

		public boolean hasSupportedTypes() {
			if (!IUtils.isNull(this.supportedTypes) && this.supportedTypes.length > 0) {
				return true;
			}
			return false;
		}

		public DataTypes[] getSupportedTypes() {
			return supportedTypes;
		}

		public String getHint() {
			return hint;
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

		public boolean isRangeOperator() {
			if (type == KWTypes.OPERATOR) {
				if (!key.equals("=") && !key.equals("!=")) {
					return true;
				}
			}
			return false;
		}

		public boolean isNegativeOprtr() {
			if (this.isOperator()) {
				if (Keywords.NotEquals == this || Keywords.NotIN == this
						|| Keywords.NotLike == this) {
					return true;
				}
			}
			return false;
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
		public static List<Keywords> listKeywords(KWTypes type) {
			List<Keywords> lst = new ArrayList<>();
			for (Keywords kw : Keywords.values()) {
				if (kw.type == type) {
					lst.add(kw);
				}
			}
			return lst;
		}

		/**
		 * Method to list all the keywords of specified type.
		 * @param type
		 * @return
		 */
		public static List<Operator> list(KWTypes type) {
			List<Operator> lst = new ArrayList<>();
			for (Keywords kw : Keywords.values()) {
				if (kw.type == type) {
					lst.add(Operator.create(kw));
				}
			}
			return lst;
		}

		/**
		 * Method to list all Keywords and functions group by types.
		 * @return
		 */
		public static OperatorsDesc listFieldsMap () {
			OperatorsDesc opDesc = new OperatorsDesc();
			for (KWTypes type : KWTypes.values()) {
				if (KWTypes.WILDCARD != type) {
					opDesc.putInMap(type.name(), list(type));
				}
			}
			logger.info("Map: " + opDesc.getMap());
			return opDesc;
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
	String PSD_STR_LEN = "processedString";
	String MATCH = "match";
	String MUST = "must";
	String MUST_NOT = "must_not";
	String MULTI_MATCH = "multi_match";
	String NOT_QRY = "isNotQry";
	String NST_PTH_QRY = "nstPthQry";
	String OPERATOR = "operator";
	String QUERY = "query";
	String RANGE = "range";
	String REGEXP = "regexp";
	String SHOULD = "should";
	String SHOULD_NOT = "should_not";
	String SPACE = " ";
	String TYPE = "type";
	String WILDCARD = "wildcard";

	String API_PARSE_QUERY = "/queryParser";
	String API_EXECUTE = "/execute";
	String API_SUGGEST = "/suggestKey";
	String API_OPRTORS_BY_TYPE = "/operatorsByType";
	String GET_INDX_MAPPING_URI = "/getIndexMapping";
	String SET_INDX_MAPPING_URI = "/setIndexMapping";
	String ELASTIC_QUERY = "/elsQuery";
	String ELASTIC_LIST = "/list";
	String ELASTIC_DOCS = "/getDocs";
	String ELASTIC_INDX_DOCS = "/searchWithIndexAndType";
	String PRM_INDEX = "index";
	String PRM_TYPE = "type";

	String GULF_HOST = "synectiks.gelf.tcp.host";
	String GULF_PORT = "synectiks.gelf.tcp.port";
	String GELF_USER = "synectiks.gelf.user";
	String GELF_PASS = "synectiks.gelf.pass";
	String GET_GELF_INDEXES = "http://%1$s:%2$s/api/system/indexer/indices";
}