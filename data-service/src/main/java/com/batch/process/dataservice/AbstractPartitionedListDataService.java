package com.batch.process.dataservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.batch.process.common.BatchConstant;
import com.batch.process.common.PartitionInfo;
import com.batch.process.common.utils.file.FileConstants;
import com.batch.process.common.utils.file.JsonFileWriter;
import com.batch.process.common.utils.storage.client.BinaryFileClient;
import com.batch.process.dataservice.rowmapper.RowMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractPartitionedListDataService<T> implements ListDataService<T> {

    public abstract JdbcTemplate getJdbcTemplate();
    public abstract BinaryFileClient getBinaryFileClient();
    public abstract RowMapper<T> getRowMapper();
    public abstract String getStorageName();
    public abstract String getDataFolderName();
    public abstract String getDataQuery(String jobId, Map<String, Object> parametersMap, ContextMap contextMap);

    @Override
    public List<T> getData(String jobId, Map<String, Object> parametersMap, ContextMap contextMap) {

        String query = getDataQuery(jobId, parametersMap, contextMap);
        Map<String, String> partitionMap = populatePartitionVsQueryMap(contextMap, query);

        try {

            File outputFolder = Files.createTempDirectory(jobId)
                    .toFile();
            String tempOutputFolderPath = outputFolder.getAbsolutePath();

            log.info("Temp Output Folder: {} create", tempOutputFolderPath);

            for (Map.Entry<String, String> entry : partitionMap.entrySet()) {
                String partitionName = entry.getKey();
                String partitionQuery = entry.getValue();

                File outputFile = new File(outputFolder, partitionName + FileConstants.JSON_EXTENSION);

                AtomicInteger rowCount = new AtomicInteger(0);

                try (JsonFileWriter<T> jsonFileWriter = new JsonFileWriter<>(outputFile)) {
                    getJdbcTemplate().query(partitionQuery, rs -> {
                        rowCount.getAndIncrement();
                        T mapRow = getRowMapper().mapRow(rs);
                        jsonFileWriter.write(mapRow);
                    });
                    log.info("Row count for partition: {}, row count: {}", partitionName, rowCount);
                } catch (IOException e1) {
                    throw new IllegalStateException(e1.getMessage());
                }

                getBinaryFileClient()
                        .archiveAndPut(
                                getStorageName(), jobId + FileConstants.SLASH + getDataFolderName()
                                        + FileConstants.SLASH + partitionName + FileConstants.JSON_EXTENSION,
                                outputFile.getAbsolutePath());
            }

            FileUtils.deleteDirectory(outputFolder);
            log.info("Temp Output Folder: {} deleted", tempOutputFolderPath);
        } catch (IOException e) {
            log.error("Exception occurred: {}", e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }

        return null;
    }

    private Map<String, String> populatePartitionVsQueryMap(ContextMap contextMap, String query) {
        PartitionInfo partitionInfo = contextMap.get(BatchConstant.PARTITION_COUNT);

        if (Objects.isNull(partitionInfo)) {
            throw new IllegalStateException("Partition info not available");
        }
        log.info("Query template for partition : {}", query);

        int countPartition = 0;

        Map<String, String> partitionMap = new LinkedHashMap<>();

        for (Integer offset : partitionInfo.getOffsets()) {
            String loadQuery = query//
                    .replace(BatchConstant.QUERY_TOKEN_START_OFFSET, String.valueOf(offset))
                    .replace(BatchConstant.QUERY_TOKEN_END_OFFSET, String.valueOf(partitionInfo.getBatchCount()));
            partitionMap.put("partition_" + countPartition, loadQuery);
            countPartition++;
        }
        return partitionMap;
    }

}
