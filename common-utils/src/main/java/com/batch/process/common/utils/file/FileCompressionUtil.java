package com.batch.process.common.utils.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class FileCompressionUtil {

    public File compressAndDelete(String zipFileName, String zipFodlderLocation, String fileToBeCompressed)
            throws IOException {
        File zipFile = new File(zipFodlderLocation, zipFileName + FileConstants.GZ_EXTENSION);
        String newFilePath = compressToGzipFile(fileToBeCompressed, zipFile.getAbsolutePath());

        deleteFile(fileToBeCompressed);
        return new File(newFilePath);
    }

    public void deleteFile(String filePath) {
        log.info("Deleting file {}", filePath);

        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            log.error("Exception occurred", e);
        }
    }

    public String decompressGzipFile(InputStream is, String decompressedFile) {
        log.info("Decompressing to {}", decompressedFile);
        GZIPInputStream gzipIS = null;
        FileOutputStream fos = null;

        try {
            gzipIS = new GZIPInputStream(is);
            fos = new FileOutputStream(decompressedFile);
            byte[] buffer = new byte[8 * 1024];
            int len;

            while ((len = gzipIS.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            log.error("Exception occurred", e);
            return null;
        } finally {
            closeResources(is, fos, null, gzipIS);
        }
        return decompressedFile;

    }

    private String compressToGzipFile(String file, String compressedFile) throws IOException {
        log.info("Compressing {} to {}", file, compressedFile);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPOutputStream gzipOS = null;

        try {
            fis = new FileInputStream(file);
            fos = new FileOutputStream(compressedFile);
            gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[8 * 1024];
            int len;

            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
        } finally {
            closeResources(fis, fos, gzipOS, null);
        }
        return compressedFile;
    }

    public List<String> getUncompressedFiles(List<String> inputFiles) {

        if (CollectionUtils.isEmpty(inputFiles)) {
            return null;
        }
        List<String> uncompressedFileLocations = new ArrayList<>();

        for (String inputFileLocation : inputFiles) {
            File inputFile = new File(inputFileLocation);

            String inputFileName = inputFile.getName();

            if (StringUtils.containsIgnoreCase(inputFileName, FileConstants.GZ_EXTENSION)) {

                try {
                    File decompressFile = new File(inputFile.getParent(),
                            inputFileName.replace(FileConstants.GZ_EXTENSION, ""));
                    FileCompressionUtil.decompressGzipFile(new FileInputStream(inputFile),
                            decompressFile.getAbsolutePath());
                    inputFile.delete();
                    inputFile = decompressFile;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                }

            }
            uncompressedFileLocations.add(inputFile.getAbsolutePath());
        }
        return uncompressedFileLocations;

    }

    private void closeResources(InputStream fis, FileOutputStream fos, GZIPOutputStream gzipOS,
            GZIPInputStream gzipIS) {

        // close resources
        try {

            if (Objects.nonNull(gzipOS)) {
                gzipOS.close();
            }

            if (Objects.nonNull(fos)) {
                fos.close();
            }

            if (Objects.nonNull(gzipIS)) {
                gzipIS.close();
            }

            if (Objects.nonNull(fis)) {
                fis.close();
            }
        } catch (IOException e) {
            log.error("Exception occurred", e);
        }
    }

}
