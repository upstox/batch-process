package com.batch.process.dataservice;

import java.util.Map;

public interface SingleDataService<D> extends DataService<D> {

	/**
	 * Any atomic data being fetched from a data source.<br>
	 * Note: Can be tweaked to put any kind of D type
	 * 
	 * @param jobId         The job id
	 * @param parametersMap The parameters passed to the job
	 * @param contextMap    Any information that passed from previous step
	 * @return The D object
	 */
	D getData(String jobId, Map<String, Object> parameterMap, ContextMap contextMap);

	/**
	 * Any data to be stored at a location for processing.
	 * 
	 * @param jobId         The job id
	 * @param parametersMap The parameters passed to the job
	 * @param contextMap    Any information that passed from previous step
	 */
	void storeData(String string, D data, Map<String, Object> parameterMap, ContextMap contextMap);

}
