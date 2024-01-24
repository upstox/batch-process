package com.batch.process.common.utils.storage.client;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.batch.process.common.utils.file.FileCompressionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface JsonFileClient {
    public List<String> get(String storage, String customLocation);

    public <M> void put(String storage, String jobId, String fileName, List<M> modelList);

    public boolean delete(String storage, String customLocation);

    default <M> List<M> getModel(String jsonModelFilePath, Class<M> clazz) {
        String jsonFile = jsonModelFilePath.replace(".gz", "");

        if (jsonModelFilePath.endsWith(".gz")) {
            File file = new File(jsonModelFilePath);

            try {
                FileCompressionUtil.decompressGzipFile(FileUtils.openInputStream(file), jsonFile);
            } catch (IOException e) {
                throw new IllegalStateException(format("Failed to get input stream for file: %s", file.getName()), e);
            }
        }
        return readModelFromFile(jsonFile, clazz);
    }

    private static <M> List<M> readModelFromFile(String jsonFile, Class<M> clazz) {
        List<M> modelList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();

        try (FileReader fr = new FileReader(jsonFile);//
                BufferedReader br = new BufferedReader(fr);) {
            String jsonLine = null;

            while ((jsonLine = br.readLine()) != null) {
                M model = mapper.readValue(jsonLine, clazz);
                modelList.add(model);
            }
        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to read model from file file: %s", jsonFile), e);
        }
        return modelList;
    }
}
