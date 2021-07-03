/**
 * 
 */
package com.synectiks.policy.runner;


import java.net.InetSocketAddress;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.synectiks.policy.runner.utils.IConstants;

/**
 * @author Rajesh
 *
 */
@Configuration
public class GelfMessageConfiguration {

	@Autowired
	private Environment env;
	
	@Bean
	public GelfConfiguration getGelfConfiguration() {
		String host = this.env.getProperty(IConstants.GULF_HOST);
		int port = Integer.parseInt(env.getProperty(IConstants.GULF_PORT));
		
		return new GelfConfiguration(new InetSocketAddress(host, port))
        .transport(GelfTransports.TCP)
        .queueSize(512)
        .connectTimeout(5000)
        .reconnectDelay(1000)
        .tcpNoDelay(true)
        .sendBufferSize(32768);
	}
	
	@Bean
	public GelfTransport getGelfTransport() {
		return GelfTransports.create(getGelfConfiguration());
	}

}
