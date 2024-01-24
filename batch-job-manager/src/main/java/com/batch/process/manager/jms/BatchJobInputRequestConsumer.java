package com.batch.process.manager.jms;

import com.batch.process.common.BatchConstant;
import com.batch.process.manager.BatchJobLauncher;
import com.batch.process.manager.exception.BatchJobInitiationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The Job input queue consumer.
 * 
 * @author Krishna Murthy P Mirajkar
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobInputRequestConsumer {

    private final BatchJobLauncher batchJobLauncher;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BatchJMSQueueManagementService batchJmsQueueManagementService;

    /**
     * The job input queue used for managing concurrency in the batch process
     * framework.
     * 
     * @param jobInputRequestMap The request received as a map that has mandatory
     *                           key
     *                           <b>{@link BatchConstant}.KEY_REQUEST_JOB_TYPE</b>
     *                           to identify the job to look up
     * @throws Exception
     */
    @JmsListener(id = BatchConstant.QUEUE_JOB_INPUT_REQUEST_ID, destination = "${batch.framework.queue.unique.name}", concurrency = "1-1")
    public void consumeMessage(Map<String, Object> jobInputRequestMap) throws Exception {

        try {
            log.info("New message received | message:[{}]", MAPPER.writeValueAsString(jobInputRequestMap));
        } catch (JsonProcessingException e) {
            log.error("Exception occurred in the input job queue while deserializing. Job request rejected.", e);
            return;
        }

        try {
            log.info("Launching Job as acquired lock to run the job.");
            batchJobLauncher.runJob(jobInputRequestMap);
            log.info("Batch job launched.");

            log.info("About to pause job queue consumer listener | queueId:[{}]",
                    BatchConstant.QUEUE_JOB_INPUT_REQUEST_ID);
            batchJmsQueueManagementService.pauseMessageListener(BatchConstant.QUEUE_JOB_INPUT_REQUEST_ID);
            log.info("Job queue consumer listener paused.| queueId:[{}]", BatchConstant.QUEUE_JOB_INPUT_REQUEST_ID);

        } catch (BatchJobInitiationException e) {

            log.error("Exception occurred while launching job. Job queue consumer listener resumed", e);
            batchJmsQueueManagementService.resumeMessageListener(BatchConstant.QUEUE_JOB_INPUT_REQUEST_ID);
            log.info("Resumed job listener successfully after encountering exception while launching job");
        }
    }
}
