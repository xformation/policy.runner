/**
 * 
 */
package com.synectiks.policy.runner.utils;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants.Keywords;

/**
 * POJO Model for rule check's evaluation
 * @author Rajesh Upadhyay
 */
public class EvalCheck {

	private String field;
	private Keywords operator;
	private String value;
	private boolean grpVal;
	private boolean nested;
	private boolean regexKey;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		if (!IUtils.isNullOrEmpty(field)) {
			if (field.contains(".")) {
				this.nested = true;
			}
			if (field.contains("*") || field.contains("?")) {
				this.regexKey = true;
			}
		}
		this.field = field;
	}

	public Keywords getOperator() {
		return operator;
	}

	public void setOperator(Keywords operator) {
		this.operator = operator;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		if (!IUtils.isNullOrEmpty(value) && IUtilities.isStartWithGroup(value)) {
			this.grpVal = true;
		}
		this.value = value;
	}

	public boolean isGrpVal() {
		return grpVal;
	}

	public boolean isNested() {
		return nested;
	}

	public boolean isRegexKey() {
		return regexKey;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		if (field != null)
			builder.append("\"field\": ").append(field);
		if (operator != null) {
			builder.append(builder.length() > 3 ? ", " : "");
			builder.append("\"operator\": ").append(operator);
		}
		if (value != null) {
			builder.append(builder.length() > 3 ? ", " : "");
			builder.append("\"value\": ").append(value);
		}
		builder.append(" }");
		return builder.toString();
	}

	/**
	 * Method to create a EvalCheck object
	 * @param key
	 * @param op
	 * @param val
	 * @return
	 */
	public static EvalCheck create(String key, Keywords op, String val) {
		EvalCheck chk = new EvalCheck();
		chk.setField(key);
		chk.setOperator(op);
		chk.setValue(val);
		return chk;
	}
}
