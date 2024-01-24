package com.batch.process.common.utils.storage.client;

import static java.lang.String.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import com.batch.process.common.utils.file.FileCompressionUtil;
import com.batch.process.common.utils.file.FileConstants;
import com.batch.process.common.utils.storage.S3Storage;
import com.batch.process.common.utils.storage.StorageCheckUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Profile(value = { "!dev" })
@Component("fileClient")
@Slf4j
public class JsonS3Client implements JsonFileClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    public JsonS3Client() {
    }

    public List<String> get(String storage, String customLocation) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storage);
        List<String> filePath = new ArrayList<>();

        try {
            log.info("Get request for bucket:{}, key:{}", s3Storage.getBucket(), customLocation);
            List<String> fileNames = getFileNames(s3Storage, customLocation);

            final Path tempDownloadProcessedTradesDirPath = Files.createTempDirectory("tempProcessedDirectory");
            final File tempDownloadProcessedTradesDir = tempDownloadProcessedTradesDirPath.toFile();
            tempDownloadProcessedTradesDir.mkdirs();

            for (String fileName : fileNames) {
                final String downloadLocation = downloadFile(s3Storage, fileName, tempDownloadProcessedTradesDir);

                if (StringUtils.isNotBlank(downloadLocation)) {
                    filePath.add(downloadLocation);
                }
            }
            log.info("Successfully saved file at : {}", tempDownloadProcessedTradesDir);
        } catch (IOException | S3Exception ex) {
            log.error("Exception occurred", ex);
        }
        return filePath;
    }

    @Override
    public <M> void put(String storage, String jobId, String fileName, List<M> modelList) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storage);

        try {
            final File tempFile = writeModelToTempLocation(modelList);
            saveFileToS3(s3Storage, jobId, fileName, tempFile);
        } catch (S3Exception ex) {
            log.error("Exception occurred", ex);
        }
    }

    @Override
    public boolean delete(String storage, String location) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storage);

        if (StringUtils.isNotBlank(location)) {
            String s3Location = s3Storage.getBaseLocation() + location;
            log.info("Delete request for bucket:{}, keyName:{}, tempFile:{}", s3Storage.getBucket(), s3Location,
                    location);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Storage.getBucket())
                    .key(s3Location)
                    .build();
            @SuppressWarnings("unused")
            final DeleteObjectResponse deleteObjectResponse = s3Storage.getS3()
                    .deleteObject(deleteObjectRequest);
            log.info("Delete request to S3 is successful");
            return true;
        }
        return false;
    }

    private void saveFileToS3(S3Storage backOfficeS3Client, String jobId, String fileName, File tempFile) {

        if (tempFile != null) {
            String s3Location = backOfficeS3Client.getBaseLocation() + jobId + FileConstants.SLASH + fileName
                    + FileConstants.GZ_EXTENSION;
            log.info("Put request for bucket:{}, keyName:{}, tempFile:{}", backOfficeS3Client.getBucket(), s3Location,
                    tempFile.getName());
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(backOfficeS3Client.getBucket())
                    .key(s3Location)
                    .build();
            final Path sourcePath = Path.of(tempFile.toURI());
            final PutObjectResponse putObjectResponse = backOfficeS3Client.getS3()
                    .putObject(putObjectRequest, sourcePath);
            SdkHttpResponse sdkHttpResponse = putObjectResponse.sdkHttpResponse();
            String putResponse = String.format("Response successful=%b, statusCode=%s, statusMessage=%s",
                    sdkHttpResponse.isSuccessful(), sdkHttpResponse.statusCode(), sdkHttpResponse.statusText());
            log.info("Put request to S3 is successful: {}", putResponse);
            tempFile.delete();
        }
    }

    private <M> File writeModelToTempLocation(List<M> modelList) {
        File tempProcessedTradesFile = null;

        try {
            tempProcessedTradesFile = File.createTempFile("tmpJson", ".json");

            try (FileWriter fw = new FileWriter(tempProcessedTradesFile)) {

                for (int i = 0; i < modelList.size(); i++) {
                    M m = modelList.get(i);
                    fw.write(MAPPER.writeValueAsString(m) + (i == (modelList.size() - 1) ? "" : "\n"));
                }
            } catch (IOException e) {
                throw new IllegalStateException(format("Failed to write to file: %s", tempProcessedTradesFile), e);
            }
            log.info("Saved models to temp location:{}", tempProcessedTradesFile.getAbsolutePath());
            tempProcessedTradesFile = FileCompressionUtil.compressAndDelete(tempProcessedTradesFile.getName(),
                    tempProcessedTradesFile.getParent(), tempProcessedTradesFile.getAbsolutePath());
            log.info("Compressed file to temp location:{}", tempProcessedTradesFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Exception occurred", e);
        }
        return tempProcessedTradesFile;
    }

    private List<String> getFileNames(S3Storage s3Storage, String location) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(s3Storage.getBucket())
                .prefix(s3Storage.getBaseLocation() + location)
                .build();
        final ListObjectsV2Response listObjectsV2Response = s3Storage.getS3()
                .listObjectsV2(listObjectsV2Request);
        final List<S3Object> contents = listObjectsV2Response.contents();
        return contents.stream()
                .filter(c -> !c.key()
                        .endsWith(FileConstants.SLASH))
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    private String downloadFile(S3Storage s3Storage, String key, File tempDownloadProcessedTradesDir) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .key(key)
                .bucket(s3Storage.getBucket())
                .build();
        ResponseInputStream<GetObjectResponse> responseStream = s3Storage.getS3()
                .getObject(objectRequest);
        final String downloadLocation = tempDownloadProcessedTradesDir.getAbsolutePath() + FileConstants.SLASH
                + key.substring(key.lastIndexOf(FileConstants.SLASH) + 1);

        return FileCompressionUtil.decompressGzipFile(responseStream,
                downloadLocation.replace(FileConstants.GZ_EXTENSION, FileConstants.JSON_EXTENSION));
    }

}
