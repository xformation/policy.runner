/**
 * 
 */
package com.synectiks.policy.runner.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

//import com.synectiks.commons.constants.IDBConsts;
import com.synectiks.commons.entities.Policy;
//import com.synectiks.commons.utils.IUtils;

/**
 * @author Rajesh
 */
@Repository
public interface PolicyRepository extends CrudRepository<Policy, Long> {

//	public Policy findByName(String name) {
//		Iterable<Policy> states = super.findByKeyValue(IDBConsts.Col_NAME, name);
//		if (!IUtils.isNull(states) && !IUtils.isNull(states.iterator())
//				&& states.iterator().hasNext()) {
//			return states.iterator().next();
//		}
//		return null;
//	}

}
