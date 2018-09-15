package com.synectiks.policy.runner;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.synectiks.commons.config.Config;
import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.SourceEntity;
import com.synectiks.commons.entities.SourceMapping;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;
import com.synectiks.schemas.config.DynamoDbConfig;

@SpringBootApplication
@ComponentScan(basePackages = { "com.synectiks" })
public class PolicyApplication {

	private static final Logger logger = LoggerFactory.getLogger(PolicyApplication.class);

	private static ConfigurableApplicationContext ctx;

	@SuppressWarnings("unused")
	@Autowired
	private DynamoDbConfig dynamoConfig;
	@SuppressWarnings("unused")
	@Autowired
	private Config commonConfig;

	@Autowired
	private Environment env;
	@Autowired
	private RestTemplate rest;

	public static void main(String[] args) {
		ctx = SpringApplication.run(PolicyApplication.class, args);
		for (String bean : ctx.getBeanDefinitionNames()) {
			logger.info("Beans: " + bean);
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void setIndexAndMapping() {
		String searchHost = env.getProperty(IConsts.KEY_SEARCH_URL, "");
		String searchUrl = searchHost + IApiController.URL_SEARCH
				+ IConstants.SET_INDX_MAPPING_URI;
		logger.info("searchUrl: " + searchUrl);
		Map<String, Object> params = IUtils.getRestParamMap(IConsts.PRM_CLASS,
				SourceEntity.class.getName(), IConsts.PRM_MAPPINGS,
				SourceMapping.getSourceEntityMapping().toString());
		logger.info("Request: " + params);
		try {
			Boolean res = IUtils.sendPostRestRequest(rest,
					searchUrl, null, Boolean.class,
					params, MediaType.APPLICATION_FORM_URLENCODED);
			logger.info("Indexing response: " + res);
			// Set keys for suggestion
			IUtilities.fillIndexedKeys(rest, searchHost);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	@Bean
	public DynamoDB dynamoDb(AmazonDynamoDB amazonDynamoDB) {
		return new DynamoDB(amazonDynamoDB);
	}
}
