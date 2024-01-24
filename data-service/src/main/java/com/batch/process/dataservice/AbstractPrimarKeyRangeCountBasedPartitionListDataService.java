package com.batch.process.dataservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StopWatch;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.PartitionInfo;
import com.batch.process.common.PostPartitionData;
import com.batch.process.common.RangeInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * A partitioner that uses the primary keys in the system to be partitioned
 * based on a batch size expected. Helpful in partitioning data where limit
 * based queries under perform on certain databases or data sources.
 * 
 * @param <D> Model for which partition needs to be done
 * @param <T> Model for post partitioning work
 */
@Slf4j
public abstract class AbstractPrimarKeyRangeCountBasedPartitionListDataService<D, T extends PostPartitionData>
		extends AbstractCountBasedPartitionListDataService<D, T> {

	@Override
	public List<D> getData(String jobId, Map<String, Object> parametersMap, ContextMap contextMap) {

		PartitionInfo partitionInfo = contextMap.get(BatchConstant.PARTITION_COUNT);
		RangeInfo rangeInfo = contextMap.get(BatchConstant.RANGE_INFO);

		if (Objects.isNull(partitionInfo)) {
			throw new IllegalStateException("Partition info not available");
		}

		if (Objects.isNull(rangeInfo)) {
			throw new IllegalStateException("RangeInfo not available");
		}
		long minId = rangeInfo.getMinId() - 1;
		long maxId = rangeInfo.getMaxId();

		String query = this.getDataQuery(jobId, parametersMap, contextMap);
		log.info("Query template for partition : {}", query);

		int countPartition = 0;

		Map<String, String> partitionMap = new LinkedHashMap<>();

		for (Integer offset : partitionInfo.getOffsets()) {
			long min = minId + offset;
			long max = min + partitionInfo.getBatchCount();
			String loadQuery = query.replace(BatchConstant.QUERY_TOKEN_START_OFFSET, String.valueOf(min))
					.replace(BatchConstant.QUERY_TOKEN_END_OFFSET, String.valueOf(Math.min(max, maxId)));
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

		return Collections.emptyList();
	}
}
