/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.synectiks.commons.entities.Customer;

/**
 * @author Rajesh
 */
@Repository
public interface CustomerRepository extends CrudRepository<Customer, Long> {

}
