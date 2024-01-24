package com.batch.process.manager.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import com.batch.process.dataservice.DataService;
import com.batch.process.dataservice.configuration.BatchJobConfiguration;
import com.batch.process.manager.StepExecutor;

public class JobCleanupListener implements JobExecutionListener {
	private final BatchJobConfiguration batchJobConfiguration;

	public JobCleanupListener(BatchJobConfiguration batchJobConfiguration) {
		this.batchJobConfiguration = batchJobConfiguration;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		// No Op
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		DataService<?> cleanUpService = batchJobConfiguration.getCleanUpService();

		if (cleanUpService != null) {
			StepExecutor.execute(jobExecution, cleanUpService);
		}
	}

}
