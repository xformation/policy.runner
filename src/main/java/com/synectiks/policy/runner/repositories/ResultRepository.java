/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.synectiks.commons.entities.PolicyRuleResult;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.schemas.repositories.DynamoDbRepository;

/**
 * @author Rajesh
 */
@Repository
public class ResultRepository extends DynamoDbRepository<PolicyRuleResult, String> {

	public ResultRepository() {
		super(PolicyRuleResult.class);
	}

	/**
	 * Method to find an entity by policyId and ruleId
	 * @param policyId
	 * @param ruleId
	 * @return
	 */
	public PolicyRuleResult findByPolicyAndRuleId(String policyId, String ruleId) {
		DynamoDBScanExpression exp = new DynamoDBScanExpression();
		exp.addFilterCondition("policyId", new Condition()
				.withComparisonOperator(ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withS(policyId)));
		exp.addFilterCondition("ruleId", new Condition()
				.withComparisonOperator(ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withS(ruleId)));
		Iterable<PolicyRuleResult> states = super.scan(exp);
		if (!IUtils.isNull(states) && !IUtils.isNull(states.iterator())
				&& states.iterator().hasNext()) {
			return states.iterator().next();
		}
		return null;
	}

	/**
	 * Method to check if entry with policy and rule is already exists.
	 * @param entity
	 * @return
	 */
	public PolicyRuleResult saveOrUpdate(PolicyRuleResult entity) {
		if (!IUtils.isNull(entity)) {
			if (IUtils.isNull(entity.getId())) {
				PolicyRuleResult res = findByPolicyAndRuleId(
						entity.getPolicyId(), entity.getRuleId());
				// set existing entity id to update it.
				if (!IUtils.isNull(res)) {
					entity.setId(res.getId());
				}
			}
			return super.save(entity);
		}
		return entity;
	}

}
