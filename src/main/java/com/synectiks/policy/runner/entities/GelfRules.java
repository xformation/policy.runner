/**
 * 
 */
package com.synectiks.policy.runner.entities;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.synectiks.commons.entities.PSqlEntity;

/**
 * @author Rajesh
 */
@Entity
@Table(name = "gelf_rules")
public class GelfRules extends PSqlEntity {

	private static final long serialVersionUID = 9171370182736202596L;

	private String gelfId;
	private String type;
	private String value;
	private String filed;
	private boolean inverted;
	private String description;

	public String getGelfId() {
		return gelfId;
	}

	public void setGelfId(String gelfId) {
		this.gelfId = gelfId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFiled() {
		return filed;
	}

	public void setFiled(String filed) {
		this.filed = filed;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
