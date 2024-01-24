package com.batch.process.manager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.listener.CompositeJobExecutionListener;
import org.springframework.batch.core.partition.support.RemoteStepExecutionAggregator;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import com.batch.process.common.BatchConstant;
import com.batch.process.dataservice.DataService;
import com.batch.process.dataservice.PartitionDataService;
import com.batch.process.dataservice.configuration.BatchJobConfiguration;
import com.batch.process.manager.jms.BatchJMSQueueManagementService;
import com.batch.process.manager.listener.JMSConsumerJobExecutionListener;
import com.batch.process.manager.listener.JobCleanupListener;
import com.batch.process.manager.listener.JobListener;
import com.batch.process.manager.listener.MDCManagerListener;

@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
public class GenericBatchJobConfigurator {
    private final JobBuilderFactory jobBuilderFactory;

    private final RemotePartitioningManagerStepBuilderFactory managerStepBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;

    private final JobExplorer jobExplorer;

    private final GenericApplicationContext context;

    private final GenericJobConfigurationManager genericJobConfigurationManager;

    private final JMSConsumerJobExecutionListener lockReleaserJobExecutionListener;

    public GenericBatchJobConfigurator(JobBuilderFactory jobBuilderFactory,
            RemotePartitioningManagerStepBuilderFactory managerStepBuilderFactory,
            StepBuilderFactory stepBuilderFactory, @Qualifier(BatchConstant.BEAN_JOB_EXPLORER) JobExplorer jobExplorer,
            GenericApplicationContext context, GenericJobConfigurationManager genericJobConfigurationManager,
            BatchJMSQueueManagementService batchJmsQueueManagementService) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.managerStepBuilderFactory = managerStepBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobExplorer = jobExplorer;
        this.context = context;
        this.genericJobConfigurationManager = genericJobConfigurationManager;
        this.lockReleaserJobExecutionListener = new JMSConsumerJobExecutionListener(batchJmsQueueManagementService);
    }

    @PostConstruct
    public void postConstruct() {
        createJobs();
    }

    public void createJobs() {
        Map<String, BatchJobConfiguration> jobTypeToJobConfigurationMap = genericJobConfigurationManager
                .getJobTypeToJobConfigurationMap();
        Set<String> keySet = jobTypeToJobConfigurationMap.keySet();

        for (String jobType : keySet) {
            BatchJobConfiguration jobConfiguration = jobTypeToJobConfigurationMap.get(jobType);
            JobBuilder jobBuilder = this.jobBuilderFactory.get(jobConfiguration.getJobType());
            Map<String, DataService<?>> dataLoaders = jobConfiguration.getDataServices();

            Set<Entry<String, DataService<?>>> entrySet = dataLoaders.entrySet();

            SimpleJobBuilder stepBuilder = null;
            Step managerStep = null;
            FlowBuilder<FlowJobBuilder> flowBuilder = null;

            for (Entry<String, DataService<?>> entry : entrySet) {
                String stepName = entry.getKey();
                DataService<?> dataService = entry.getValue();

                if (stepBuilder == null) {
                    stepBuilder = jobBuilder.start(createStep(stepName, dataService));
                } else {

                    if (dataService instanceof PartitionDataService) {
                        @SuppressWarnings("rawtypes")
                        PartitionDataService partitionDataService = (PartitionDataService) dataService;
                        managerStep = createManagerStep(partitionDataService);
                        stepBuilder.next(managerStep);
                    } else {

                        if (managerStep != null) {
                            flowBuilder = stepBuilder.on("FAILED")
                                    .to(createStep(stepName, jobConfiguration.getCleanUpService()))
                                    .from(managerStep)
                                    .on("COMPLETED")
                                    .to(createStep(stepName, dataService));
                            managerStep = null;
                        } else {
                            Step step = createStep(stepName, dataService);

                            if (flowBuilder != null) {
                                flowBuilder.next(step);
                            } else {
                                stepBuilder.next(step);
                            }
                        }
                    }
                }
            }
            Job job = flowBuilder != null ? flowBasedJob(jobConfiguration, flowBuilder)
                    : stepBasedJob(jobConfiguration, stepBuilder);
            genericJobConfigurationManager.registerJob(jobType, job);
            context.registerAlias(job.getName(), jobType);
        }
    }

    private Step createStep(String stepName, final DataService<?> dataService) {
        GenericTasklet genericTasklet = new GenericTasklet(dataService);
        return stepBuilderFactory.get(stepName)
                .tasklet(genericTasklet)
                .build();
    }

    @SuppressWarnings("rawtypes")
    private Step createManagerStep(PartitionDataService partitionDataService) {
        String partitionName = partitionDataService.getDataServiceName();
        GenericPartitioner genericPartitioner = new GenericPartitioner();

        return managerStepBuilderFactory.get(partitionName)
                .partitioner(partitionDataService.getWorkerstepName(), genericPartitioner)
                .outputChannel(partitionDataService.requestChannel())
                .inputChannel(partitionDataService.replyChannel())
                .listener(genericPartitioner)
                .aggregator(new RemoteStepExecutionAggregator(jobExplorer))
                .build();
    }

    private Job flowBasedJob(BatchJobConfiguration jobConfiguration, FlowBuilder<FlowJobBuilder> flowBuilder) {
        return flowBuilder.end()
                .listener(compositeJobExecutionListener(jobConfiguration))
                .build();
    }

    private Job stepBasedJob(BatchJobConfiguration jobConfiguration, SimpleJobBuilder stepBuilder) {
        return stepBuilder.listener(compositeJobExecutionListener(jobConfiguration))
                .build();
    }

    private JobExecutionListener compositeJobExecutionListener(BatchJobConfiguration jobConfiguration) {
        CompositeJobExecutionListener compositeJobExecutionListener = new CompositeJobExecutionListener();
        compositeJobExecutionListener.register(new MDCManagerListener());
        compositeJobExecutionListener.register(new JobListener(jobConfiguration));
        compositeJobExecutionListener.register(new JobCleanupListener(jobConfiguration));
        compositeJobExecutionListener.register(lockReleaserJobExecutionListener);
        return compositeJobExecutionListener;
    }

}
