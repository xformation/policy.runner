package com.synectiks.policy.runner;

import static org.junit.Assert.assertNotNull;

import org.codehaus.jettison.json.JSONObject;

import com.synectiks.commons.entities.Policy;

public class PolicyExecutor {

	private Policy policy;
	private String source;
	private String field;

	public PolicyExecutor(Policy policy, String source, String field) {
		assertNotNull("Policy should not be null", policy);
		this.policy = policy;
		this.source = source;
		this.field = field;
	}

	public JSONObject execute() {
		return null;
	}

}
