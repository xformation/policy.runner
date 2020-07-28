/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.stereotype.Repository;

import com.synectiks.commons.constants.IDBConsts;
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.schemas.repositories.DynamoDbRepository;

/**
 * @author Rajesh
 */
@Repository
public class PolicyRepository extends DynamoDbRepository<Policy, String> {

	public PolicyRepository() {
		super(Policy.class);
	}

	public Policy findByName(String name) {
		Iterable<Policy> states = super.findByKeyValue(IDBConsts.Col_NAME, name);
		if (!IUtils.isNull(states) && !IUtils.isNull(states.iterator())
				&& states.iterator().hasNext()) {
			return states.iterator().next();
		}
		return null;
	}

}
