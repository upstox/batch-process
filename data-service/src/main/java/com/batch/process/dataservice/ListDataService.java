package com.batch.process.dataservice;

import java.util.List;
import java.util.Map;

public interface ListDataService<D> extends DataService<D> {

	/**
	 * Any list data being fetched from a data source.
	 * 
	 * @param jobId         The job id
	 * @param parametersMap The parameters passed to the job
	 * @param contextMap    Any information that passed from previous step
	 * @return The list of D type records
	 */
	public List<D> getData(String jobId, Map<String, Object> parametersMap, ContextMap contextMap);

	/**
	 * Any list data to be stored at a location for processing.
	 * 
	 * @param jobId         The job id
	 * @param parametersMap The parameters passed to the job
	 * @param contextMap    Any information that passed from previous step
	 * @param contextMap
	 */
	public void storeData(String jobId, List<D> data, Map<String, Object> parametersMap, ContextMap contextMap);

}
