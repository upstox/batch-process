package com.batch.process.manager.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import com.batch.process.common.BatchConstant;
import com.batch.process.manager.jms.BatchJMSQueueManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JMSConsumerJobExecutionListener implements JobExecutionListener {

    private final BatchJMSQueueManagementService batchJobConcurrencyManager;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Jms queue job listener before job invoked");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Resuming jms message consumer listener");
        batchJobConcurrencyManager.resumeMessageListener(BatchConstant.QUEUE_JOB_INPUT_REQUEST_ID);
        log.info("Resumed jms message consumer listener");
    }

}
