package com.batch.process.message.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.batch.process.common.BatchConstant;

@Configuration
@PropertySource("classpath:application.properties")
public class ActiveMQConfiguration {

    @Value("${batchframework.activemq.broker-url}")
    private String brokerUrl;

    @Value("${batchframework.activemq.user}")
    private String user;

    @Value("${batchframework.activemq.password}")
    private String password;

    @Bean(BatchConstant.BEAN_ACTIVE_MQ_CONNECTION)
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(this.brokerUrl);
        connectionFactory.setUserName(user);
        connectionFactory.setPassword(password);
        connectionFactory.setTrustAllPackages(true);
        return connectionFactory;
    }

}
