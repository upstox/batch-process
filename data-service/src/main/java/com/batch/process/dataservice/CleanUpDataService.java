package com.batch.process.dataservice;

import java.util.Map;

/**
 * CleanUpDataService interface is used for cleaning Up Data
 * 
 * @param <T> Model for post partitioning work
 */
public interface CleanUpDataService<T> extends DataService<T> {

    /**
     * Perform any cleanup activity post completion or failure of job
     * 
     * @param stepIdentifier Job Step Identifier
     * @param parameterMap   Job parameters
     * @param contextMap     Job contextMap shared across data services
     */
    public void cleanUp(String stepIdentifier, Map<String, Object> parameterMap, ContextMap contextMap);
}
