package com.batch.process.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

@Component
public class ProfileInfo {

    @Autowired
    private ConfigurableEnvironment env;

    public boolean isDevProfile() {
        String[] activeProfiles = env.getActiveProfiles();

        for (String profile : activeProfiles) {

            if ("dev".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

}
