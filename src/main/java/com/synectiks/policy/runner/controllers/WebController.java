package com.synectiks.policy.runner.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.translators.QueryParser;

/**
 * @author Rajesh
 */
@Controller
@CrossOrigin
public class WebController {

	private static final Logger logger = LoggerFactory.getLogger(WebController.class);

	@RequestMapping(path = "/", method = RequestMethod.GET)
	public String index(ModelMap model) {
		return "index";
	}

	@RequestMapping(path = "/translate", method = RequestMethod.POST)
	public @ResponseBody ObjectNode translate(HttpServletRequest req,
			HttpServletResponse res, @RequestBody String body) {
		logger.info("body: " + body);
		logger.info("Params: " + req.getParameterMap());
		JSONObject jObj = null;
		try {
			jObj = new JSONObject(body);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		logger.info("query: " + jObj);
		ObjectNode json = null;
		try {
			JSONObject map = new QueryParser(jObj.optString("query")).parse();
			json = IUtils.getObjectFromValue(map.toString(), ObjectNode.class);
			logger.info("output: " + json);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		return json;
	}

}
