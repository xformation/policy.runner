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
import com.synectiks.commons.entities.Policy;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.repositories.PolicyRepository;

/**
 * @author Rajesh
 */
@CrossOrigin
@RestController
@RequestMapping(path = IApiController.PLC_API, method = RequestMethod.POST)
public class PolicyController implements IApiController {

	private static final Logger logger = LoggerFactory.getLogger(
			PolicyController.class);

	@Autowired
	private PolicyRepository repository;

	@Override
	@RequestMapping(path = IConsts.API_FIND_ALL, method = RequestMethod.GET)
	public ResponseEntity<Object> findAll(HttpServletRequest request) {
		Object entities = null;
		try {
			List<Policy> list = (List<Policy>) repository.findAll();
			entities = this.getSurveyEntityResult(request, list);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(entities);
	}

	@Override
	@RequestMapping(path = IConsts.API_FIND_ID, method = RequestMethod.GET)
	public ResponseEntity<Object> findById(String id) {
		Policy entity = null;
		try {
			entity = repository.findById(id).orElse(null);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(entity);
	}

	@Override
	@RequestMapping(path = IConsts.API_DELETE_ID)
	public ResponseEntity<Object> deleteById(String id) {
		try {
			repository.delete(id);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body("Policy removed Successfully");
	}

	@Override
	@RequestMapping(path = IConsts.API_CREATE)
	public ResponseEntity<Object> create(@RequestBody ObjectNode entity,
			HttpServletRequest request) {
		Policy policy = null;
		try {
			String user = IUtils.getUserFromRequest(request);
			policy = IUtils.createEntity(entity, user, Policy.class);
			policy = repository.save(policy);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(policy);
	}

	@Override
	@RequestMapping(path = IConsts.API_UPDATE)
	public ResponseEntity<Object> update(@RequestBody ObjectNode entity,
			HttpServletRequest request) {
		return create(entity, request);
	}

	@Override
	@RequestMapping(path = IConsts.API_DELETE)
	public ResponseEntity<Object> delete(@RequestBody ObjectNode entity) {
		if (!IUtils.isNull(entity.get(IDBConsts.Col_ID))) {
			return deleteById(entity.get(IDBConsts.Col_ID).asText());
		}
		return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
				.body(IUtils.getFailedResponse("Not a valid entity"));
	}

}
