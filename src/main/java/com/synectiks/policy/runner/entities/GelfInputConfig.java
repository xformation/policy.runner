/**
 * 
 */
package com.synectiks.policy.runner.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.synectiks.commons.entities.PSqlEntity;

/**
 * @author Rajesh
 */
@Entity
@Table(name = "GelfInputConfig")
public class GelfInputConfig extends PSqlEntity {

	private static final long serialVersionUID = -4655889411611064195L;

	private String gelfId;
	private String title;
	private String name;
	private String type;
	private String node;
	private boolean global;
	private GelfAttributes attributes;

	public String getGelfId() {
		return gelfId;
	}

	public void setGelfId(String id) {
		this.gelfId = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public GelfAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(GelfAttributes attributes) {
		this.attributes = attributes;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	/**
	 * Inner class to hold Gelf Input Config Attributes.
	 * 
	 * @author Rajesh
	 */
	public static class GelfAttributes implements Serializable {

		private int recv_buffer_size;
		private boolean tcp_keepalive;
		private boolean use_null_delimiter;
		private int number_worker_threads;
		private String timezone;
		private String tls_client_auth_cert_file;
		private String locale;
		private String bind_address;
		private String tls_cert_file;
		private String port;
		private String tls_key_file;
		private boolean tls_enable;
		private String tls_key_password;
		private int max_message_size;
		private String tls_client_auth;
		private boolean use_full_names;

		public int getRecv_buffer_size() {
			return recv_buffer_size;
		}

		public void setRecv_buffer_size(int recv_buffer_size) {
			this.recv_buffer_size = recv_buffer_size;
		}

		public boolean isTcp_keepalive() {
			return tcp_keepalive;
		}

		public void setTcp_keepalive(boolean tcp_keepalive) {
			this.tcp_keepalive = tcp_keepalive;
		}

		public boolean isUse_null_delimiter() {
			return use_null_delimiter;
		}

		public void setUse_null_delimiter(boolean use_null_delimiter) {
			this.use_null_delimiter = use_null_delimiter;
		}

		public int getNumber_worker_threads() {
			return number_worker_threads;
		}

		public void setNumber_worker_threads(int number_worker_threads) {
			this.number_worker_threads = number_worker_threads;
		}

		public String getTimezone() {
			return timezone;
		}

		public void setTimezone(String timezone) {
			this.timezone = timezone;
		}

		public String getTls_client_auth_cert_file() {
			return tls_client_auth_cert_file;
		}

		public void setTls_client_auth_cert_file(String tls_client_auth_cert_file) {
			this.tls_client_auth_cert_file = tls_client_auth_cert_file;
		}

		public String getLocale() {
			return locale;
		}

		public void setLocale(String locale) {
			this.locale = locale;
		}

		public String getBind_address() {
			return bind_address;
		}

		public void setBind_address(String bind_address) {
			this.bind_address = bind_address;
		}

		public String getTls_cert_file() {
			return tls_cert_file;
		}

		public void setTls_cert_file(String tls_cert_file) {
			this.tls_cert_file = tls_cert_file;
		}

		public String getPort() {
			return port;
		}

		public void setPort(String port) {
			this.port = port;
		}

		public String getTls_key_file() {
			return tls_key_file;
		}

		public void setTls_key_file(String tls_key_file) {
			this.tls_key_file = tls_key_file;
		}

		public boolean isTls_enable() {
			return tls_enable;
		}

		public void setTls_enable(boolean tls_enable) {
			this.tls_enable = tls_enable;
		}

		public String getTls_key_password() {
			return tls_key_password;
		}

		public void setTls_key_password(String tls_key_password) {
			this.tls_key_password = tls_key_password;
		}

		public int getMax_message_size() {
			return max_message_size;
		}

		public void setMax_message_size(int max_message_size) {
			this.max_message_size = max_message_size;
		}

		public String getTls_client_auth() {
			return tls_client_auth;
		}

		public void setTls_client_auth(String tls_client_auth) {
			this.tls_client_auth = tls_client_auth;
		}

		public boolean isUse_full_names() {
			return use_full_names;
		}

		public void setUse_full_names(boolean use_full_names) {
			this.use_full_names = use_full_names;
		}

	}

}
