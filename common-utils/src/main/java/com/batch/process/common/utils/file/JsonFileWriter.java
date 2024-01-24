package com.batch.process.common.utils.file;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFileWriter<T> implements Closeable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private FileWriter fileWriter;
    private BufferedWriter bufferedWriter;

    public JsonFileWriter(String fileLocation) {
        File file = new File(fileLocation);

        if (!file.exists()) {
            throw new IllegalStateException("File not found:" + fileLocation);
        }
        initialize(file);

    }

    public JsonFileWriter(File file) {
        initialize(file);

    }

    private void initialize(File file) {

        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void write(T t) {

        try {
            bufferedWriter.write(MAPPER.writeValueAsString(t));
            bufferedWriter.newLine();
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
        }
    }

    @Override
    public void close() throws IOException {

        if (bufferedWriter != null) {
            bufferedWriter.close();
        }

        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
