package com.batch.process.dataservice;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.jms.IllegalStateException;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.PartitionInfo;

import java.util.Map;

@Slf4j
public abstract class AbstractPartitionInfoDataService implements SingleDataService<PartitionInfo> {

    protected abstract long getTotalCount(String jobId, Map<String, Object> parameterMap, ContextMap contextMap);

    protected abstract int getBatchCount(String jobId, Map<String, Object> parameterMap, ContextMap contextMap);

    @SneakyThrows
    public PartitionInfo getData(String jobId, Map<String, Object> parameterMap, ContextMap contextMap) {
        long count = getTotalCount(jobId, parameterMap, contextMap);
        int batchCount = getBatchCount(jobId, parameterMap, contextMap);

        if (batchCount == 0) {
            throw new IllegalStateException("Batch count cannot be 0");
        }

        long partitionSize = count / batchCount + (count % batchCount != 0 ? 1 : 0);
        PartitionInfo partitionInfo = PartitionInfo.builder()
                .partitionCount((int) partitionSize)
                .batchCount(batchCount)
                .totalCount((int) count)
                .build();

        int countPartition = 0;

        while (count > 0) {
            int startOffset = (batchCount * countPartition);
            partitionInfo.addOffset(startOffset);
            count = count - batchCount;
            countPartition++;
        }
        log.info("Partition info details: {}", partitionInfo);
        return partitionInfo;
    }

    @Override
    public void storeData(String jobId, PartitionInfo partitionInfo, Map<String, Object> parameterMap,
            ContextMap contextMap) {
        contextMap.put(BatchConstant.PARTITION_COUNT, partitionInfo);
    }
}
