/**
 * 
 */
package com.synectiks.policy.runner.parsers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IConstants.Keywords;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * Class to represent query key
 * @author Rajesh Upadhyay
 */
public class Key implements Serializable {

	private static final long serialVersionUID = 2434155270455332872L;

	private static final Logger logger = LoggerFactory.getLogger(Key.class);

	private String key;
	private String processedStr;

	private List<Key> keys;
	
	/**
	 * true if there is multiple keys to be match with expression
	 */
	private boolean isMulti;
	
	/**
	 * true if key is nested object key
	 */
	private boolean isNested;
	
	/**
	 * true if key has wildcard characters
	 */
	private boolean isWildcard;

	/** true if we have to evaluate the length of value. */
	private boolean isLenCheck;

	private Key(String key, String processedStr, boolean multi) {
		this.isMulti = multi;
		if (this.isMulti) {
			List<String> strs = IUtils.getListFromString(key, null);
			this.keys = new ArrayList<>();
			for (String str : strs) {
				this.keys.add(parse(str));
			}
		} else {
			this.key = key;
			if (this.key.endsWith(".length")) {
				this.isLenCheck = true;
				this.key = this.key.replace(".length", "");
			}
			if (this.key.contains(".")) {
				this.isNested = true;
			} else if (this.key.contains(IConstants.Keywords.STAR.getKey())) {
				this.isWildcard = true;
			}
		}
		this.processedStr = processedStr;
	}

	public String getKey() {
		return key;
	}

	public String getProcessedStr() {
		return processedStr;
	}

	public List<Key> getKeys() {
		return keys;
	}

	public boolean isMulti() {
		return isMulti;
	}

	public boolean isNested() {
		return isNested;
	}

	public boolean isWildcard() {
		return isWildcard;
	}

	public boolean isLenCheck() {
		return isLenCheck;
	}

	/**
	 * Method to extract query key from input string
	 * @param in
	 * @return
	 */
	public static Key parse(String in) {
		Key key = null;
		if (!IUtils.isNullOrEmpty(in)) {
			if ( IUtilities.isStartWithGroup(in)) {
				Keywords grp = IUtilities.getStartWithGroup(in);
				if (IConstants.Keywords.CptlBrkt == grp) {
					String prcd = IUtilities.getGroupValue(in, grp, true);
					String str = IUtilities.getGroupValue(in, grp, false);
					key = new Key(str, prcd, true);
				} else {
					logger.warn("Invalid string key: " + in);
				}
			} else {
				if (!IUtilities.isStartWithHasKeyword(in)) {
					String str = IUtilities.getFirstString(in);
					key = new Key(str, str, false);
				} else {
					logger.warn("Query starts with 'has' operator: " + in);
				}
			}
		}
		logger.debug("Parsed key: " + key);
		return key;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		if (key != null)
			builder.append("\"key\": \"").append(key).append("\", ");
		if (processedStr != null)
			builder.append("\"processedStr\": \"").append(processedStr).append("\", ");
		if (keys != null)
			builder.append("\"keys\": ").append(keys).append(", ");
		builder.append("\"isMulti\": ").append(isMulti).append(", \"isNested\": ")
				.append(isNested).append(", \"isWildcard\": ").append(isWildcard)
				.append(", \"isLenCheck\": ").append(isLenCheck)
				.append(" }");
		return builder.toString();
	}

}
