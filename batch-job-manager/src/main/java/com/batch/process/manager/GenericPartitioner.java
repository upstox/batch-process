package com.batch.process.manager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.PartitionInfo;
import com.batch.process.dataservice.ContextMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericPartitioner implements Partitioner, StepExecutionListener {

    private StepExecution stepExecution;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        JobParameters jobParameters = jobExecution.getJobParameters();
        Map<String, JobParameter> parameters = jobParameters.getParameters();
        Map<String, Object> parameterMap = new HashMap<>();

        ExecutionContext executionContext = jobExecution.getExecutionContext();

        // Update the contextMap with info to be propagated to next step
        ContextMap contextMap = new ContextMap();
        log.info("Step Context map parameter -");
        executionContext.entrySet()
                .forEach(e -> {
                    String key = e.getKey();
                    contextMap.put(key, e.getValue());
                    log.info("\t\t" + (String.format("%-50s", key)) + " -> {}", contextMap.get(key)
                            .toString());
                });

        // Pass the job parameters to each data service
        Set<Entry<String, JobParameter>> entrySet = parameters.entrySet();

        for (Entry<String, JobParameter> entry : entrySet) {
            parameterMap.put(entry.getKey(), entry.getValue());
        }

        Map<String, String> map = partition(contextMap);
        Map<String, ExecutionContext> executionContextMap = new LinkedHashMap<>();
        map.forEach((k, v) -> executionContextMap.put(k, new ExecutionContext(Map.of("file", k))));
        return executionContextMap;
    }

    @Override
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        JobExecution jobExecution = stepExecution.getJobExecution();
        String jobName = jobExecution.getJobInstance()
                .getJobName();
        Long jobId = jobExecution.getJobId();
        Long stepId = stepExecution.getId();
        String stepName = stepExecution.getStepName();
        log.info("Partition Execution Started | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] ", jobName, jobId,
                stepName, stepId);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution()
                .getJobInstance()
                .getJobName();
        Long jobId = stepExecution.getJobExecution()
                .getJobId();
        ExitStatus exitStatus = stepExecution.getExitStatus();
        Long stepId = stepExecution.getId();
        long startTime = stepExecution.getStartTime().getTime();
        String stepName = stepExecution.getStepName();
        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;

        if (ExitStatus.FAILED.getExitCode()
                .equals(exitStatus.getExitCode())) {
            log.error("Worker returned with status = {}. Invoking clean-up service", exitStatus.getExitCode());
            JobExecution jobExecution = stepExecution.getJobExecution();
            jobExecution.stop();
            log.info(
                    "Partition Execution Failed | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] timeTaken:[{}] ms exitStatus:[{}]",
                    jobName, jobId, stepName, stepId, timeTaken, exitStatus.getExitCode());
        } else {
            log.info(
                    "Partition Execution Completed | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] timeTaken:[{}] ms exitStatus:[{}]",
                    jobName, jobId, stepName, stepId, timeTaken, exitStatus.getExitCode());
        }

        return exitStatus;
    }

    private Map<String, String> partition(ContextMap contextMap) {
        Map<String, String> partitionMap = new LinkedHashMap<>();
        PartitionInfo partitionInfo = contextMap.get(BatchConstant.PARTITION_COUNT);
        int partitionCount = partitionInfo.getPartitionCount();

        for (int i = 0; i < partitionCount; i++) {
            partitionMap.put("partition_" + i, String.valueOf(i));
        }
        return partitionMap;
    }
}
