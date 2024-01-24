package com.batch.process.manager.listener;

import static com.batch.process.common.BatchConstant.MANAGER_TIME;
import static com.batch.process.common.BatchConstant.TOTAL_JOB_TIME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;

import com.batch.process.dataservice.ContextMap;
import com.batch.process.dataservice.JobStatus;
import com.batch.process.dataservice.configuration.BatchJobConfiguration;
import com.batch.process.listener.BatchJobInfo;
import com.batch.process.listener.BatchJobListener;
import com.batch.process.listener.JobEvent;

@Slf4j
public class JobListener implements JobExecutionListener {
    private final BatchJobConfiguration batchJobConfiguration;

    public JobListener(BatchJobConfiguration batchJobConfiguration) {
        this.batchJobConfiguration = batchJobConfiguration;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        List<BatchJobListener> batchJobListeners = batchJobConfiguration.getBatchJobListeners();
        JobEvent jobEvent = prepareJobEvent(jobExecution);
        String jobName = jobExecution.getJobInstance()
                .getJobName();
        Long jobId = jobExecution.getJobInstance()
                .getId();
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        executionContext.put(MANAGER_TIME, 0L);
        executionContext.put(TOTAL_JOB_TIME, System.currentTimeMillis());
        log.info("Job Execution Started | jobName:[{}] jobId:[{}]", jobName, jobId);

        if (CollectionUtils.isNotEmpty(batchJobListeners)) {

            for (BatchJobListener batchJobListener : batchJobListeners) {

                try {
                    batchJobListener.beforeJob(jobEvent);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        List<BatchJobListener> batchJobListeners = batchJobConfiguration.getBatchJobListeners();
        JobEvent jobEvent = prepareJobEvent(jobExecution);
        String jobName = jobExecution.getJobInstance()
                .getJobName();
        Long jobId = jobExecution.getJobInstance()
                .getId();
        String exitCode = jobExecution.getExitStatus()
                .getExitCode();

        if (CollectionUtils.isNotEmpty(batchJobListeners)) {

            for (BatchJobListener batchJobListener : batchJobListeners) {

                try {
                    batchJobListener.afterJob(jobEvent);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        Object timeObject = jobExecution.getExecutionContext()
                .get(MANAGER_TIME);
        Object totalJobTime = jobExecution.getExecutionContext()
                .get(TOTAL_JOB_TIME);

        if (Objects.nonNull(timeObject) && Objects.nonNull(totalJobTime)) {
            long launchTime = Long.parseLong(totalJobTime.toString());
            long jobCompletionTime = System.currentTimeMillis() - launchTime;
            long managerTime = Long.parseLong(timeObject.toString());
            log.info(
                    "Job Time Details | jobName:[{}] jobId:[{}] jobCompletionTime:[{}] ms managerTime:[{}] ms exitStatus:[{}]",
                    jobName, jobId, jobCompletionTime, managerTime, exitCode);
        }
    }

    private static JobEvent prepareJobEvent(JobExecution jobExecution) {
        Map<String, Object> parameterMap = getJobParameters(jobExecution);
        ContextMap contextMap = getContextMap(jobExecution);
        BatchJobInfo batchJobInfo = BatchJobInfo.builder()
                .jobId(jobExecution.getJobId())
                .contextMap(contextMap)
                .jobParameters(parameterMap)
                .endTime(jobExecution.getEndTime())
                .startTime(jobExecution.getStartTime())
                .lastUpdated(jobExecution.getLastUpdated())
                .status(JobStatus.valueOf(jobExecution.getStatus()
                        .name()))
                .exitStatus(jobExecution.getExitStatus()
                        .toString())
                .errorDescription(populateErrorDescription(jobExecution))
                .failureExceptions(jobExecution.getFailureExceptions())
                .allFailureExceptions(jobExecution.getAllFailureExceptions())
                .build();
        return new JobEvent(batchJobInfo);
    }

    private static String populateErrorDescription(JobExecution jobExecution) {
        String errorDesc = "";
        List<Throwable> failureException = jobExecution.getAllFailureExceptions();

        if (CollectionUtils.isEmpty(failureException)) {
            return errorDesc;
        }
        StringBuilder sb = new StringBuilder();

        int exceptionListSize = failureException.size();

        for (int i = 0; i < exceptionListSize; i++) {
            Throwable exception = failureException.get(i);
            sb.append(exception.getMessage());

            if (i == exceptionListSize - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static ContextMap getContextMap(JobExecution jobExecution) {
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        // Update the contextMap with info to be propagated to next step
        ContextMap contextMap = new ContextMap();
        executionContext.entrySet()
                .forEach(e -> {
                    String key = e.getKey();
                    contextMap.put(key, e.getValue());
                });
        return contextMap;
    }

    private static Map<String, Object> getJobParameters(JobExecution jobExecution) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        Map<String, JobParameter> parameters = jobParameters.getParameters();
        Map<String, Object> parameterMap = new HashMap<>();

        // Pass the job parameters to each data service
        for (String parameterKey : parameters.keySet()) {
            parameterMap.put(parameterKey, parameters.get(parameterKey));
        }
        return parameterMap;
    }
}
