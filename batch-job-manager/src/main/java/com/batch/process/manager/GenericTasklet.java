package com.batch.process.manager;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.batch.process.dataservice.DataService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericTasklet implements Tasklet {

    private final DataService<?> dataService;

    public GenericTasklet(final DataService<?> dataService) {
        this.dataService = dataService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        JobExecution jobExecution = contribution.getStepExecution()
                .getJobExecution();
        StepExecution stepExecution = contribution.getStepExecution();
        Long stepId = stepExecution.getId();
        String jobName = jobExecution.getJobInstance()
                .getJobName();
        Long jobId = jobExecution.getJobId();
        String dataServiceName = dataService.getDataServiceName();

        try {
            log.info("Step Execution Started | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}]", jobName, jobId,
                    dataServiceName, stepId);
            StepExecutor.execute(jobExecution, dataService);
            long startTime = stepExecution.getStartTime()
                    .getTime();
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            log.info("Step Execution Completed | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] timeTaken:[{}] ms",
                    jobName, jobId, dataServiceName, stepId, timeTaken);
        } catch (Exception e) {
            log.info("Exception occurred while executing step | jobName:[{}] jobId:[{}] stepName:[{}] stepId:[{}] ",
                    jobName, jobId, dataServiceName, stepId);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

}
