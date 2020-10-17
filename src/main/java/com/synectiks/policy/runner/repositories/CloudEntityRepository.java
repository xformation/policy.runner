/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.synectiks.commons.entities.CloudEntity;

/**
 * @author Rajesh
 */
@Repository
public interface CloudEntityRepository extends CrudRepository<CloudEntity, Long> {

	@Query("select ce from #{#entityName} ce group by ce.cloudName, ce.groupName order by ce.entity")
	public List<CloudEntity> findByOrderAndSort();

}
