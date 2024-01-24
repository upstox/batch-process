package com.batch.process.dataservice.configuration;

import java.util.concurrent.CopyOnWriteArrayList;

import com.batch.process.listener.BatchJobListener;

public abstract class AbstractBatchJobConfiguration implements BatchJobConfiguration {
    private final CopyOnWriteArrayList<BatchJobListener> batchJobListeners = new CopyOnWriteArrayList<BatchJobListener>();

    @Override
    public void addBatchJobListener(BatchJobListener batchJobListener) {
        batchJobListeners.add(batchJobListener);
    }

    @Override
    public void removeBatchJobListener(BatchJobListener batchJobListener) {
        batchJobListeners.remove(batchJobListener);
    }

    @Override
    public CopyOnWriteArrayList<BatchJobListener> getBatchJobListeners() {
        return batchJobListeners;
    }
}
