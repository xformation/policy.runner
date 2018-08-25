package com.synectiks.policy.runner.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants.Keywords;

/**
 * @author Rajesh
 */
public interface IUtilities {

	/**
	 * Method to extract first non space char sequence form input.
	 * @param input
	 * @return
	 */
	static String getFirstString(String input) {
		return getRefinedFirstString(input, false);
	}

	/**
	 * Method to find non space char sequence and refine it, if rmList is true,
	 * it removes enclosing <> from output string.
	 * @param input
	 * @param rmList
	 * @return
	 */
	static String getRefinedFirstString(String input, boolean rmList) {
		System.out.println("first: " + input);
		if (!IUtils.isNullOrEmpty(input) && input.contains(" ")) {
			int indx = input.indexOf(" ");
			String res = null;
			if (indx != -1) {
				res = input.substring(0, indx);
			} else {
				res = input;
			}
			if (rmList) {
				return refineEntityName(res.trim());
			} else {
				return res.trim();
			}
		}
		return input;
	}

	/**
	 * Method to removes enclosing <> from input string
	 * @param input
	 * @return
	 */
	static String refineEntityName(String input) {
		if (!IUtils.isNullOrEmpty(input)) {
			int indx = input.indexOf("<");
			if (indx != -1) {
				input = input.substring(indx, input.indexOf(">"));
			}
		}
		return input;
	}

	/**
	 * Method to remove key from input query.
	 * @param query
	 * @param key
	 * @return
	 */
	static String removeProcessedString(String query, String key) {
		if (!IUtils.isNullOrEmpty(query) && !IUtils.isNullOrEmpty(key)) {
			query = query.substring(key.length());
		}
		return (IUtils.isNullOrEmpty(query) ? "" : query.trim());
	}

	/**
	 * Method to find-out the closing index of group operator.
	 * @param input
	 * @param grp
	 * @return
	 */
	static int findClosingIndex(String input, Keywords grp) {
		System.out.println(input + " -> " + grp.getKey());
		if (!IUtils.isNullOrEmpty(input)) {
			String open = grp.getGroupStart();
			String close = grp.getGroupEnd();
			final int strLen = input.length();
			final int closeLen = close.length();
			final int openLen = open.length();
			int pos = 0;
			while (pos < strLen - closeLen) {
				int start = input.indexOf(open, pos);
				if (start < 0) {
					break;
				}
				start += openLen;
				final int end = input.indexOf(close, start);
				if (end < 0) {
					break;
				}
				pos = end + closeLen;
			}
			return pos;
		}
		return -1;
	}
}
