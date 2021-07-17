/**
 * 
 */
package com.synectiks.policy.runner.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.synectiks.commons.entities.PSqlEntity;

/**
 * @author Rajesh
 */
@Entity
@Table(name = "gelf_index_set")
public class GelfIndexSet extends PSqlEntity {

	private static final long serialVersionUID = -2862656832508737588L;

	private String gelfId;
	private String title;
	private String description;
	@JsonProperty("default")
	private boolean _default;
	private boolean writable;
	private String index_prefix;
	private int shards;
	private int replicas;
	private String rotation_strategy_class;
	private Strategy rotation_strategy;
	private String retention_strategy_class;
	private Strategy retention_strategy;
	private String index_analyzer;
	private int index_optimization_max_num_segments;
	private boolean index_optimization_disabled;
	private int field_type_refresh_interval;
	private String index_template_type;

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

	public boolean is_default() {
		return _default;
	}

	public void set_default(boolean _default) {
		this._default = _default;
	}

	public boolean isWritable() {
		return writable;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	public String getIndex_prefix() {
		return index_prefix;
	}

	public void setIndex_prefix(String index_prefix) {
		this.index_prefix = index_prefix;
	}

	public int getShards() {
		return shards;
	}

	public void setShards(int shards) {
		this.shards = shards;
	}

	public int getReplicas() {
		return replicas;
	}

	public void setReplicas(int replicas) {
		this.replicas = replicas;
	}

	public String getRotation_strategy_class() {
		return rotation_strategy_class;
	}

	public void setRotation_strategy_class(String rotation_strategy_class) {
		this.rotation_strategy_class = rotation_strategy_class;
	}

	public Strategy getRotation_strategy() {
		return rotation_strategy;
	}

	public void setRotation_strategy(Strategy rotation_strategy) {
		this.rotation_strategy = rotation_strategy;
	}

	public String getRetention_strategy_class() {
		return retention_strategy_class;
	}

	public void setRetention_strategy_class(String retention_strategy_class) {
		this.retention_strategy_class = retention_strategy_class;
	}

	public Strategy getRetention_strategy() {
		return retention_strategy;
	}

	public void setRetention_strategy(Strategy retention_strategy) {
		this.retention_strategy = retention_strategy;
	}

	public String getIndex_analyzer() {
		return index_analyzer;
	}

	public void setIndex_analyzer(String index_analyzer) {
		this.index_analyzer = index_analyzer;
	}

	public int getIndex_optimization_max_num_segments() {
		return index_optimization_max_num_segments;
	}

	public void setIndex_optimization_max_num_segments(int index_optimization_max_num_segments) {
		this.index_optimization_max_num_segments = index_optimization_max_num_segments;
	}

	public boolean isIndex_optimization_disabled() {
		return index_optimization_disabled;
	}

	public void setIndex_optimization_disabled(boolean index_optimization_disabled) {
		this.index_optimization_disabled = index_optimization_disabled;
	}

	public int getField_type_refresh_interval() {
		return field_type_refresh_interval;
	}

	public void setField_type_refresh_interval(int field_type_refresh_interval) {
		this.field_type_refresh_interval = field_type_refresh_interval;
	}

	public String getIndex_template_type() {
		return index_template_type;
	}

	public void setIndex_template_type(String index_template_type) {
		this.index_template_type = index_template_type;
	}

	/**
	 * Class to hold Rotation and Retention Strategies.
	 * @author Rajesh
	 */
	public static class Strategy implements Serializable {

		private static final long serialVersionUID = -6613220333250450118L;

		private String type;
		private long max_docs_per_index;
		private long max_number_of_indices;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public long getMax_docs_per_index() {
			return max_docs_per_index;
		}

		public void setMax_docs_per_index(long max_docs_per_index) {
			this.max_docs_per_index = max_docs_per_index;
		}

		public long getMax_number_of_indices() {
			return max_number_of_indices;
		}

		public void setMax_number_of_indices(long max_number_of_indices) {
			this.max_number_of_indices = max_number_of_indices;
		}

	}

}
