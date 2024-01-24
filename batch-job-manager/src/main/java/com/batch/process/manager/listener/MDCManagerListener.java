package com.batch.process.manager.listener;

import static com.batch.process.common.BatchConstant.JOB_REQUEST_ID;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class MDCManagerListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        Object requestId = jobExecution.getJobParameters()
                .getParameters()
                .get(JOB_REQUEST_ID);

        if (Objects.nonNull(requestId)) {
            MDC.put(JOB_REQUEST_ID, requestId.toString());
        }
        MDC.put("jobId", String.valueOf(jobExecution.getJobId()));

    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        MDC.remove("jobId");
        String jobRequestId = MDC.get(JOB_REQUEST_ID);

        if (StringUtils.isNotEmpty(jobRequestId)) {
            MDC.remove(JOB_REQUEST_ID);
        }
    }
}
