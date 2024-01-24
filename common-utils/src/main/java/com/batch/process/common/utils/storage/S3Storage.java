package com.batch.process.common.utils.storage;

import org.apache.commons.lang3.StringUtils;

import com.batch.process.common.utils.file.FileConstants;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Creates the S3 client object that is shared across all the Back Office APIs
 * 
 * @author krishna
 */
@Getter
@AllArgsConstructor
@Builder
public class S3Storage implements Storage {

    private String region;
    private String bucket;
    private S3Client s3;

    private String saveLocation;

    public S3Client getS3() {
        return s3;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public String getBaseLocation() {

        if (StringUtils.isBlank(saveLocation)) {
            return StringUtils.EMPTY;
        }
        String baseLocation = saveLocation.trim();
        char lastCharacter = baseLocation.charAt(baseLocation.length() - 1);

        if (lastCharacter == '/') {
            return baseLocation;
        } else {
            return baseLocation + FileConstants.SLASH;
        }

    }
}
