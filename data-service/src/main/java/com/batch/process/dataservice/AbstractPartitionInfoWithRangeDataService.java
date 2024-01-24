package com.batch.process.dataservice;

import java.util.Map;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.RangeInfo;

/**
 * A partition info provider that helps partitioning by an Id rather than SQL
 * offset or limit. It is observed that MySQl deteriorates in performance with
 * large limit based SQL query execution. Hence, this abstraction mitigates the
 * problem by using the primary key of the table as the partitioner.
 * 
 */
public abstract class AbstractPartitionInfoWithRangeDataService extends AbstractPartitionInfoDataService {

	/**
	 * Return the full range of data based on primary key IDs in consideration for
	 * partitioning.
	 * 
	 * @param jobId        The jobId
	 * @param parameterMap The parameters passed in the Job launch
	 * @param contextMap   The context data available during the course of execution
	 * @return RangeInfo that captures the entire range of primary key to be
	 *         partitioned based on batch count configured
	 */
	protected abstract RangeInfo getRangeInfo(String jobId, Map<String, Object> parameterMap, ContextMap contextMap);

	@Override
	protected long getTotalCount(String jobId, Map<String, Object> parameterMap, ContextMap contextMap) {
		RangeInfo rangeInfo = getRangeInfo(jobId, parameterMap, contextMap);
		contextMap.put(BatchConstant.RANGE_INFO, rangeInfo);
		return (rangeInfo.getMaxId() - rangeInfo.getMinId()) + 1;
	}

}
