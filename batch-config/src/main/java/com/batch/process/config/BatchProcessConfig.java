package com.batch.process.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.batch.process.common.BatchConstant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class BatchProcessConfig {

    @Bean(BatchConstant.BEAN_EXECUTOR_POOL)
    public TaskExecutor batchProcessTaskExecutor() {
        log.info("Initializing batchExecutorPool");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("Job-");
        executor.initialize();
        return executor;
    }

    @Bean(BatchConstant.BEAN_JOB_LAUNCHER)
    @DependsOn(value = { BatchConstant.BEAN_EXECUTOR_POOL })
    public JobLauncher batchProcessJobLauncher(JobRepository jobRepository,
            @Autowired @Qualifier(BatchConstant.BEAN_EXECUTOR_POOL) TaskExecutor taskExecutor) {
        log.info("Initializing backOfficeJobLauncher");
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        log.info("Thread Pool {}", taskExecutor);
        jobLauncher.setTaskExecutor(taskExecutor);
        return jobLauncher;
    }

    @Bean
    public SimpleJobOperator batchProcessJobOperator(JobExplorer jobExplorer, JobRepository jobRepository,
            JobRegistry jobRegistry, JobLauncher jobLauncher) {
        log.info("Initializing jobOperator");
        SimpleJobOperator jobOperator = new SimpleJobOperator();
        jobOperator.setJobExplorer(jobExplorer);
        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.setJobLauncher(jobLauncher);
        return jobOperator;
    }

    @Bean
    @Qualifier(BatchConstant.BEAN_JDBC_TEMPLATE)
    public JdbcTemplate batchProcessJdbcTemplate(DataSource batchDataSource) {
        return new JdbcTemplate(batchDataSource);
    }

    @Bean(BatchConstant.BEAN_JOB_EXPLORER)
    public JobExplorer batchProcessJobExplorer(DataSource batchDataSource,
            @Qualifier(BatchConstant.BEAN_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) throws Exception {
        log.info("Initializing jobExplorer");
        JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
        factoryBean.setDataSource(batchDataSource);
        factoryBean.setJdbcOperations(jdbcTemplate);
        factoryBean.setSerializer(new Jackson2ExecutionContextStringSerializer());
        return factoryBean.getObject();
    }
}
