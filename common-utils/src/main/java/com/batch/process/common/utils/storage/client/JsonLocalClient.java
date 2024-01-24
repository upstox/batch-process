package com.batch.process.common.utils.storage.client;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Profile(value = { "dev", "DEV" })
@Component("fileClient")
@Slf4j
public class JsonLocalClient implements JsonFileClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    public JsonLocalClient() {
    }

    public List<String> get(String storage, String path) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);
        List<String> downloadedProcessedTradeLocation = null;
        String modelPath = localStorage.getBaseLocation() + FileConstants.SLASH + path;
        File file = new File(modelPath);

        if (file.isDirectory()) {

            for (File f : file.listFiles()) {
                boolean containsJson = f.getName()
                        .contains(FileConstants.JSON_EXTENSION);

                try {
                    FileInputStream openInputStream = FileUtils.openInputStream(f);
                    String fileWithExtn = f.getName()
                            .replace(FileConstants.GZ_EXTENSION,
                                    containsJson ? StringUtils.EMPTY : FileConstants.JSON_EXTENSION);
                    String decompressedFile = file + FileConstants.SLASH + fileWithExtn;
                    FileCompressionUtil.decompressGzipFile(openInputStream, decompressedFile);
                } catch (IOException e) {
                    String msg = format("Failed to get input stream for file: %s", f.getName());
                    throw new IllegalStateException(msg, e);
                }
                FileCompressionUtil.deleteFile(f.getPath());
            }
        } else {
            boolean jsonExtn = file.getName()
                    .contains(FileConstants.JSON_EXTENSION);

            try {
                file = new File(modelPath + FileConstants.GZ_EXTENSION);
                String fileLocation = file.getAbsolutePath();
                fileLocation = fileLocation.replace(FileConstants.GZ_EXTENSION,
                        jsonExtn ? StringUtils.EMPTY : FileConstants.JSON_EXTENSION);
                FileCompressionUtil.decompressGzipFile(FileUtils.openInputStream(file), fileLocation);
                downloadedProcessedTradeLocation = new ArrayList<>();
                downloadedProcessedTradeLocation.add(fileLocation);
                // Do not delete the gz file in local client. Worker would face issue
                // file.delete();
                return downloadedProcessedTradeLocation;
            } catch (IOException e) {
                String msg = format("Failed to get input stream for file: %s", file.getName());
                throw new IllegalStateException(msg, e);
            }
        }
        downloadedProcessedTradeLocation = Arrays.stream(file.listFiles())
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        return downloadedProcessedTradeLocation;
    }

    public <M> void put(String storage, String path, String fileName, List<M> modelList) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);

        String absolutePath = computeBaseLocation(localStorage.getBaseLocation()) + path;

        try {
            Files.createDirectories(Path.of(absolutePath));
        } catch (IOException e) {
            log.warn("Directory already created", e.getMessage());
        }
        writeFile(absolutePath, fileName, modelList);
    }

    @Override
    public boolean delete(String storage, String customLocation) {
        StorageCheckUtil.checkStorageAndThrowException(storage);
        LocalStorage localStorage = (LocalStorage) genericApplicationContext.getBean(storage);

        try {
            return Files.deleteIfExists(Path.of(localStorage.getBaseLocation() + FileConstants.SLASH + customLocation));
        } catch (IOException e) {
            String msg = String.format("Exception occurred while deleting the file at : %s", customLocation);
            log.error(msg, e);
            return false;
        }
    }

    private static String computeBaseLocation(String baseLocation) {
        return StringUtils.isEmpty(baseLocation) ? "" : baseLocation + FileConstants.SLASH;
    }

    private <M> void writeFile(String baseLocation, String fileName, List<M> modelList) {
        String filePath = baseLocation + FileConstants.SLASH + fileName;

        try (FileWriter fw = new FileWriter(filePath)) {

            for (int i = 0; i < modelList.size(); i++) {
                M m = modelList.get(i);
                fw.write(MAPPER.writeValueAsString(m) + (i == (modelList.size() - 1) ? "" : "\n"));
            }

        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to write filename: %s", fileName), e);
        }

        try {
            FileCompressionUtil.compressAndDelete(fileName, baseLocation, filePath);
        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to compress file: %s", fileName), e);
        }
    }
}
