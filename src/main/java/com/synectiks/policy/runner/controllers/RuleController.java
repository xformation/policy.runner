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
import com.synectiks.commons.entities.Rule;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.repositories.RuleRepository;

@CrossOrigin
@RestController
@RequestMapping(path = IApiController.RULE_API, method = RequestMethod.POST)
public class RuleController {

	private static final Logger logger = LoggerFactory.getLogger(
			RuleController.class);

	@Autowired
	private RuleRepository repository;

	@RequestMapping(path = IConsts.API_FIND_ALL, method = RequestMethod.GET)
	public ResponseEntity<Object> findAll(HttpServletRequest request) {
		Object entities = null;
		try {
			entities = (List<Rule>) repository.findAll();
			//List<Rule> list = (List<Rule>) repository.findAll();
			//entities = this.getSurveyEntityResult(request, list);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(entities);
	}

	@RequestMapping(path = IConsts.API_FIND_ID, method = RequestMethod.GET)
	public ResponseEntity<Object> findById(long id) {
		Rule entity = null;
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
	public ResponseEntity<Object> deleteById(long id) {
		try {
			repository.deleteById(id);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body("Rule removed Successfully");
	}

	@RequestMapping(path = IConsts.API_CREATE)
	public ResponseEntity<Object> create(@RequestBody ObjectNode entity,
			HttpServletRequest request) {
		Rule rule = null;
		try {
			String user = IUtils.getUserFromRequest(request);
			rule = IUtils.createEntity(entity, user, Rule.class);
			rule = repository.save(rule);
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(IUtils.getFailedResponse(th.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.OK).body(rule);
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
