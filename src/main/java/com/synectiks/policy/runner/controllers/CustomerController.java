/**
 * 
 */
package com.synectiks.policy.runner.controllers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.constants.IDBConsts;
import com.synectiks.commons.entities.Customer;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.repositories.CustomerRepository;

/**
 * @author Rajesh
 */
@CrossOrigin
@RestController
@RequestMapping(path = IApiController.CUSTOMER_API, method = RequestMethod.POST)
public class CustomerController {

	private static final Logger logger = LoggerFactory.getLogger(
			CustomerController.class);

	@Autowired
	private CustomerRepository repository;

	@RequestMapping(path = IConsts.API_FIND_ALL, method = RequestMethod.GET)
	public ResponseEntity<Object> findAll(HttpServletRequest request) {
		Object entities = null;
		try {
			entities = (List<Customer>) repository.findAll();
			//entities = this.getSurveyEntityResult(request, list);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(entities);
	}

	@RequestMapping(path = IConsts.API_FIND_ID, method = RequestMethod.GET)
	public ResponseEntity<Object> findById(Long id) {
		Customer entity = null;
		try {
			entity = repository.findById(id).orElse(null);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(entity);
	}

	@RequestMapping(path = IConsts.API_DELETE_ID)
	public ResponseEntity<Object> deleteById(Long id) {
		try {
			repository.deleteById(id);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body("Policy removed Successfully");
	}

	@RequestMapping(path = IConsts.API_CREATE)
	public ResponseEntity<Object> create(@RequestBody ObjectNode entity,
			HttpServletRequest request) {
		Customer cust = null;
		try {
			String user = IUtils.getUserFromRequest(request);
			cust = IUtils.createEntity(entity, user, Customer.class);
			cust = repository.save(cust);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(cust);
	}

	@RequestMapping(path = IConsts.API_UPDATE)
	public ResponseEntity<Object> update(@RequestBody ObjectNode entity,
			HttpServletRequest request) {
		return create(entity, request);
	}

	@RequestMapping(path = IConsts.API_DELETE)
	public ResponseEntity<Object> delete(@RequestBody ObjectNode entity) {
		if (!IUtils.isNull(entity.get(IDBConsts.Col_ID))) {
			return deleteById(entity.get(IDBConsts.Col_ID).asLong());
		}
		return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
				.body(IUtils.getFailedResponse("Not a valid entity"));
	}

}
