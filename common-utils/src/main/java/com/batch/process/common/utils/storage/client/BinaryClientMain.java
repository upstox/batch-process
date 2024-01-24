package com.batch.process.common.utils.storage.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.batch.process.common.utils.file.FileConstants;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
public class BinaryClientMain {
    // Test bucket
    private final String bucket = "kpm-trades";

    // Load aws-credentials. Note: Never commit the properties file.
    private final S3Client s3 = S3Client.builder()
            .credentialsProvider(() -> {

                try (InputStream inputStream = BinaryClientMain.class
                        .getResourceAsStream("/awsCredential.properties");) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String accessKey = properties.getProperty("AWSAccessKeyId");
                    String secretKey = properties.getProperty("AWSSecretKey");
                    return AwsBasicCredentials.create(accessKey, secretKey);
                } catch (IOException e) {
                    log.error("Exception Occurred loading aws-credentials", e);
                }
                return null;
            })// US East
            .region(Region.US_EAST_1)
            .build();

    public List<String> get(String customLocation) {
        List<String> filePath = new ArrayList<>();

        try {
            // log.info("Get request for bucket:{}, key:{}", backOfficeS3Client.getBucket(),
            // customLocation);
            List<String> fileNames = getFileNames(customLocation);

            final Path tempDownloadProcessedTradesDirPath = Files.createTempDirectory("tempProcessedDirectory");
            final File tempDownloadProcessedTradesDir = tempDownloadProcessedTradesDirPath.toFile();
            tempDownloadProcessedTradesDir.mkdirs();

            for (String fileName : fileNames) {
                final String downloadLocation = downloadFile(fileName, tempDownloadProcessedTradesDir);

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

    public void put(String jobId, String fileName, byte[] data) {

        try {
            final File tempFile = writeTradesToTempLocation(data);
            saveFileToS3(jobId, fileName, tempFile);
        } catch (S3Exception ex) {
            log.error("Exception occurred", ex);
        }
    }

    private void saveFileToS3(String jobId, String fileName, File tempFile) {

        if (tempFile != null) {
            String s3Location = getRootFileLocation() + FileConstants.SLASH + jobId + FileConstants.SLASH + fileName;
            log.info("Put request for bucket:{},  tempFile:{}", s3Location, tempFile.getName());
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Location)
                    .build();
            final Path sourcePath = Path.of(tempFile.toURI());
            final PutObjectResponse putObjectResponse = s3.putObject(putObjectRequest, sourcePath);
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
            tempProcessedTradesFile = File.createTempFile("processedTrades", "");
            String tempFilePath = tempProcessedTradesFile.getAbsolutePath();
            fileOutputStream = new FileOutputStream(tempFilePath);
            fileOutputStream.write(data);
            log.info("Saved processed trades to temp location:{}", tempProcessedTradesFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Exception occurred", e);
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

    private List<String> getFileNames(String jobId) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(getRootFileLocation() + FileConstants.SLASH + jobId)
                .build();
        final ListObjectsV2Response listObjectsV2Response = s3.listObjectsV2(listObjectsV2Request);
        final List<S3Object> contents = listObjectsV2Response.contents();
        return contents.stream()
                .filter(c -> !c.key()
                        .endsWith(FileConstants.SLASH))
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    private String downloadFile(String key, File tempDownloadProcessedTradesDir) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .key(key)
                .bucket(bucket)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
        final String downloadLocation = tempDownloadProcessedTradesDir.getAbsolutePath() + FileConstants.SLASH
                + key.substring(key.lastIndexOf(FileConstants.SLASH) + 1);

        try (InputStream inputStream = objectBytes.asInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(downloadLocation)) {
            byte[] buffer = new byte[8 * 1024];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            log.error("Exception occurred", e);
        } catch (IOException e) {
            log.error("Exception occurred", e);
        }
        return downloadLocation;
    }

    private String getRootFileLocation() {
        return "test";
    }

    public static void main(String[] args) {
        BinaryClientMain client = new BinaryClientMain();
        String file = BinaryClientMain.class.getResource("/test.pfx")
                .getFile();
        byte[] bytes = getFileByteArray(new File(file));
        client.put("3", file, bytes);
        List<String> list = client
                .get("2//Users/krishna/git/back-office-report-service/report-utils/target/classes/test.pfx");
        System.out.println(list);

    }

    private static byte[] getFileByteArray(File reportFile) {

        try {
            return FileUtils.readFileToByteArray(reportFile);
        } catch (IOException e) {
            String errorMessage = "Exception occurred while storing report file to S3.";
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
