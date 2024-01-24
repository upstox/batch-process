package com.batch.process.common.utils.file;

import java.io.File;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtils {

    public void deleteTempFiles(List<String> filePaths) {

        if (CollectionUtils.isNotEmpty(filePaths)) {

            for (String filePath : filePaths) {
                File file = new File(filePath);

                if (file.exists()) {
                    org.apache.commons.io.FileUtils.deleteQuietly(file);
                }
            }
        }

    }

    /****
     * deletes parent folder and all empty sub folders present in the path.
     * 
     * @param filePaths
     */
    public void deleteTempFilesAndEmptyParentFolders(List<String> filePaths) {

        if (CollectionUtils.isNotEmpty(filePaths)) {

            for (String filePath : filePaths) {
                File file = new File(filePath);
                String directoryToBeDeleted = file.getParent();

                if (file.exists()) {
                    org.apache.commons.io.FileUtils.deleteQuietly(file);
                }
                deleteFolderIfEmpty(new File(directoryToBeDeleted));
            }
        }

    }

    private void deleteFolderIfEmpty(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();

        if (ArrayUtils.isNotEmpty(allContents)) {
            return;
        }
        directoryToBeDeleted.delete();
    }

}
