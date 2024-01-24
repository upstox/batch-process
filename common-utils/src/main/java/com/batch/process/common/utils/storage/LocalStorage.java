package com.batch.process.common.utils.storage;

public class LocalStorage implements Storage {

    private final String localSaveLocation;

    public LocalStorage(String localSaveLocation) {
        this.localSaveLocation = localSaveLocation;
    }

    @Override
    public String getBaseLocation() {
        return localSaveLocation;
    }
}
