package com.batch.process.common.utils.storage.client;

import java.io.File;
import java.io.FileOutputStream;
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

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
@Profile(value = { "!dev" })
@Component("binaryClient")
public class BinaryS3Client implements BinaryFileClient {

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Override
    public void put(String storageBeanName, String s3FileLocation, String localFileLocation) {
        StorageCheckUtil.checkStorageAndThrowException(storageBeanName);
        File file = new File(localFileLocation);

        if (!file.exists()) {
            log.error("****** File doesn't exist to uplaod to S3 bucket ********* {}", localFileLocation);
            return;
        }

        saveFileToS3(storageBeanName, s3FileLocation, localFileLocation);
    }

    @Override
    public void archiveAndPut(String storageBeanName, String s3FileLocation, String localFileLocation) {

        try {
            File inputFile = new File(localFileLocation);

            if (!inputFile.exists()) {
                log.error("****** File doesn't exist to uplaod to S3 bucket ********* {}", localFileLocation);
                return;
            }

            String fileName = inputFile.getName();

            if (!StringUtils.endsWithIgnoreCase(fileName, FileConstants.GZ_EXTENSION)) {
                inputFile = FileCompressionUtil.compressAndDelete(fileName, inputFile.getParent(),
                        inputFile.getAbsolutePath());
            }

            saveFileToS3(storageBeanName, s3FileLocation, inputFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to upload file to S3:{}", e.getMessage());
            throw new IllegalStateException(e);
        }

    }

    @Override
    public List<String> get(String storage, String s3Location) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storage);
        List<String> filePath = new ArrayList<>();

        try {
            log.info("Get request for bucket:{}, key:{}", s3Storage.getBucket(), s3Location);
            List<String> fileNames = getFileNames(s3Storage, s3Location);
            log.info("Names of temp file name: {}", fileNames);

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
            throw new IllegalStateException("Exception occurred while get file", ex);
        }
        return filePath;
    }

    @Override
    public List<String> unarchiveAndGet(String storage, String s3Location) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storage);
        List<String> filePath = new ArrayList<>();

        try {
            log.info("Get request for bucket:{}, key:{}", s3Storage.getBucket(), s3Location);
            List<String> fileNames = getFileNames(s3Storage, s3Location);
            log.info("Names of temp file name: {}", fileNames);

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
            throw new IllegalStateException("Exception occurred while get file", ex);
        }
        filePath = FileCompressionUtil.getUncompressedFiles(filePath);
        return filePath;
    }

    public void put(String storage, String jobId, String fileName, byte[] data) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storage);

        try {
            final File tempFile = writeTradesToTempLocation(data);
            saveFileToS3(s3Storage, jobId, fileName, tempFile);
        } catch (S3Exception ex) {
            throw new IllegalStateException("Exception occurred while get file", ex);
        }
    }

    private void saveFileToS3(String storageBeanName, String s3FileLocation, String localFileLocation) {

        try {
            StorageCheckUtil.checkStorageAndThrowException(storageBeanName);
            S3Storage s3Storage = (S3Storage) genericApplicationContext.getBean(storageBeanName);

            String baseLocation = s3Storage.getBaseLocation();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Storage.getBucket())//
                    .key(baseLocation + s3FileLocation)//
                    .build();

            final Path sourcePath = Path.of(localFileLocation);
            s3Storage.getS3()
                    .putObject(putObjectRequest, sourcePath);
            log.info("file uploaded to S3 bucket:{}", s3FileLocation);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload file to S3", e);
        }
    }

    private void saveFileToS3(S3Storage s3Storage, String jobId, String fileName, File tempFile) {

        if (tempFile != null) {
            String baseLocation = s3Storage.getBaseLocation();
            String s3Location = baseLocation + jobId + FileConstants.SLASH + fileName;
            log.info("Put request for bucket:{}, keyName:{}, tempFile:{}", s3Storage.getBucket(), s3Location,
                    tempFile.getName());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Storage.getBucket())
                    .key(s3Location)
                    .build();
            final Path sourcePath = Path.of(tempFile.toURI());
            final PutObjectResponse putObjectResponse = s3Storage.getS3()
                    .putObject(putObjectRequest, sourcePath);
            SdkHttpResponse sdkHttpResponse = putObjectResponse.sdkHttpResponse();
            String putResponse = String.format("Response successful=%b, statusCode=%s, statusMessage=%s",
                    sdkHttpResponse.isSuccessful(), sdkHttpResponse.statusCode(), sdkHttpResponse.statusText());
            log.info("Put request to S3 is successful: {}", putResponse);
            tempFile.delete();
        }
    }

    private File writeTradesToTempLocation(byte[] data) {
        File tempProcessedTradesFile = null;
        FileOutputStream fileOutputStream = null;

        try {
            tempProcessedTradesFile = File.createTempFile("binaryFile", "");
            String tempFilePath = tempProcessedTradesFile.getAbsolutePath();
            fileOutputStream = new FileOutputStream(tempFilePath);
            fileOutputStream.write(data);
            log.info("Saved processed trades to temp location:{}", tempProcessedTradesFile.getAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Exception occurred while put file", e);
        } finally {

            if (fileOutputStream != null) {

                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    log.error("Exception occurred", e);
                }
            }
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
        final String downloadLocation = tempDownloadProcessedTradesDir.getAbsolutePath() + FileConstants.SLASH
                + key.substring(key.lastIndexOf(FileConstants.SLASH) + 1);

        try (ResponseInputStream<GetObjectResponse> responseStream = s3Storage.getS3()
                .getObject(objectRequest);//
                FileOutputStream fileOutputStream = new FileOutputStream(downloadLocation)) {
            byte[] buffer = new byte[8 * 1024];
            int len;

            while ((len = responseStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception occurred while fetching file", e);
        }
        return downloadLocation;
    }

}
