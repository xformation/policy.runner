/**
 * 
 */
package com.synectiks.policy.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.salesforce.dynamodbv2.mt.context.MTAmazonDynamoDBContextProvider;
import com.salesforce.dynamodbv2.mt.context.impl.MTAmazonDynamoDBContextProviderImpl;
import com.salesforce.dynamodbv2.mt.mappers.MTAmazonDynamoDBByTable;

/**
 * @author Rajesh
 */
@Configuration
public class AppConfig {

	private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

	@Value("${amazon.dynamodb.endpoint}")
	private String dynamoDbEndpoint;
	@Value("${synecticks.customer.table.prefix}")
	private String tablePrefix;
	@Value("${multitenant.context.key}")
	private String contextKey;

	@Bean
	public MTAmazonDynamoDBContextProvider mtContextProvider() {
		logger.info("DynamoDB context provider initialized: " + contextKey);
		MTAmazonDynamoDBContextProviderImpl context = new MTAmazonDynamoDBContextProviderImpl();
		context.setContext(contextKey);
		return context;
	}

	@Bean
	public MTAmazonDynamoDBByTable mtDynamoTable() {
		logger.info("DynamoDB mt table initialized");
		return MTAmazonDynamoDBByTable.builder()
				.withAmazonDynamoDB(AmazonDynamoDBClientBuilder.standard()
						.withEndpointConfiguration(new EndpointConfiguration(
								dynamoDbEndpoint, Regions.EU_WEST_1.getName()))
						.build())
				.withTablePrefix(tablePrefix)
				.withContext(mtContextProvider())
				.build();
	}

	@Bean
	public DynamoDBMapper dynamoDbMapper(AmazonDynamoDB amazonDynamoDB) {
		logger.trace("Entering dynamoDbMapper()");
		return new DynamoDBMapper(amazonDynamoDB);
	}

	@Bean
	public DynamoDB dynamoDb(AmazonDynamoDB amazonDynamoDB) {
		return new DynamoDB(amazonDynamoDB);
	}
}
