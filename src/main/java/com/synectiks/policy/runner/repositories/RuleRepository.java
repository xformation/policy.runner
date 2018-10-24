/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.stereotype.Repository;

import com.synectiks.commons.entities.Rule;
import com.synectiks.schemas.repositories.DynamoDbRepository;

/**
 * @author Rajesh
 */
@Repository
public class RuleRepository extends DynamoDbRepository<Rule, String> {

	public RuleRepository() {
		super(Rule.class);
	}

}
