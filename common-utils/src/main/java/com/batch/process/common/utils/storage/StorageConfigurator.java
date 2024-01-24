package com.batch.process.common.utils.storage;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import com.batch.process.common.utils.ProfileInfo;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "batch.storage")
public class StorageConfigurator {

    private static final String STORAGE = "storage";

    private List<String> regions = new ArrayList<>();

    private List<String> buckets = new ArrayList<>();

    private List<String> saveLocations = new ArrayList<>();

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Autowired
    private ProfileInfo profileInfo;

    @PostConstruct
    public void init() {

        if (profileInfo.isDevProfile()) {
            registerLocalStorageBeans();
        } else {
            registerS3StorageBeans();
        }
    }

    private void registerLocalStorageBeans() {

        for (int i = 0; i < saveLocations.size(); i++) {
            LocalStorage localStorage = new LocalStorage(saveLocations.get(i));
            genericApplicationContext.registerBean(getStorageBeanName(i), LocalStorage.class, () -> localStorage,
                    ls -> ls.setAutowireCandidate(false));
        }
    }

    private void registerS3StorageBeans() {
        int min = getMinimumCollectionSize();

        for (int i = 0; i < min; i++) {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(regions.get(i)))
                    .build();
            S3Storage s3Storage = S3Storage.builder()
                    .bucket(buckets.get(i))
                    .region(regions.get(i))
                    .s3(s3)
                    .saveLocation(saveLocations.get(i))
                    .build();
            genericApplicationContext.registerBean(getStorageBeanName(i), S3Storage.class, () -> s3Storage,
                    s3s -> s3s.setAutowireCandidate(false));
        }
    }

    private int getMinimumCollectionSize() {
        return Math.min(regions.size(), buckets.size());
    }

    private String getStorageBeanName(int i) {
        return STORAGE + i;
    }

}
