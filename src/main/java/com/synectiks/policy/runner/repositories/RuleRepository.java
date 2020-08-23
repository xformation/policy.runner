/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.synectiks.commons.entities.Rule;

/**
 * @author Rajesh
 */
@Repository
public interface RuleRepository extends CrudRepository<Rule, Long> {

}
