package com.batch.process.dataservice.configuration;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import com.batch.process.common.validation.model.Problem;
import com.batch.process.dataservice.DataService;
import com.batch.process.listener.BatchJobListener;

/**
 * Job Configuration that describes the data services needed to generate the
 * job. It is used to construct the Spring Batch Job during the booting time.
 * The job definition created is mapped with the job configuration type name and
 * that job is what is instantiated when the job generation request is received.
 * 
 * @author Krishna Murthy P Mirajkar
 */
public interface BatchJobConfiguration {
    /**
     * The job type name this configuration is meant for.
     * 
     * @return The job name
     */
    String getJobType();

    /**
     * The data services that belong to this job configuration
     * 
     * @return The map of data service name to respective data service
     *         implementation
     */
    Map<String, DataService<?>> getDataServices();

    /**
     * The clean-up service to be used on error or on job completion
     * 
     * @return
     */
    DataService<?> getCleanUpService();

    /**
     * Validate the input parameters that are specific to this configuration
     * 
     * @param properties The pay-load converted into properties
     * @return {@link List<Problem>} The problems in the pay-load
     */
    List<Problem> validate(Properties properties);

    /**
     * Add a BatchJobListener to be notified for job events
     * 
     * @param batchJobListener The listener
     */
    void addBatchJobListener(BatchJobListener batchJobListener);

    /**
     * Remove a BatchJobListener to be notified for job events
     * 
     * @param batchJobListener The listener
     */
    void removeBatchJobListener(BatchJobListener batchJobListener);

    /**
     * Get all the Job listeners
     * 
     * @return All registered listeners
     */
    CopyOnWriteArrayList<BatchJobListener> getBatchJobListeners();
}
