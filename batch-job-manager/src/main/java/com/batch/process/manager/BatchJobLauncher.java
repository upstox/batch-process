package com.batch.process.manager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.utils.MapToPropertiesConverter;
import com.batch.process.common.validation.model.Problem;
import com.batch.process.dataservice.configuration.BatchJobConfiguration;
import com.batch.process.manager.exception.BatchJobInitiationException;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

/**
 * The batch job launcher. This encapsulates the job launching mechanism.
 * 
 * @author Krishna Murthy P Mirajkar
 */
@Component
@Slf4j
public class BatchJobLauncher {

    @Autowired
    @Qualifier(BatchConstant.BEAN_JOB_LAUNCHER)
    private JobLauncher jobLauncher;

    @Autowired
    private GenericJobConfigurationManager genericJobConfigurationManager;

    /**
     * Launches the job with the request payload received from the job input queue
     * 
     * @param jobRequest Map payload which has a mandatory key
     *                   {@link BatchConstant}.KEY_REQUEST_JOB_TYPE
     * @return Job id of the job launched
     * @throws BatchJobInitiationException On any issue encountered prior to
     *                                     launching the job
     */
    public void runJob(Map<String, Object> jobRequest) throws BatchJobInitiationException {

        try {
            Properties properties = MapToPropertiesConverter.getRequestBodyProperties(jobRequest);

            String jobType = properties.getProperty(BatchConstant.KEY_REQUEST_JOB_TYPE);
            BatchJobConfiguration batchJobConfiguration = genericJobConfigurationManager
                    .getBatchJobConfiguration(jobType);

            if (Objects.isNull(batchJobConfiguration)) {
                String msg = "Unknown batch job configuration";
                log.error(msg + "| jobType:[{}]", jobType);
                throw new BatchJobInitiationException(msg + "| jobType:[" + jobType + "]");
            }

            List<Problem> problems = batchJobConfiguration.validate(properties);

            if (CollectionUtils.isNotEmpty(problems)) {
                String validationMessage = "Validation failed for job request";
                log.error(validationMessage + " | problems:[{}]", problems);
                throw new BatchJobInitiationException(validationMessage + "| jobType:[" + jobType + "]");
            }

            JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
            jobParametersBuilder.addLong("launchTime", System.currentTimeMillis());
            properties.forEach((k, v) -> jobParametersBuilder.addString(k.toString(), v.toString()));

            Job job = genericJobConfigurationManager.getJob(jobType);
            String jobName = job.getName();
            JobExecution jobExecution = jobLauncher.run(job, jobParametersBuilder.toJobParameters());
            log.info("Job launched | jobName:[{}] jobId:[{}]", jobName, jobExecution.getJobId());

        } catch (JsonProcessingException | JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("Exception occurred while launching job from job input queue", e);
            throw new BatchJobInitiationException(e.getMessage(), e);
        }
    }
}
