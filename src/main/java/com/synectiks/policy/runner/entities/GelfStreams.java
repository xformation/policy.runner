/**
 * 
 */
package com.synectiks.policy.runner.entities;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.synectiks.commons.entities.PSqlEntity;

/**
 * @author Rajesh
 */

@Entity
@Table(name = "gelf_streams")
public class GelfStreams extends PSqlEntity {

	private static final long serialVersionUID = 3548090749851646533L;

	private String gelfId;
	private String title;
	private String description;
	@ElementCollection(targetClass = Long.class)
	private List<Long> rules;
	private String content_pack;
	private String matching_type;
	private boolean remove_matches_from_default_stream;
	private String index_set_id;

	public String getGelfId() {
		return gelfId;
	}

	public void setGelfId(String gelfId) {
		this.gelfId = gelfId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Long> getRules() {
		return rules;
	}

	public void setRules(List<Long> rules) {
		this.rules = rules;
	}

	public String getContent_pack() {
		return content_pack;
	}

	public void setContent_pack(String content_pack) {
		this.content_pack = content_pack;
	}

	public String getMatching_type() {
		return matching_type;
	}

	public void setMatching_type(String matching_type) {
		this.matching_type = matching_type;
	}

	public boolean isRemove_matches_from_default_stream() {
		return remove_matches_from_default_stream;
	}

	public void setRemove_matches_from_default_stream(boolean remove_matches_from_default_stream) {
		this.remove_matches_from_default_stream = remove_matches_from_default_stream;
	}

	public String getIndex_set_id() {
		return index_set_id;
	}

	public void setIndex_set_id(String index_set_id) {
		this.index_set_id = index_set_id;
	}
}
