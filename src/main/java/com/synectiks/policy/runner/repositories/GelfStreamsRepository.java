/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;

import com.synectiks.policy.runner.entities.GelfStreams;

/**
 * @author Rajesh
 */
public interface GelfStreamsRepository extends CrudRepository<GelfStreams, Long> {

	public GelfStreams findByTitle(String title);

}
