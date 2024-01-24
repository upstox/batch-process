package com.batch.process.listener;

import java.util.EventListener;

public interface BatchJobListener extends EventListener {
    public void afterJob(JobEvent jobEvent);

    public void beforeJob(JobEvent jobEvent);
}
