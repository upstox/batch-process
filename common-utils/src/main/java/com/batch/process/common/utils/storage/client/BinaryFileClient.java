package com.batch.process.common.utils.storage.client;

import java.util.List;

public interface BinaryFileClient {

    List<String> get(String storage, String jobId);

    /***
     * @deprecated use instead put(String, String, String)
     * @param storage
     * @param jobId
     * @param fileName
     * @param data
     */

    void put(String storage, String jobId, String fileName, byte[] data);

    void put(String storage, String s3FileLocation, String localFileLocation);

    /***
     * Archive the file and upload to S3
     * 
     * @param storage           - Storage bean name
     * @param s3FileLocation    - s3 location where the fill will be uploaded
     * @param localFileLocation - local file location which will get uploaded to S3
     * @author Sameer Chawdhary
     */
    void archiveAndPut(String storage, String s3FileLocation, String localFileLocation);

    /***
     * Download the file from S3 and unarhive the file if it is in archived format.
     * 
     * @param storage        - Storage bean name
     * @param s3FileLocation - s3 location where the fill will be uploaded
     * @author Sameer Chawdhary
     */
    List<String> unarchiveAndGet(String storage, String s3Location);

}
