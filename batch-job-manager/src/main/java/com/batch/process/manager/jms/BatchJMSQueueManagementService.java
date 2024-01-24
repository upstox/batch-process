package com.batch.process.manager.jms;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import com.batch.process.manager.exception.BatchJobProcessJMSManagementException;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code JMSQueueManagementService} is a service to manage JMS Queues
 * 
 * @author SamratChakraborty
 */

@Service
@Slf4j
public class BatchJMSQueueManagementService {

    @Autowired
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    /**
     * Pauses Message receiving for this particular {@link MessageListenerContainer}
     * with the id below
     * 
     * @param id Id for the Message Listener, see {@link JmsListener#id()}
     * @return !{@link MessageListenerContainer#isRunning()}
     */
    public boolean pauseMessageListener(String id) {
        log.info("Halting Message Listener for id : {}", id);
        MessageListenerContainer messageListenerContainer = getMessageListenerContainerById(id);
        messageListenerContainer.stop();
        log.info("Message Listener Container with id : {} stopped", id);
        return !messageListenerContainer.isRunning();
    }

    /**
     * Starts receiving Message for this particular {@link MessageListenerContainer}
     * with the id below
     * 
     * @param id Id for the Message Listener, see {@link JmsListener#id()}
     * @return {@link MessageListenerContainer#isRunning()}
     */
    public boolean resumeMessageListener(String id) {
        log.info("Restarting Message Listener for id : {}", id);
        MessageListenerContainer messageListenerContainer = getMessageListenerContainerById(id);
        messageListenerContainer.start();
        log.info("Message Listener Container with id : {} started", id);
        return messageListenerContainer.isRunning();
    }

    private MessageListenerContainer getMessageListenerContainerById(String id) {
        MessageListenerContainer messageListenerContainer = jmsListenerEndpointRegistry.getListenerContainer(id);

        if (Objects.isNull(messageListenerContainer)) {
            log.warn("No Message Listener Container found with id : {}", id);
            throw new BatchJobProcessJMSManagementException("No such message listener container for id: " + id);
        }
        return messageListenerContainer;
    }

}
