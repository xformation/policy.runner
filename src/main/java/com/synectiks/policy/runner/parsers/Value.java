/**
 * 
 */
package com.synectiks.policy.runner.parsers;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants.DataTypes;
import com.synectiks.policy.runner.utils.IConstants.Keywords;
import com.synectiks.policy.runner.utils.IConstants.TimeUnits;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * Class to represent and store the query value.
 * @author Rajesh Upadhyay
 */
public class Value implements Serializable {

	private static final long serialVersionUID = -2204499201148708431L;

	private static final Logger logger = LoggerFactory.getLogger(Value.class);

	private String val;
	private String format;
	private DataTypes type;
	private TimeUnits unit;
	private Keywords function;
	private String processedStr;

	private List<Value> vals;
	
	/**
	 * true if there is multiple keys to be match with expression
	 */
	private boolean isMulti;

	/**
	 * true if value is must be matched
	 */
	private boolean isMust;

	/**
	 * true if value is for like query
	 */
	private boolean isWildcard;

	private Value(String val, String processed, DataTypes type, String format,
			TimeUnits unit, Keywords func, boolean multi, boolean must, boolean wildcard) {
		this.isMulti = multi;
		if (this.isMulti) {
			List<String> strs = IUtils.getListFromString(val, null);
			this.vals = new ArrayList<>();
			for (String str : strs) {
				this.vals.add(parse(str));
			}
		} else {
			this.val = val;
		}
		this.type = type;
		this.unit = unit;
		this.isMust = must;
		this.format = format;
		this.function = func;
		this.isWildcard = wildcard;
		this.processedStr = processed;
	}

	public String getVal() {
		return val;
	}

	public DataTypes getType() {
		return type;
	}

	public TimeUnits getUnit() {
		return unit;
	}

	public Keywords getFunction() {
		return function;
	}

	public String getFormat() {
		return format;
	}

	public String getProcessedStr() {
		return processedStr;
	}

	public List<Value> getVals() {
		return vals;
	}

	public boolean isMulti() {
		return isMulti;
	}

	public boolean isMust() {
		return isMust;
	}

	public boolean isWildcard() {
		return isWildcard;
	}

	public Long getDateValue() {
		if (!IUtils.isNull(function) && Keywords.TODATE == function) {
			return IUtilities.getLongTime(val, format);
		}
		return null;
	}

	/**
	 * Method to extract query value from input string.
	 * @param in
	 * @return
	 */
	public static Value parse(String in) {
		return parse(in, null);
	}

	/**
	 * Method to extract query value from input string.
	 * @param in
	 * @param func
	 * @return
	 */
	public static Value parse(String in, Keywords func) {
		Value val = null;
		StringBuilder prcd = new StringBuilder();
		if (!IUtils.isNullOrEmpty(in)) {
			String value = null, format = null;
			TimeUnits unit = null;
			DataTypes type = null;
			boolean multi = false, must = false, wildcard = false;
			// Check if value has any function
			if (IUtilities.isStartWithFunction(in)) {
				func = IUtilities.getFunction(in, false);
				prcd.append(func.getKey());
				in = IUtilities.removeProcessedString(in, func.getKey());
			}
			// Check for must query
			if (in.startsWith(Keywords.MUST.getKey())) {
				must = true;
				prcd.append(Keywords.MUST.getKey());
				in = IUtilities.removeProcessedString(in, Keywords.MUST.getKey());
			}
			// Check if there is group in value
			if (IUtilities.isStartWithGroup(in)) {
				Keywords grp = IUtilities.getStartWithGroup(in);
				prcd.append(IUtilities.getGroupValue(in, grp, true));
				value = IUtilities.getGroupValue(in, grp, false);
				in = IUtilities.removeProcessedString(in, grp.getKey());
				// Refine value if its function todate then extract format form it.
				if (!IUtils.isNull(func)) {
					if (Keywords.AFTER == func || Keywords.BEFORE == func) {
						List<String> lst = IUtils.getListFromString(value, null);
						if (!IUtils.isNull(lst) && lst.size() == 2) {
							value = IUtilities.getParsedValue(lst.get(0));
							String tunit = IUtilities.getParsedValue(lst.get(1));
							unit = TimeUnits.fromString(tunit);
						} else {
							logger.info("Invalid use of function should have 2 params");
						}
					} else if (Keywords.TODATE == func && value.contains(",")) {
						List<String> lst = IUtils.getListFromString(value, null);
						if (!IUtils.isNull(lst) && lst.size() == 2) {
							value = IUtilities.getParsedValue(lst.get(0));
							format = IUtilities.getParsedValue(lst.get(1));
						}
					} else if (Keywords.SmlBrkt == grp) {// date or regex function
						if (IUtilities.isStartWithGroup(value)) {
							value = IUtilities.getParsedValue(value);
						}
					}
					if (Keywords.TODATE == func) {
						type = DataTypes.DATE;
					}
				} else /*if (Keywords.SmlBrkt == grp)*/ {// multi value case
					multi = true;
				}
			} else {// Get the first string as value
				value = IUtilities.getFirstString(in);
				prcd.append(value);
				in = IUtilities.removeProcessedString(in, value);
			}
			if (IUtilities.hasWildcard(value)) {
				wildcard = true;
			}
			// Detect and set value type
			if (IUtils.isNull(type)) {
				type = DataTypes.findType(value);
			}
			val = new Value(value, prcd.toString(), type, format,
					unit, func, multi, must, wildcard);
		}
		logger.debug("Parsed Value: " + val);
		return val;
	}

	/**
	 * Method to return before date
	 * @return
	 */
	public Date getDateBefore() {
		if (!IUtils.isNull(unit) && !IUtils.isNullOrEmpty(val) &&
				val.matches("[+|-]?\\d+")) {
			try {
				return Date.from(LocalDateTime.now().minus(
						Long.parseLong(val), unit.getChronoUnit())
						.toInstant(ZoneOffset.UTC)
						);
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
		return null;
	}

	/**
	 * Method to return after date
	 * @return
	 */
	public Date getDateAfter() {
		if (!IUtils.isNull(unit) && !IUtils.isNullOrEmpty(val) &&
				val.matches("[+|-]?\\d+")) {
			try {
				return Date.from(LocalDateTime.now().plus(
						Long.parseLong(val), unit.getChronoUnit())
						.toInstant(ZoneOffset.UTC)
						);
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		if (val != null)
			builder.append("\"val\": \"").append(val).append("\", ");
		if (format != null)
			builder.append("\"format\": \"").append(format).append("\", ");
		if (type != null)
			builder.append("\"type\": \"").append(type).append("\", ");
		if (unit != null)
			builder.append("\"timeUnit\": \"").append(unit.getUnit()).append("\", ");
		if (function != null)
			builder.append("\"function\": \"").append(function).append("\", ");
		if (processedStr != null)
			builder.append("\"processedStr\": \"").append(processedStr).append("\", ");
		if (vals != null)
			builder.append("\"vals\": \"").append(vals).append("\", ");
		builder.append("\"isMulti\": ").append(isMulti).append(", \"isMust\": ")
				.append(isMust).append(", \"isWildcard\": ").append(isWildcard)
				.append(" }");
		return builder.toString();
	}
}
