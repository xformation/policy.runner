/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.synectiks.commons.entities.CloudAccount;

/**
 * @author Rajesh
 */
@Repository
public interface CloudAccountRepository extends CrudRepository<CloudAccount, Long> {

}
