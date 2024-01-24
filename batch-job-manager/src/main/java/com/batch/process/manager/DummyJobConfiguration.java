package com.batch.process.manager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

import com.batch.process.common.validation.model.Problem;
import com.batch.process.dataservice.ContextMap;
import com.batch.process.dataservice.DataService;
import com.batch.process.dataservice.SingleDataService;
import com.batch.process.dataservice.configuration.AbstractBatchJobConfiguration;

@Configuration
public class DummyJobConfiguration extends AbstractBatchJobConfiguration {

    private static final String PRIVATE_DUMMY_JOB_TYPE = "DummyJob";
    private final Map<String, DataService<?>> nameToDataServiceMap = new LinkedHashMap<>();
    private final DummyCleanUp dummyCleanup = new DummyCleanUp();

    @Override
    public String getJobType() {
        return PRIVATE_DUMMY_JOB_TYPE;
    }

    @PostConstruct
    public void postContruct() {
        nameToDataServiceMap.put(dummyCleanup.getDataServiceName(), dummyCleanup);
    }

    @Override
    public Map<String, DataService<?>> getDataServices() {
        return Collections.unmodifiableMap(nameToDataServiceMap);
    }

    @Override
    public DataService<?> getCleanUpService() {
        return dummyCleanup;
    }

    @Override
    public List<Problem> validate(Properties properties) {
        return Collections.emptyList();
    }

    private static final class DummyCleanUp implements SingleDataService<String> {

        private static final String DUMMY_CLEAN_UP = "DummyCleanUp";

        @Override
        public String getDataServiceName() {
            return DUMMY_CLEAN_UP;
        }

        @Override
        public String getData(String jobId, Map<String, Object> parameterMap, ContextMap contextMap) {
            return StringUtils.EMPTY;
        }

        @Override
        public void storeData(String string, String data, Map<String, Object> parameterMap, ContextMap contextMap) {

        }

    }

}
