package com.batch.process.worker.service;

import static com.batch.process.common.BatchConstant.FILE;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerStepListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution()
                .getJobInstance()
                .getJobName();
        Long jobId = stepExecution.getJobExecution()
                .getJobId();
        Long stepId = stepExecution.getId();
        String stepName = stepExecution.getStepName();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        String partition = StringUtils.EMPTY;

        if (Objects.nonNull(executionContext.get(FILE))) {
            partition = executionContext.get(FILE)
                    .toString();
        }
        log.info("Worker Execution Started | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] partition:[{}] ",
                jobName, jobId, stepName, stepId, partition);

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution()
                .getJobInstance()
                .getJobName();
        Long jobId = stepExecution.getJobExecution()
                .getJobId();
        Long stepId = stepExecution.getId();
        String stepName = stepExecution.getStepName();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        String partition = StringUtils.EMPTY;

        if (Objects.nonNull(executionContext.get(FILE))) {
            partition = executionContext.get(FILE)
                    .toString();
        }
        long startTime = stepExecution.getStartTime()
                .getTime();
        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        log.info(
                "Worker Execution Completed | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] partition:[{}] timeTaken:[{}] ms exitStatus:[{}]",
                jobName, jobId, stepName, stepId, partition, timeTaken, stepExecution.getExitStatus()
                        .getExitCode());

        return stepExecution.getExitStatus();
    }
}
