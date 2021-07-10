/**
 * 
 */
package com.synectiks.policy.runner;


import java.net.InetSocketAddress;
import java.util.Map;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.synectiks.commons.utils.IUtils;
import com.synectiks.policy.runner.entities.GelfInputConfig;
import com.synectiks.policy.runner.entities.GelfInputConfig.GelfAttributes;
import com.synectiks.policy.runner.repositories.GelfInputConfigRepository;
import com.synectiks.policy.runner.utils.IConstants;
import com.synectiks.policy.runner.utils.IUtilities;

/**
 * @author Rajesh
 *
 */
@Configuration
public class GelfMessageConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(GelfMessageConfiguration.class);

	@Autowired
	private Environment env;
	@Autowired
	private GelfInputConfigRepository inputRepo;

	/**
	 * Method to create Gelf Config instance.
	 * @param host
	 * @param port
	 * @param queyeSize
	 * @param timeout
	 * @param delay
	 * @param noDelay
	 * @param bufferSize
	 * @return
	 */
	private GelfConfiguration getGelfConfig(String host, int port,
			int queyeSize, int timeout, int delay, boolean noDelay, int bufferSize) {
		return new GelfConfiguration(new InetSocketAddress(host, port))
		        .transport(GelfTransports.TCP)
		        .queueSize(queyeSize)
		        .connectTimeout(timeout)
		        .reconnectDelay(delay)
		        .tcpNoDelay(noDelay)
		        .sendBufferSize(bufferSize);
	}

	/**
	 * Create bean for Default Gelf Configuration.
	 * @return
	 */
	@Bean
	public GelfConfiguration getGelfConfiguration() {
		String host = this.env.getProperty(IConstants.GULF_HOST);
		int port = Integer.parseInt(env.getProperty(IConstants.GULF_PORT));
		return getGelfConfig(host, port, 512, 5000, 1000, true, 32768);
	}

	/**
	 * Method to get Gelf TCP Transport bean.
	 * @return
	 */
	@Bean
	public GelfTransport getGelfTransport() {
		GelfTransport trans = null;
		trans = getPolicyResultInputGelfTransport();
		if (IUtils.isNull(trans)) {
			trans = GelfTransports.create(getGelfConfiguration());
		}
		return trans;
	}

	/**
	 * Method to return Gelf transport object saving policy rule result.
	 * @return
	 */
	//@Bean
	public GelfTransport getPolicyResultInputGelfTransport() {
		GelfInputConfig gic = getGelfInputConfig();
		if (!IUtils.isNull(gic)) {
			String host = gic.getAttributes().getBind_address();
			int port = Integer.valueOf(gic.getAttributes().getPort());
			return GelfTransports.create(this.getGelfConfig(host, port, 512, 5000, 1000, true, 32768));
		}
		logger.error("*************Filed to create GelfTransport Bean************");
		return null;
	}

	/**
	 * Method to load or create GelfInputConfig object.
	 * @return
	 */
	private GelfInputConfig getGelfInputConfig() {
		GelfInputConfig config = null;
		Iterable<GelfInputConfig> list = inputRepo.findAll();
		if (!IUtils.isNull(list) && list.iterator().hasNext()) {
			config = list.iterator().next();
			logger.info("Loading Gelf Input transport configuration from db config");
		} else {
			logger.info("Creating new transport into Gelf by api call.");
			String host = this.env.getProperty(IConstants.GULF_HOST);
			int port = Integer.parseInt(env.getProperty(IConstants.GULF_PORT));
			String id = createInputConfig(host, port);
			config = getAndSaveInputConfig(id);
		}
		return config;
	}

	/**
	 * Method to get and save gelf input config by its id.
	 * @param id
	 * @return
	 */
	private GelfInputConfig getAndSaveInputConfig(String id) {
		GelfInputConfig config = null;
		if (!IUtils.isNullOrEmpty(id)) {
			String url = String.format(IConstants.GET_GELF_INPUT_BY_ID,
					env.getProperty(IConstants.GULF_HOST), 
					env.getProperty(IConstants.GULF_PORT), id);
			try {
				Map<String, String> hdrs = IUtilities.getAuthHeader(env.getProperty(IConstants.GELF_USER), 
						env.getProperty(IConstants.GELF_PASS));
				String res = IUtils.sendGetRestReq(url, hdrs, null);
				logger.info("Res: " + res);
				if (!IUtils.isNullOrEmpty(res)) {
					config = IUtils.OBJECT_MAPPER.readValue(res, GelfInputConfig.class);
					if (!IUtils.isNull(config)) {
						config.setGelfId(id);
						GelfAttributes attr = config.getAttributes();
						attr.setBind_address(env.getProperty(IConstants.GULF_HOST));
						config.setAttributes(attr);
						config = inputRepo.save(config);
					}
				}
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		return config;
	}

	/**
	 * Method to create new input configuration into gelf by using post request.
	 * @param host
	 * @param port
	 * @return
	 */
	private String createInputConfig(String host, int port) {
		String id = null;
		String url = String.format(IConstants.POST_GELF_INPUT, host, port);
		try {
			Map<String, String> hdrs = IUtilities.getAuthHeader(env.getProperty(IConstants.GELF_USER), 
					env.getProperty(IConstants.GELF_PASS));
			hdrs.put(HttpHeaders.CONTENT_TYPE, "application/json");
			hdrs.put("X-Requested-By", "localhost");
			/*{"title":"Temp_input",
			 * "type":"com.synectiks.process.common.plugins.cef.input.CEFTCPInput",
			 * "configuration":{
			 * 	"bind_address":"0.0.0.0",
			 * 	"port":12202,
			 * 	"recv_buffer_size":1048576,
			 * 	"number_worker_threads":8,
			 * 	"tls_cert_file":"",
			 * 	"tls_key_file":"",
			 * 	"tls_enable":false,
			 * 	"tls_key_password":"",
			 * 	"tls_client_auth":"disabled",
			 * 	"tls_client_auth_cert_file":"",
			 * 	"tcp_keepalive":false,
			 * 	"use_null_delimiter":false,
			 * 	"max_message_size":2097152,
			 * 	"timezone":"America/New_York",
			 * 	"locale":"",
			 * 	"use_full_names":false},
			 * "global":false,
			 * "node":"0aa99cdd-bebc-4c94-a2c4-17b14e98c53e"}*/
			String reqObj = "{"
					+ "\"title\": \"Sync_Dyn_Rslt_Input\","
					+ "\"type\": \"com.synectiks.process.common.plugins.cef.input.CEFTCPInput\","
					+ "\"configuration\": {"
					+ "\"bind_address\": \"0.0.0.0\", \"port\": 12203, \"recv_buffer_size\": 1048576, "
					+ "\"number_worker_threads\": 8, \"tls_cert_file\": \"\", \"tls_key_file\": \"\", "
					+ "\"tls_enable\": false, \"tls_key_password\": \"\",\"tls_client_auth\": \"disabled\", "
					+ "\"tls_client_auth_cert_file\": \"\", \"tcp_keepalive\": false, "
					+ "\"use_null_delimiter\": false, \"max_message_size\": 2097152, "
					+ "\"timezone\": \"America/New_York\", \"locale\": \"\", \"use_full_names\": false}, "
					//+ "\"node\": \"\","
					+ "\"global\": false"
					+ "}";
			Object res = IUtils.sendPostJsonDataReq(url, hdrs, reqObj);
			logger.info("Res: " + res);
			if (!IUtils.isNull(res)) {
				id = IUtils.getJSONObject(res.toString()).optString("id");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return id;
	}

}
