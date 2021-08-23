/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;

import com.synectiks.policy.runner.entities.GelfRules;

/**
 * @author Rajesh
 */
public interface GelfRulesRepository extends CrudRepository<GelfRules, Long> {

	public GelfRules findByValue(String value);

}
