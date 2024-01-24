package com.batch.process.worker.service;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;

public interface WorkerConfiguration {
    public String getJobType();

    public String getFileType();

    public String workerStepName();

    public DirectChannel getInputChannel();

    public DirectChannel getOutputChannel();

    public IntegrationFlow getInFlow(DirectChannel inputChannel);

    public IntegrationFlow getOutFlow(DirectChannel outputChannel);

    public Step targetWorkerBean(DirectChannel inputChannel, DirectChannel outputChannel);

    Tasklet tasklet(String partition);
}
