/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import com.synectiks.commons.entities.Rule;
import com.synectiks.schemas.repositories.DynamoDbRepository;

/**
 * @author Rajesh
 */
public class RuleRepository extends DynamoDbRepository<Rule, String> {

	public RuleRepository() {
		super(Rule.class);
	}

}
