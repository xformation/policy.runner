/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.synectiks.policy.runner.entities.GelfInputConfig;

/**
 * @author Rajesh
 */
@Repository
public interface GelfInputConfigRepository extends CrudRepository<GelfInputConfig, Long> {

}
