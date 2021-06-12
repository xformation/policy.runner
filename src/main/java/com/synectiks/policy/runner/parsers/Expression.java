/**
 * 
 */
package com.synectiks.policy.runner.parsers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.entities.EvalPolicyRuleResult;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IConstants.Keywords;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * Class to keep a query expression with key, value, operator, function and keywords.
 * @author Rajesh Upadhyay
 */
public class Expression implements Serializable {

	private static final long serialVersionUID = 7052269551994332209L;

	private static final Logger logger = LoggerFactory.getLogger(Expression.class);

	private Long policyId;
	private Long ruleId;

	/** Expression is full text search type */
	private boolean isFullText;
	/** Expression is just to check if a key exists in target object */
	private boolean isExists;

	private List<Expression> andExpressions;
	private List<Expression> orExpressions;
	
	private Key key;
	private Keywords operator;
	private Keywords function;
	private Value value;

	private String processedStr;

	private Expression() {
		
	}

	private Expression(Key key, Keywords oprtr, Keywords func, Value val,
			List<Expression> andExp, List<Expression> orExp, boolean has, boolean fts,
			String prsds) {
		this.key = key;
		this.function = func;
		this.operator = oprtr;
		this.value = val;
		this.andExpressions = andExp;
		this.orExpressions = orExp;
		this.isExists = has;
		this.isFullText = fts;
		this.processedStr = prsds;
	}

	public Long getPolicyId() {
		return policyId;
	}

	public void setPolicyId(Long policyId) {
		this.policyId = policyId;
	}

	public Long getRuleId() {
		return ruleId;
	}

	public void setRuleId(Long ruleId) {
		this.ruleId = ruleId;
	}

	public boolean isFullText() {
		return isFullText;
	}

	public boolean isExists() {
		return isExists;
	}

	public List<Expression> getAndExpressions() {
		return andExpressions;
	}

	public List<Expression> getOrExpressions() {
		return orExpressions;
	}

	public Key getKey() {
		return key;
	}

	public Keywords getOperator() {
		return operator;
	}

	public Keywords getFunction() {
		return function;
	}

	public Value getValue() {
		return value;
	}

	public String getProcessedStr() {
		return processedStr;
	}

	/**
	 * Method to parse input string as Expression
	 * @param in
	 * @return
	 */
	public static Expression parse(String in, Long pId, Long rId) {
		Expression expr = null;
		if (!IUtils.isNullOrEmpty(in)) {
			while (!IUtils.isNull(in) && in.trim().length() > 0) {
				logger.debug("Processing input: " + in);
				expr = parse(in.trim(), expr);
				if (!IUtils.isNull(expr)) {
					in = IUtilities.removeProcessedString(in, expr.getProcessedStr());
				}
			}
			expr.setPolicyId(pId);
			expr.setRuleId(rId);
		}
		logger.debug("Final Expression: " + expr);
		return expr;
	}

	/**
	 * Method to be used recursivly for processing whole string.
	 * @param in
	 * @param expr
	 * @return
	 */
	private static Expression parse(String in, Expression expr) {
		if (!IUtils.isNullOrEmpty(in)) {
			StringBuilder sb = new StringBuilder();
			Key key = null;
			Value val = null;
			Keywords func = null;
			Keywords oprtr = null;
			boolean has = false, fts = false;
			List<Expression> andExp = null, orExp = null;
			// Check if we have not null input expression.
			logger.debug("Input: " + in);
			if (IUtilities.isStartWithConjuction(in)) {
				Keywords conj = IUtilities.getConjuncOperator(in);
				sb.append(conj.getKey() + IConstants.SPACE);
				in = IUtilities.removeProcessedString(in, conj.getKey());
				Expression exp = parse(in, null);
				if (!IUtils.isNull(exp)) {
					if (Keywords.AND == conj) {
						andExp = new ArrayList<>();
						if (!IUtils.isNull(expr)) {
							andExp.add(expr);
						}
						andExp.add(exp);
					} else {
						orExp = new ArrayList<>();
						if (!IUtils.isNull(expr)) {
							orExp.add(expr);
						}
						orExp.add(exp);
					}
					sb.append(exp.getProcessedStr());
					in = IUtilities.removeProcessedString(in, exp.getProcessedStr());
				}
			} else if (IUtilities.isStartWithHasKeyword(in)) {
				has = true;
				sb.append(Keywords.HAS.getKey() + IConstants.SPACE);
				in = IUtilities.removeProcessedString(in, Keywords.HAS.getKey());
				// Extract key from input.
				key = Key.parse(in);
				if (!IUtils.isNull(key)) {
					sb.append(key.getProcessedStr());
					in = IUtilities.removeProcessedString(in, key.getProcessedStr());
				}
			} else if (IUtilities.isStartWithGroup(in)) {
				Keywords grp = IUtilities.getStartWithGroup(in);
				String grpStr = IUtilities.getGroupValue(in, grp, true);
				sb.append(grpStr);
				in = IUtilities.removeProcessedString(in, grpStr);
				switch (grp) {
				case CptlBrkt: // Case of multi keys
					key = Key.parse(grpStr);
					sb.append(IConstants.SPACE);
					break;
				case SmlBrkt: // Case of conjunction operator query
					grpStr = IUtilities.getGroupValue(grpStr, grp, false);
					while (!IUtils.isNull(grpStr) && grpStr.trim().length() > 0) {
						expr = parse(grpStr, expr);
						grpStr = IUtilities.removeProcessedString(grpStr, expr.getProcessedStr());
					}
					// Finally add and or expression to create final expressions.
					andExp = expr.getAndExpressions();
					orExp = expr.getOrExpressions();
					break;
				case DblQuote:
				case SnglQuote:
					// Its only value so apply full text search
					// we will handle it below
					break;
				default:
					logger.warn("Unknown group: " + grp);
				}
			} else if (IUtilities.haveFunction(in) ||
					IUtilities.haveOperator(in)) { // Parse Key
				key = Key.parse(in);
				if (!IUtils.isNull(key)) {
					sb.append(key.getProcessedStr() + IConstants.SPACE);
					in = IUtilities.removeProcessedString(in, key.getProcessedStr());
				}
			}
			// Find and process operators
			if (IUtilities.isStartWithOperator(in)) {
				oprtr = IUtilities.getOperator(in, false);
				//sb.append(sb.length() > 0 ? IConstants.SPACE : "");
				sb.append(oprtr.getKey() + IConstants.SPACE);
				in = IUtilities.removeProcessedString(in, oprtr.getKey());
			}
			// Find and process functions
			if (IUtilities.isStartWithFunction(in)) {
				func = IUtilities.getFunction(in, false);
				sb.append(func.getKey());
				in = IUtilities.removeProcessedString(in, func.getKey());
			}
			if (!IUtils.isNullOrEmpty(in) && IUtils.isNull(andExp) && IUtils.isNull(orExp) && IUtils.isNull(key)) { // Its only value so apply full text search
				fts = true;
			}
			// finally process value
			if (!IUtils.isNullOrEmpty(in) && !IUtilities.isStartWithConjuction(in)) {
				val = Value.parse(in, func);
				sb.append(val.getProcessedStr());
			}
			// Finally create an Expression object
			expr = new Expression(key, oprtr, func, val, andExp, orExp, has, fts, sb.toString());
		}
		logger.debug("Parsed Expression: " + expr);
		return expr;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		if (policyId != null)
			builder.append("\"policyId\": ").append(policyId).append(", ");
		if (ruleId != null)
			builder.append("\"ruleId\": ").append(ruleId).append(", ");
		builder.append("\"isFullText\": ").append(isFullText).append(", \"isExists\": ")
				.append(isExists).append(", ");
		if (andExpressions != null)
			builder.append("\"andExpressions\": ").append(andExpressions).append(", ");
		if (orExpressions != null)
			builder.append("\"orExpressions\": ").append(orExpressions).append(", ");
		if (key != null)
			builder.append("\"key\": ").append(key).append(", ");
		if (operator != null)
			builder.append("\"operator\": ").append(operator).append(", ");
		if (function != null)
			builder.append("\"function\": ").append(function).append(", ");
		if (value != null)
			builder.append("\"value\": ").append(value).append(", ");
		/*if (processedStr != null)
			builder.append("\"processedStr\": \"").append(processedStr);*/
		builder.append("\" }");
		return builder.toString();
	}

	/**
	 * Method to evaluate expression for provided json entity.
	 * @param entity
	 * @return
	 */
	public EvalPolicyRuleResult evaluate(JSONObject entity) {
		return evaluate(entity, null, null);
	}

	/**
	 * Method to evaluate expression for provided json entity.
	 * @param entity
	 * @return
	 */
	public EvalPolicyRuleResult evaluate(JSONObject entity, String indx, String clz) {
		EvalPolicyRuleResult res = new EvalPolicyRuleResult();
		res.setPolicyId(String.valueOf(this.policyId));
		res.setRuleId(String.valueOf(this.ruleId));
		res.setIndex(indx);
		res.setClz(clz);
		StringBuilder msgs = new StringBuilder();
		boolean suc = this.evaluate(entity, msgs);
		if (entity.has("id")) {
			res.setDocId(entity.optString("id"));
		}
		res.setMsgs(msgs.toString());
		res.setPass(suc);
		return res;
	}

	/**
	 * Method to evaluate the expression for entity and collect msgs.
	 * @param entity
	 * @param msgs
	 * @return
	 */
	private boolean evaluate(JSONObject entity, StringBuilder msgs) {
		boolean suc = false;
		if (IUtils.isNull(entity)) {
			msgs.append("Input entity is null.");
			return suc;
		}
		boolean isAnd = false;
		if (!IUtils.isNull(this.andExpressions) && this.andExpressions.size() > 0) {
			suc = evalAndExpr(entity, msgs);
			isAnd = true;
		}
		if (!IUtils.isNull(this.orExpressions) && this.orExpressions.size() > 0) {
			boolean isSuc = evalOrExpr(entity, msgs);
			suc = suc || isSuc;
		}
		// Now evaluate this expression for function operator and other values types.
		boolean isPass = (isAnd ? suc : false);
		if (isFullText) {
			if (IUtilities.evalFullText(this.value, entity)) {
				isPass = true;
				msgs.append(msgs.length() > 0 ? ", " : "");
				msgs.append("'" + value.getVal() + "' found in entity.");
			} else {
				msgs.append(msgs.length() > 0 ? ", " : "");
				msgs.append("'" + value.getVal() + "' doesn't exists in entity.");
			}
		} else if (isExists) {
			if (entity.has(key.getKey())) {
				isPass = true;
				msgs.append(msgs.length() > 0 ? ", " : "");
				msgs.append("'" + key.getKey() + "' exists in entity.");
			} else {
				msgs.append(msgs.length() > 0 ? ", " : "");
				msgs.append("'" + key.getKey() + "' doesn't exists in entity.");
			}
		} else {
			if (!IUtils.isNull(this.function) && Keywords.TODATE != this.function) {
				isPass = processFunction(entity, msgs);
			} else if (!IUtils.isNull(this.operator)) {
				isPass = processOperators(entity, msgs);
			} else { // These are contains queries.
				List<String> lks = new ArrayList<>();
				if (!IUtils.isNull(key)) {
					if (this.key.isMulti()) {
						key.getKeys().forEach((itm) -> {
							lks.add(itm.getKey());
						});
					} else {
						lks.add(key.getKey());
					}
					isPass = evalContainsQry(entity, lks, msgs);
				} else {
					// ignore the expression. if value is also null, may be its AND/OR query
					if (!IUtils.isNull(this.value)) {
						isPass = evalContainsQry(entity, lks, msgs);
					}
				}
			}
		}
		// Final evaluation to check expression is success.
		suc = isAnd ? (suc && isPass) : (suc || isPass);
		return suc;
	}

	/**
	 * Method to evaluate if the entity contains value.
	 * @param entity
	 * @param lks list of keys to check, empty to check all keys.
	 * @param msgs 
	 * @return
	 */
	private boolean evalContainsQry(JSONObject entity, List<String> lks, StringBuilder msgs) {
		if (lks.isEmpty()) {
			lks = IUtils.getJsonKeys(entity);
		}
		boolean res = false;
		if (!IUtils.isNull(lks)) {
			String keys = "";
			for (String k : lks) {
				boolean er = evalContains(entity, k);
				if (er) {
					keys += (keys.length() > 0 ? ", " : "") + k;
				}
				res = res || er;
			}
			if (!IUtils.isNullOrEmpty(keys)) {
				msgs.append(msgs.length() > 0 ? "\n" : "");
				msgs.append("Match found in keys [" + keys + "].");
			}
		}
		if (!res) {
			msgs.append(msgs.length() > 0 ? " \n" :
				"No match found in keys " + lks + ".");
		}
		return res;
	}

	/**
	 * Method to evaluate if key in entity contains value
	 * @param entity
	 * @param k
	 * @return
	 */
	private boolean evalContains(JSONObject entity, String k) {
		if (!IUtils.isNullOrEmpty(k)) {
			Object val = IUtilities.getValueOfKey(entity, k, k.contains("."));
			if (!IUtils.isNull(val) && !IUtils.isNull(this.value)) {
				return ((String) val).toLowerCase().contains(
						this.value.getVal().toLowerCase());
			}
		}
		return false;
	}

	/**
	 * Method to process opertor's queries.
	 * @param entity
	 * @param msgs
	 * @return
	 */
	private boolean processOperators(JSONObject entity, StringBuilder msgs) {
		boolean isPass = false;
		switch (this.operator) {
		case Equals:
		case NotEquals:
		case GreaterThan:
		case LessThan:
		case GreaterThanEquals:
		case LessThanEquals:
		case IN:
		case Like:
		case NotIN:
		case NotLike:
			isPass = chkAndEvalOperator(entity, msgs, this.operator.isNegativeOprtr());
			break;
		default:
			logger.warn("Unsupported operator: '" + this.operator.name() + "'");
			break;
		}
		return isPass;
	}

	/**
	 * Method to check if the value is equals or not equals.
	 * @param entity
	 * @param msgs
	 * @param isNeg
	 * @return
	 */
	private boolean chkAndEvalOperator(JSONObject entity, StringBuilder msgs, boolean isNeg) {
		boolean isPass = false;
		if (this.key.isMulti()) {
			for (Key k : key.getKeys()) {
				boolean res = compareKeyVal(entity, k, this.operator);
				msgs = IUtilities.setEvalMsg(msgs, k.getKey(), k.isLenCheck(),
						isPass, isNeg, this.operator.name());
				isPass = isPass || res;
			}
		} else {
			isPass = compareKeyVal(entity, key, this.operator);
			msgs = IUtilities.setEvalMsg(msgs, key.getKey(), key.isLenCheck(),
					isPass, isNeg, this.operator.name());
		}
		return isPass;
	}

	/**
	 * Method to compare the input value with key value of entity.
	 * @param entity
	 * @param k
	 * @param op
	 * @return
	 */
	private boolean compareKeyVal(JSONObject entity, Key k, Keywords op) {
		boolean res = false;
		if (k.isWildcard()) {
			List<String> ks = IUtils.listMatchingKeys(entity, k.getKey());
			for (String it : ks) {
				Object v = IUtilities.getValueOfKey(entity, it, k.isNested());
				if (k.isLenCheck()) {
					v = (!IUtils.isNull(v)) ? String.valueOf(v).length() : 0;
				}
				res = res || compareValues(v, op.getKey());
			}
		} else {
			Object v = IUtilities.getValueOfKey(entity, k.getKey(), k.isNested());
			if (k.isLenCheck()) {
				v = (!IUtils.isNull(v)) ? String.valueOf(v).length() : 0;
			}
			res = compareValues(v, op.getKey());
		}
		return res;
	}

	/**
	 * Method to compare key and input values as par operators.
	 * @param val object value
	 * @param op
	 * @return
	 */
	private boolean compareValues(Object val, String op) {
		boolean res = false;
		if (this.value.isMulti()) {
			for (Value v : this.value.getVals()) {
				res = res || IUtilities.evalOperator(v.getVal(), op, val);
			}
		} else if (this.value.isWildcard()) {
			res = IUtilities.evalRegex(this.value.getVal(), String.valueOf(val));
			if (Keywords.Equals.getKey() != op) {// Its a not like query
				res = !res;
			}
		} else if (!IUtils.isNull(this.value.getFunction())) {
			long dtTime = IUtilities.getLongTime((String) val, null);
			res = IUtilities.evalOperator(this.value.getDateValue(), op, dtTime);
		} else {
			res = IUtilities.evalOperator(this.value.getVal(), op, val);
		}
		return res;
	}

	/**
	 * Method to process functions
	 * @param entity
	 * @param msgs
	 * @return
	 */
	private boolean processFunction(JSONObject entity, StringBuilder msgs) {
		boolean isPass = false;
		switch(this.function) {
		case ISNULL:
		case ISEMPTY:
			isPass = evalIsNullOrEmpty(entity);
			msgs.append(msgs.length() > 0 ? " \n" :
				"'" + key.getKey() + "' " + (isPass ? "" : "NOT ") +
				this.function.getKey() + ".");
			break;
		case ISNOTNULL:
		case ISNOTEMPTY:
			isPass = ! evalIsNullOrEmpty(entity);
			msgs.append(msgs.length() > 0 ? " \n" :
				"'" + key.getKey() + "' " + (isPass ? this.function.getKey() :
					this.function.getKey().replace("Not", "")) + ".");
			break;
		case REGEX:
			isPass = evalRegex(entity);
			msgs.append(msgs.length() > 0 ? " \n" :
				"'" + key.getKey() + "' " + (isPass ? "" : "NOT") +
				" match for regex '" + this.value.getVal() + "'.");
			break;
		case TODATE:
			// Ignore it, we will take care of it in operators
			break;
		default:
			msgs.append(msgs.length() > 0 ? " \n" :
				"Unsupported function '" + this.function.getKey() + "'.");
			break;
		}
		return isPass;
	}

	/**
	 * Method to check if key's value match with given reges.
	 * @param entity
	 * @return
	 */
	private boolean evalRegex(JSONObject entity) {
		boolean res = false;
		if (!IUtils.isNull(entity) && entity.has(key.getKey())) {
			String val = (String) IUtilities.getValueOfKey(
					entity, key.getKey(), key.isNested());
			res = IUtilities.evalRegex(value.getVal(), val);
		}
		return res;
	}

	/**
	 * Method to check if entity has null or empty value for key
	 * @param entity
	 * @return 
	 */
	private boolean evalIsNullOrEmpty(JSONObject entity) {
		boolean isEmpty = true;
		if (!IUtils.isNull(key) && !IUtils.isNullOrEmpty(key.getKey())) {
			Object vl = IUtilities.getValueOfKey(entity, key.getKey(), key.isNested());
			if (!IUtils.isNull(vl) && !IUtils.isNullOrEmpty(String.valueOf(vl))) {
				return false;
			}
		}
		return isEmpty;
	}

	/**
	 * Method to evaluate all or expressions
	 * @param entity
	 * @param msgs
	 * @return
	 */
	private boolean evalOrExpr(JSONObject entity, StringBuilder msgs) {
		boolean isSuc = false;
		for (Expression expr : this.orExpressions) {
			if (expr.evaluate(entity, msgs)) {
				isSuc = true;
			}
		}
		return isSuc;
	}

	/**
	 * Method to evaluate all and expressions.
	 * @param entity
	 * @param msgs
	 * @return
	 */
	private boolean evalAndExpr(JSONObject entity, StringBuilder msgs) {
		boolean isSuc = true;
		for (Expression expr : this.andExpressions) {
			if (!expr.evaluate(entity, msgs)) {
				isSuc = false;
			}
		}
		return isSuc;
	}

}
