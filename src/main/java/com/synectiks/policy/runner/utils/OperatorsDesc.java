/**
 * 
 */
package com.synectiks.policy.runner.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants.Keywords;

/**
 * @author Rajesh
 *
 */
public class OperatorsDesc implements Serializable {

	private static final long serialVersionUID = 5511926056333309235L;

	private Map<String, List<Operator>> map;

	public Map<String, List<Operator>> getMap() {
		return map;
	}

	public void setMap(Map<String, List<Operator>> map) {
		this.map = map;
	}

	public void putInMap(String key, List<Operator> lst) {
		if (IUtils.isNull(map)) {
			map = new HashMap<>();
		}
		if (!IUtils.isNullOrEmpty(key) && !IUtils.isNull(lst) &&
				lst.size() > 0) {
			map.put(key, lst);
		}
	}

	public static class Operator implements Serializable {

		private static final long serialVersionUID = -5385476641700567008L;

		private String key;
		private String type;
		private String hint;
		private String supportedTypes;

		private Operator() {
			// Prevent outside instantiation
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getHint() {
			return hint;
		}

		public void setHint(String hint) {
			this.hint = hint;
		}

		public String getSupportedTypes() {
			return supportedTypes;
		}

		public void setSupportedTypes(String supportedTypes) {
			this.supportedTypes = supportedTypes;
		}

		public static Operator create(Keywords kw) {
			Operator op = new Operator();
			op.setHint(kw.getHint());
			op.setKey(kw.getKey());
			op.setType(kw.getType().name());
			op.setSupportedTypes(IUtilities.arrToString(kw.getSupportedTypes()));
			return op;
		}
	}
}
