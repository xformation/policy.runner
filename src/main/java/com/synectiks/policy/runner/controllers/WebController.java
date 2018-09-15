package com.synectiks.policy.runner.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Rajesh
 */
@Controller
@CrossOrigin
public class WebController {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(WebController.class);

	/**
	 * MVC api to redirect on index page.
	 * @param model
	 * @return
	 */
	@RequestMapping(path = "/", method = RequestMethod.GET)
	public String index(ModelMap model) {
		return "index";
	}

}
