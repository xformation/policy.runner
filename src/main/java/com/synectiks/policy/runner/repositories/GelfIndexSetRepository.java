/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;

import com.synectiks.policy.runner.entities.GelfIndexSet;

/**
 * @author Rajesh
 */
public interface GelfIndexSetRepository extends CrudRepository<GelfIndexSet, Long> {

	public GelfIndexSet findByTitle(String title);

}
