package com.batch.process.dataservice;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;

public interface PartitionDataService<T> extends DataService<T> {
	String OUT_FLOW = "outFlow";
	String IN_FLOW = "inFlow";

	/**
	 * The request channel to send partition info to worker
	 * 
	 * @return The {@link DirectChannel}
	 */
	DirectChannel requestChannel();

	/**
	 * The reply channel to receive partition work info from the worker
	 * 
	 * @return The {@link DirectChannel}
	 */
	DirectChannel replyChannel();

	IntegrationFlow outboundFlow(DirectChannel channel, ActiveMQConnectionFactory connectionFactory);

	IntegrationFlow inboundFlow(DirectChannel channel, ActiveMQConnectionFactory connectionFactory);

	String getWorkerstepName();
}
