package com.synectiks.policy.runner;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.SourceEntity;
import com.synectiks.commons.entities.SourceMapping;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;

@SpringBootApplication
@ComponentScan("com.synectiks")
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	private static ConfigurableApplicationContext ctx;

	@Autowired
	private Environment env;
	@Autowired
	private RestTemplate rest;

	public static void main(String[] args) {
		ctx = SpringApplication.run(Application.class, args);
		for (String bean : ctx.getBeanDefinitionNames()) {
			logger.info("Beans: " + bean);
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void setIndexAndMapping() {
		String searchUrl = env.getProperty(IConsts.KEY_SEARCH_URL, "");
		searchUrl += IApiController.URL_SEARCH + "/setIndexMapping";
		logger.info("searchUrl: " + searchUrl);
		Map<String, Object> params = IUtils.getRestParamMap(
				IConsts.PRM_CLASS, SourceEntity.class.getName(),
				IConsts.PRM_MAPPINGS, SourceMapping.getSourceEntityMapping().toString());
		logger.info("Request: " + params);
		Boolean res = IUtils.sendPostRestRequest(rest, searchUrl, null, Boolean.class,
				params, MediaType.APPLICATION_FORM_URLENCODED);
		logger.info("Indexing response: " + res);
	}
}
