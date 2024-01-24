package com.batch.process.listener;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.batch.process.dataservice.ContextMap;
import com.batch.process.dataservice.JobStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchJobInfo {
    private Long jobId;
    private Date startTime;
    private Date endTime;
    private Date lastUpdated;
    private JobStatus status;
    private String exitStatus;
    private String errorDescription;
    private List<Throwable> failureExceptions;
    private List<Throwable> allFailureExceptions;
    private Map<String, Object> jobParameters;
    private ContextMap contextMap;
}
