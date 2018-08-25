package com.synectiks.policy.runner.utils;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.utils.IUtils;

/**
 * A node in a hierarchy of nested substrings. Sometimes, it is necessary to
 * find substrings of a string between two delimiters. This is usually easy,
 * unless the delimiters can be nested. Then things can get hairy.
 * 
 * This class holds data about the bounds of a subrange found in a larger
 * string, the parent subrange, the child subranges, and the text itself. The
 * calculated starts and ends can include or exclude the delimiters. The root
 * node of the tree can also have an error message.
 * 
 * If the delimiters are equal, the program acts as if the odd-numbered ones are
 * opening delimiters, and the even-numbered ones are closing.
 */
public class NestedString {

	private static final Logger logger = LoggerFactory.getLogger(NestedString.class);

	private int start, end;
	private NestedString parent;
	private List<NestedString> children = null;
	private String error = "";
	private String originalText, leftDelimiter, rightDelimiter;
	private boolean includesDelims = false;

	public NestedString(String originalText, String leftDelimiter, String rightDelimiter,
			boolean includesDelims) {
		this(null, originalText, leftDelimiter, rightDelimiter, includesDelims);
	}

	public NestedString(NestedString parent, String originalText, String leftDelimiter,
			String rightDelimiter, boolean includesDelims) {
		this.parent = parent;
		this.originalText = originalText;
		this.leftDelimiter = leftDelimiter;
		this.rightDelimiter = rightDelimiter;
		this.includesDelims = includesDelims;
		this.children = new ArrayList<>();
	}

	public List<NestedString> getChildren() {
		return children;
	}

	public int getEnd() {
		return end;
	}

	public String getError() {
		return error;
	}

	public boolean getIncludesDelims() {
		return includesDelims;
	}

	public String getLeftDelimiter() {
		return leftDelimiter;
	}

	public String getOriginalText() {
		return originalText;
	}

	public NestedString getParent() {
		return parent;
	}

	public String getRightDelimiter() {
		return rightDelimiter;
	}

	public int getStart() {
		return start;
	}

	public String getTextInRange() {
		return originalText.substring(start, end);
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		try {
			json.put("start", start);
			json.put("end", end);
			json.put("leftDelimiter", leftDelimiter);
			json.put("rightDelimiter", rightDelimiter);
			json.put("originalText", originalText);
			json.put("text", getTextInRange());
			if (!IUtils.isNull(children) && !children.isEmpty()) {
				JSONArray childs = new JSONArray();
				for (NestedString ns : children) {
					childs.put(ns.toJson());
				}
				json.put("children", childs);
			}
		} catch (JSONException je) {
			logger.error(je.getMessage(), je);
		}
		return json;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public static String getUpperGroupString(NestedString nstStr) {
		if (!IUtils.isNull(nstStr) && nstStr.getChildren().size() > 0) {
			return nstStr.getChildren().get(0).getTextInRange();
		}
		return null;
	}

	public static NestedString parse(String text, String left, String right,
			boolean includeDelims) throws SynectiksException {

		final NestedString out = new NestedString(text, left, right, includeDelims);
		out.start = 0;
		out.end = text.length();
		if (IUtils.isNullOrEmpty(text) && IUtils.isNullOrEmpty(text) &&
				IUtils.isNullOrEmpty(text)) {
			throw new SynectiksException("One or more arguments are null or empty.");
		}
		boolean lcr = left.contains(right);
		boolean rcl = right.contains(left);
		if (lcr != rcl) {
			logger.warn("The delimiters are not equal and one of them contains the other.");
			return out;
		}

		/** Are the delimiters identical? */
		final boolean eq = lcr && rcl;
		final int ll = left.length();
		final int rl = right.length();

		/**
		 * A range that has been open but not closed. Will be made the parent of
		 * the next range.
		 */
		NestedString parent = out;
		int searchStart = 0;
		while (true) {
			if (eq) {
				// Just find the next two instances of the delimiter.
				int lpos = text.indexOf(left, searchStart);
				if (lpos < searchStart) {
					out.error = "Odd number of occurences of equal delimiters";
					break;
				}
				searchStart = lpos + ll;
				int rpos = text.indexOf(right, searchStart);
				if (rpos < searchStart) {
					break;
				}
				searchStart = rpos + rl;
				NestedString ns = new NestedString(out, text, left, right, includeDelims);
				ns.start = lpos + (includeDelims ? 0 : ll);
				ns.end = rpos + (includeDelims ? rl : 0);
				out.children.add(ns);
			} else {
				int lpos = text.indexOf(left, searchStart);
				int rpos = text.indexOf(right, searchStart);
				if (rpos < searchStart) {
					// Since no right delimiters were found, no ranges can be
					// closed.
					if (parent != out) {
						// we're in an inner level, and there are still unclosed
						// ranges.
						out.error = "Too many opening delimiters.";
					}
					break;
				}
				if (lpos < searchStart) {
					lpos = Integer.MAX_VALUE;
				}
				if (rpos < lpos) {
					// The next delimiter closes the current range
					if (parent == out) {
						// We're in the outermost level and trying to close it.
						// Derp.
						out.error = "Too many closing delimiters.";
						break;
					}
					parent.end = rpos + (includeDelims ? rl : 0);
					parent.parent.children.add(parent);
					parent = parent.parent;
					searchStart = rpos + rl;
				} else {
					// The next delimiter opens a new can of worms.
					NestedString ns = new NestedString(parent, text, left, right,
							includeDelims);
					ns.start = lpos + (includeDelims ? 0 : ll);
					parent = ns;
					searchStart = lpos + ll;
				}
			}
		}
		return out;
	}
}
