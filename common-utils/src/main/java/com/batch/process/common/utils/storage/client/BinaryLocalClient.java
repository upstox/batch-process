package com.batch.process.common.utils.storage.client;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import com.batch.process.common.utils.file.FileCompressionUtil;
import com.batch.process.common.utils.file.FileConstants;
import com.batch.process.common.utils.storage.LocalStorage;
import com.batch.process.common.utils.storage.StorageCheckUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile(value = { "dev", "DEV" })
@Component("binaryClient")
public class BinaryLocalClient implements BinaryFileClient {

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Override
    public void put(String storage, String targetLocation, String localFileLocation) {

        try {
            File file = new File(localFileLocation);

            if (!file.exists()) {
                log.error("******** File doesn't exist to uplaod to S3 bucket ********* {}", localFileLocation);
                return;
            }

            saveFileLocally(storage, targetLocation, file);
        } catch (Exception e) {
            throw new IllegalStateException("Faild to upload file to S3 ->" + localFileLocation, e);
        }
    }

    @Override
    public void archiveAndPut(String storageBeanName, String targetLocation, String localFileLocation) {

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
            saveFileLocally(storageBeanName, targetLocation, inputFile);
        } catch (Exception e) {
            throw new IllegalStateException("Faild to upload file to S3 ->" + localFileLocation, e);
        }
    }

    public void put(String storage, String jobId, String fileName, byte[] data) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);

        try {
            Files.createDirectories(Path.of(localStorage.getBaseLocation() + FileConstants.SLASH + jobId));
        } catch (IOException e) {
            log.warn("Directory already created", e.getMessage());
        }
        writeFile(localStorage, jobId, fileName, data);
    }

    public List<String> get(String storage, String jobId) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);
        List<String> downloadedProcessedTradeLocation = null;
        File sourceFile = new File(localStorage.getBaseLocation() + FileConstants.SLASH + jobId);

        if (sourceFile.isDirectory()) {
            File targetTempDirectory = copyDirectoryToTemporaryTarget(sourceFile);
            downloadedProcessedTradeLocation = Arrays.stream(targetTempDirectory.listFiles())
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
        } else {
            downloadedProcessedTradeLocation = new LinkedList<>();
            File targetTempFile = copyFileToTemporaryTarget(sourceFile);
            downloadedProcessedTradeLocation.add(targetTempFile.getAbsolutePath());
        }
        return downloadedProcessedTradeLocation;
    }

    @Override
    public List<String> unarchiveAndGet(String storage, String outputLocation) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);
        List<String> downloadedProcessedTradeLocation = null;
        File sourceFile = new File(localStorage.getBaseLocation() + FileConstants.SLASH + outputLocation);

        if (sourceFile.isDirectory()) {
            File targetTempDirectory = copyDirectoryToTemporaryTarget(sourceFile);
            downloadedProcessedTradeLocation = Arrays.stream(targetTempDirectory.listFiles())
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
        } else {
            downloadedProcessedTradeLocation = new LinkedList<>();
            File targetTempFile = copyFileToTemporaryTarget(sourceFile);
            downloadedProcessedTradeLocation.add(targetTempFile.getAbsolutePath());
        }
        downloadedProcessedTradeLocation = FileCompressionUtil.getUncompressedFiles(downloadedProcessedTradeLocation);
        return downloadedProcessedTradeLocation;
    }

    private File copyDirectoryToTemporaryTarget(File sourceDir) {

        try {
            File targetTempDirectory = Files.createTempDirectory("tempProcessDir")
                    .toFile();

            for (File file : sourceDir.listFiles()) {
                copyFile(targetTempDirectory, file);
            }
            return targetTempDirectory;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private File copyFileToTemporaryTarget(File sourceFile) {

        try {
            File targetTempDirectory = Files.createTempDirectory("tempProcessDir")
                    .toFile();
            return copyFile(targetTempDirectory, sourceFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private File copyFile(File targetTempDirectory, File file) throws FileNotFoundException, IOException {
        String fileName = file.getName();
        File targetFile = new File(targetTempDirectory,
                StringUtils.replaceIgnoreCase(fileName, FileConstants.GZ_EXTENSION, ""));

        if (StringUtils.containsIgnoreCase(fileName, FileConstants.GZ_EXTENSION)) {
            FileCompressionUtil.decompressGzipFile(new FileInputStream(file), targetFile.getAbsolutePath());
        } else {
            FileUtils.copyFile(file, targetFile);
        }
        return targetFile;
    }

    private void writeFile(LocalStorage localStorage, String jobId, String fileName, byte[] data) {

        String fileLocation = localStorage.getBaseLocation() + FileConstants.SLASH + jobId;
        String filePath = fileLocation + FileConstants.SLASH + fileName;

        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            fileOutputStream.write(data);
        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to write filename: %s", fileName), e);
        }
    }

    private void saveFileLocally(String storage, String targetLocation, File file) throws IOException {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);
        String targetParentDirectory = targetLocation.substring(0, targetLocation.lastIndexOf(File.separator));
        String directory = localStorage.getBaseLocation() + FileConstants.SLASH + targetParentDirectory;
        Files.createDirectories(Path.of(directory));
        File targetFileLocation = new File(directory + File.separator + file.getName());
        FileUtils.copyFile(file, targetFileLocation);
    }

}
