package com.batch.process.dataservice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.jms.IllegalStateException;

import org.apache.commons.collections4.CollectionUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.PartitionInfo;
import com.batch.process.common.PostPartitionData;

/**
 * AbstractCountBasedPartitionedListDataService class is used to create count
 * based partitions and provides option to do any per post partition processing
 * and persisting it
 * 
 * @param <D> Model for which partition needs to be done
 * @param <T> Model for post partitioning work
 */
@Slf4j
public abstract class AbstractCountBasedPartitionListDataService<D, T extends PostPartitionData>
		implements ListDataService<D> {

	/**
	 * The folder name where the partitions will be saved
	 * 
	 * @return The folder name where partitions will be saved
	 */
	protected abstract String getDataFolderName();

	/**
	 * The Template Query for creating partitions
	 * 
	 * @param jobId         - Job Id
	 * @param parametersMap - Job parameters
	 * @param contextMap    - Job contextMap shared across data services
	 * @return The partition template query
	 */
	protected abstract String getDataQuery(String jobId, Map<String, Object> parametersMap, ContextMap contextMap);

	/**
	 * The Data Model List for creating File
	 * 
	 * @param jobId         - Job Id
	 * @param query         - Template query
	 * @param parametersMap - Job parameters
	 * @param contextMap    - Job contextMap shared across data services
	 * @return The list of data models
	 */
	protected abstract List<D> getDataModelList(String jobId, String query, Map<String, Object> parametersMap,
			ContextMap contextMap);

	/**
	 * Process Partition Data
	 *
	 * @param jobId         - Job Id
	 * @param parametersMap - Job parameters
	 * @param contextMap    - Job contextMap shared across data services
	 * @param partitionName - Partition Name
	 * @param dataModelList - Data Model List
	 * @return Processed Partition data if processing is required or else null
	 */
	protected abstract T processPartitionData(String jobId, Map<String, Object> parametersMap, ContextMap contextMap,
			String partitionName, List<D> dataModelList);

	/**
	 * Store Partition Data
	 * 
	 * @param jobId          - Job Id
	 * @param folderLocation - Folder Location where data needs to be stored
	 * @param partitionName  - Partition Name
	 * @param dataModelList  - Data Model List
	 */
	protected abstract void storePartitionedData(String jobId, String folderLocation, String partitionName,
			List<D> dataModelList);

	/**
	 * Save Post Partitioned Processed Data
	 * 
	 * @param jobId                    - Job Id
	 * @param parametersMap            - Job parameters
	 * @param contextMap               - Job contextMap shared across data services
	 * @param postPartitionProcessData - Post Partitioned Process Data List
	 */
	protected abstract void savePostPartitionProcessData(String jobId, Map<String, Object> parametersMap,
			ContextMap contextMap, List<T> postPartitionProcessData);

	/**
	 * Get Data in partition and save each partition and provides hook for post
	 * partition process
	 *
	 * @param jobId         - Job Id
	 * @param parametersMap - Job parameters
	 * @param contextMap    - Job contextMap shared across data services
	 * @return Data Model List
	 */
	@SneakyThrows
	@Override
	public List<D> getData(String jobId, Map<String, Object> parametersMap, ContextMap contextMap) {
		PartitionInfo partitionInfo = contextMap.get(BatchConstant.PARTITION_COUNT);

		if (Objects.isNull(partitionInfo)) {
			throw new IllegalStateException("Partition info not available");
		}
		String query = getDataQuery(jobId, parametersMap, contextMap);
		log.info("Query template for partition : {}", query);

		int countPartition = 0;

		Map<String, String> partitionMap = new LinkedHashMap<>();

		for (Integer offset : partitionInfo.getOffsets()) {
			String loadQuery = query.replace(BatchConstant.QUERY_TOKEN_START_OFFSET, String.valueOf(offset))
					.replace(BatchConstant.QUERY_TOKEN_END_OFFSET, String.valueOf(partitionInfo.getBatchCount()));
			partitionMap.put("partition_" + countPartition, loadQuery);
			countPartition++;
		}

		List<T> postPartitionProcessDataList = new ArrayList<>();

		for (Map.Entry<String, String> entry : partitionMap.entrySet()) {
			String partitionName = entry.getKey();
			String partitionQuery = entry.getValue();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			List<D> dataModelList = getDataModelList(jobId, partitionQuery, parametersMap, contextMap);
			int recordCount = CollectionUtils.isNotEmpty(dataModelList) ? dataModelList.size() : 0;
			stopWatch.stop();
			log.info(
					"Partition Query Execution Completed | dataService:[{}] partitionName:[{}] recordCount:[{}] timeTaken:[{}] ms ",
					getDataServiceName(), partitionName, recordCount, stopWatch.getLastTaskTimeMillis());
			T postProcessData = processPartitionData(jobId, parametersMap, contextMap, partitionName, dataModelList);

			if (postProcessData != null) {
				postPartitionProcessDataList.add(postProcessData);
			}

			storePartitionedData(jobId, jobId + "/" + getDataFolderName(), partitionName, dataModelList);
		}

		savePostPartitionProcessData(jobId, parametersMap, contextMap, postPartitionProcessDataList);

		return null;

	}

	/**
	 * Store Data As partitioned store is happening is getData() method hence this
	 * becomes dummy implementation
	 * 
	 * @param jobId         - Job Id
	 * @param data          - list of data models that needs to be stored
	 * @param parametersMap - Job parameters
	 * @param contextMap    - Job contextMap shared across data services
	 */
	@Override
	public void storeData(String jobId, List<D> data, Map<String, Object> parametersMap, ContextMap contextMap) {
		// Partitioned store happens in getData() itself hence this becomes dummy
		// implementation
	}
}
