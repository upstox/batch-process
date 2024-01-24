package com.batch.process.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.Job;
import org.springframework.stereotype.Component;

import com.batch.process.dataservice.configuration.BatchJobConfiguration;

@Component
public class GenericJobConfigurationManager {

    private final Map<String, BatchJobConfiguration> jobTypeToJobConfigurationMap = new HashMap<>();

    private final List<BatchJobConfiguration> batchConfigurations;

    private final Map<String, Job> jobTypeToJobMap = new HashMap<>();

    public GenericJobConfigurationManager(List<BatchJobConfiguration> jobConfigurations) {
        this.batchConfigurations = jobConfigurations;
    }

    @PostConstruct
    public void postConstruct() {

        for (BatchJobConfiguration jobConfiguration : batchConfigurations) {
            jobTypeToJobConfigurationMap.put(jobConfiguration.getJobType(), jobConfiguration);
        }
    }

    public BatchJobConfiguration getBatchJobConfiguration(String jobType) {
        return jobTypeToJobConfigurationMap.get(jobType);
    }

    public Map<String, BatchJobConfiguration> getJobTypeToJobConfigurationMap() {
        return Collections.unmodifiableMap(jobTypeToJobConfigurationMap);
    }

    public void registerJob(String jobType, Job job) {
        jobTypeToJobMap.put(jobType, job);
    }

    public Job getJob(String jobType) {
        return jobTypeToJobMap.get(jobType);
    }
}
