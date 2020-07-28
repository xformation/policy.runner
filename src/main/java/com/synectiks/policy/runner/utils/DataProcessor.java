/**
 * 
 */
package com.synectiks.policy.runner.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synectiks.commons.utils.IUtils;

/**
 * @author Rajesh Upadhyay
 */
public class DataProcessor {

	private static Logger logger = LoggerFactory.getLogger(DataProcessor.class);

	private static List<String> processed;

	private static final Object ARRAY = "Array";
	private static final String OBJECT = "object";
	private static final String PATTERN = "pattern";
	private static final String TYPE = "type";

	private static final String inputJSON = "{"
			+ "	drive: \"^Volume in drive (\\\\w+) .*\","
			+ "	serial: \".* Serial Number is (.*)$\","
			+ "	fullPath: \"^Directory of (.*)$\","
			+ "	dirs: {"
			+ "		type: \"Array\","
			+ "		pattern: \"^((\\\\d+)-(\\\\w+)-(\\\\d+)\\\\s+(\\\\d+):(\\\\d+)\\\\s(\\\\w+))\\\\s+<DIR>\\\\s+(.*)$\","
			+ "		object: {"
			+ "			name: 8,"
			+ "			modifiedAt: 1"
			+ "		}"
			+ "	},"
			+ "	files: {"
			+ "		type: \"Array\","
			+ "		pattern: \"^((\\\\d+)-(\\\\w+)-(\\\\d+)\\\\s+(\\\\d+):(\\\\d+)\\\\s(\\\\w+))\\\\s+([0-9,]+)\\\\s+(.*)$\","
			+ "		object: {"
			+ "			name: 9,"
			+ "			modifiedAt: 1,"
			+ "			size: 8"
			+ "		}"
			+ "	},"
			+ "	fileCount: \"^(\\\\d+) File\\\\(s\\\\) .*\","
			+ "	filesSize: \"^\\\\d+ File\\\\(s\\\\)\\\\s+([0-9,]+) bytes$\","
			+ "	dirCount: \"^(\\\\d+) Dir\\\\(s\\\\) .*\","
			+ "	freeSpace: \"^\\\\d+ Dir\\\\(s\\\\)\\\\s+([0-9,]+) bytes free$\"" + "}";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String in = "18-Nov-19  12:42 PM    <DIR>          .";
//		String regex = "^((\\d+)-(\\w+)-(\\d+)\\s+(\\d+):(\\d+)\\s(\\w+))\\s+<DIR>\\s+(.*)$";
//		logger.info(in + "\n" + regex + "\nRes: " + isMatch(in, regex));
//		logger.info(in + "\n" + regex + "\nRes: " + findGroup(in, regex, 10));
//		System.exit(0);
//		String in = "12-Mar-18  02:14 AM        54,619,076 AUD-20180312-WA0038.mp3";
//		String regex = "^((\\d+)-(\\w+)-(\\d+)\\s+(\\d+):(\\d+)\\s(\\w+))\\s+([0-9,]+)\\s+(.*)$";
//		logger.info(in + "\n" + regex + "\nRes: " + isMatch(in, regex));
//		logger.info(in + "\n" + regex + "\nRes: " + findGroup(in, regex, 10));
//		System.exit(0);
		File inputFile = getInputFile();
		if (!IUtils.isNull(inputFile)) {
			JSONObject json = IUtils.getJSONObject(inputJSON);
			logger.info("JSON: " + json);
			JSONObject output = process(inputFile, json);
			logger.info("Output: " + output);
		} else {
			logger.error("Input File NOT Found");
		}
	}

	private static JSONObject process(File inputFile, JSONObject json) {
		processed = new ArrayList<>();
		JSONObject output = new JSONObject();
		Map<String, Object> keys = getKeyValues(json);
		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			String line = br.readLine();
			while (!IUtils.isNull(line)) {
				if (!IUtils.isNullOrEmpty(line)) {
					processLine(line.trim(), keys, output);
				}
				line = br.readLine();
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return output;
	}

	private static void processLine(String line, Map<String, Object> keys,
			JSONObject output) {
		Iterator<String> it = keys.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (processed.contains(key)) {
				continue;
			}
			Object val = keys.get(key);
			if (!IUtils.isNull(val)) {
				if (val instanceof JSONObject) {
					// process a object match
					JSONObject json = (JSONObject) val;
					String pattern = json.optString(PATTERN);
					if (!IUtils.isNullOrEmpty(pattern) && isMatch(line, pattern)) {
						String type = json.optString(TYPE);
						if (!IUtils.isNullOrEmpty(type) && ARRAY.equals(type)) {
							JSONObject jobj = json.optJSONObject(OBJECT);
							putInJson(output, key,
									processInnerObj(line, pattern, jobj),
									true);
						}
					}
				} else {
					String regex = val.toString();
					if (isMatch(line, regex)) {
						putInJson(output, key, getFirstGroup(line, regex), false);
						// remove the key from map
						processed.add(key);
					}
				}
			}
		}
	}

	private static JSONObject processInnerObj(
			String line, String pattern, JSONObject json) {
		Map<String, Object> keys = getKeyValues(json);
		List<String> groups = listGroups(line, pattern);
		JSONObject out = new JSONObject();
		keys.entrySet().forEach(entry -> {
			String key = entry.getKey();
			int indx = -1;
			if (!IUtils.isNull(entry.getValue()) && entry.getValue() instanceof Number) {
				indx = (int) entry.getValue();
			}
			if (indx > 0) {
				putInJson(out, key, groups.get(indx), false);
			}
		});
		return out;
	}

	private static void putInJson(
			JSONObject output, String key, Object val, boolean isArr) {
		logger.info("Saving: " + key + " -> " + val);
		try {
			if (output.has(key)) {
				JSONArray arr = output.optJSONArray(key);
				arr.put(val);
				output.put(key, arr);
			} else {
				if (isArr) {
					JSONArray arr = new JSONArray();
					val = arr.put(val);
				}
				output.put(key, val);
			}
		} catch (JSONException e) {
			logger.error("Failed to add in json: " + e.getMessage());
		}
	}

	private static Map<String, Object> getKeyValues(JSONObject json) {
		Map<String, Object> map = new HashMap<>();
		@SuppressWarnings("rawtypes")
		Iterator it = json.keys();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object val = json.opt(key);
			if (!IUtils.isNull(val)) {
				map.put(key, val);
			}
		}
		return map;
	}

	private static boolean isMatch(String input, String regex) {
		Pattern r = Pattern.compile(regex);
		// Now create matcher object.
		Matcher m = r.matcher(input);
		if (m.matches()) {
			logger.info("Matched: " + regex + " -> " + input);
			return true;
		}
		logger.info("Match failed: " + regex + " -> " + input);
		return false;
	}

	private static String getFirstGroup(String input, String regex) {
		return findGroup(input, regex, 1);
	}

	private static List<String> listGroups(String input, String regex) {
		List<String> lst = new ArrayList<>();
		Pattern r = Pattern.compile(regex);
		// Now create matcher object.
		Matcher m = r.matcher(input);
		if (m.matches()) {
			for (int i = 0; i <= m.groupCount(); i++) {
				logger.info("Group[" + i + "]: " + m.group(i));
				lst.add(m.group(i));
			}
		}
		return lst;
	}

	private static String findGroup(String input, String regex, int grpIndx) {
		Pattern r = Pattern.compile(regex);
		// Now create matcher object.
		Matcher m = r.matcher(input);
		if (m.matches()) {
			for (int i = 0; i <= m.groupCount(); i++) {
				logger.info("Group[" + i + "]: " + m.group(i));
				if (i == grpIndx) {
					return m.group(i);
				}
			}
		}
		return null;
	}

	private static File getInputFile() {
		String fileName = "D:\\Rajesh\\PRJ\\GitHub\\policy.runner\\src\\main\\java\\com\\synectiks\\policy\\runner\\utils\\dirOutPut.log";
		File file = new File(fileName);
		logger.info("File path: " + file.getAbsolutePath());
		if (file.exists()) {
			return file;
		}
		return null;
	}

}
