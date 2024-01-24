package com.batch.process.common.utils.storage;

import org.apache.commons.lang3.StringUtils;

public class StorageCheckUtil {

    public static void checkStorageAndThrowException(String storage) {

        if (StringUtils.isBlank(storage)) {
            String exceptionMsg = String.format("Storage bean:'%s' not found", storage);
            throw new IllegalArgumentException(exceptionMsg);
        }
    }

}
