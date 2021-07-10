/**
 * 
 */
package com.synectiks.policy.runner.controllers;

import java.util.Base64;
import java.util.Map;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * @author Rajesh
 */
@RestController
@RequestMapping(path = "/gelf")
public class GelfController {

	private static final Logger logger = LoggerFactory.getLogger(GelfController.class);

	@Autowired
	private GelfTransport transport;
	@Autowired
	private GelfConfiguration gelfConfig;
	@Autowired
	private RestTemplate rest;
	@Autowired
	private Environment env;

	/**
	 * Method to create a basic authentication header.
	 * @return
	 */
	private Map<String, String> getAuthHeader() {
		String encoding = Base64.getEncoder().encodeToString((
				env.getProperty(IConstants.GELF_USER) + ":" +
				env.getProperty(IConstants.GELF_PASS)).getBytes());
		//String encoding = Base64.getEncoder().encodeToString(("test1:test1").getBytes());
		logger.info("Auth: " + encoding);
		return IUtils.getRestParamMap(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
	}

	@GetMapping("/list/indexes")
	public ResponseEntity<Object> listAllIndexes() {
		String url = String.format(IConstants.GET_GELF_INDEXES, gelfConfig.getHostname(), gelfConfig.getPort());
		logger.info("Url: " + url);
		Object res = null;
		try {
			Map<String, String> hdrs = IUtilities.getAuthHeader(env.getProperty(IConstants.GELF_USER), 
					env.getProperty(IConstants.GELF_PASS));
			res = IUtils.sendGetRestReq(url, hdrs, null);
			logger.info("Res: " + res);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(e.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}
}
