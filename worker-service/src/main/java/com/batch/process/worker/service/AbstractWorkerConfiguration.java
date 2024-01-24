package com.batch.process.worker.service;

import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;

public abstract class AbstractWorkerConfiguration implements WorkerConfiguration {
    /*
     * WorkerStepListener is kept as static because same copy will be used across
     * all the objects
     */
    private static final WorkerStepListener workerStepListener = new WorkerStepListener();
    protected final RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;

    protected AbstractWorkerConfiguration(RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory) {
        this.workerStepBuilderFactory = workerStepBuilderFactory;
    }

    protected WorkerStepListener getWorkerStepListener() {
        return workerStepListener;
    }
}
