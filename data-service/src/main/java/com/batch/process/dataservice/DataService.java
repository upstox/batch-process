package com.batch.process.dataservice;

public interface DataService<D> {

	/**
	 * The name of the data service. Used to identify the step
	 * 
	 * @return The step name
	 */
	public String getDataServiceName();

}
