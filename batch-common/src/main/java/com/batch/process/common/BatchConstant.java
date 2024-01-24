package com.batch.process.common;

public class BatchConstant {
    /**
     * Represents the key used within the framework to hold the
     * {@link PartitionInfo} object that helps in partitioning of data.
     */
    public static final String PARTITION_COUNT = "PARTITION_COUNT";
    /**
     * Represents the key used within the framework to hold the {@link RangeInfo}
     * object that helps in finding the range of data in database based on min and
     * max ID of the primary key
     */
    public static final String RANGE_INFO = "RANGE_INFO";

    /**
     * Bean name constants
     */
    public static final String BEAN_ACTIVE_MQ_CONNECTION = "batchProcessActiveMqConnection";

    public static final String BEAN_JOB_EXPLORER = "batchProcessJobExplorer";
    public static final String BEAN_JOB_LAUNCHER = "batchProcessJobLauncher";
    public static final String BEAN_EXECUTOR_POOL = "batchProcessExecutorPool";
    public static final String BEAN_JDBC_TEMPLATE = "batchProcessJdbcTemplate";

    /**
     * Query Tokens
     */
    public static final String QUERY_TOKEN_START_OFFSET = "@start_offset@";
    public static final String QUERY_TOKEN_END_OFFSET = "@end_offset@";

    /**
     * The job submission queue name
     */
    public static final String QUEUE_JOB_INPUT_REQUEST_ID = "batchJobInputRequestQueueId";

    /**
     * The key to be used by consumers for the job type
     */
    public static final String KEY_REQUEST_JOB_TYPE = "jobType";

    /**
     * The key to be used by consumers for the job parameters map
     */
    public static final String KEY_REQUEST_PARAMETERS = "parameters";

    private BatchConstant() {
    }

    /**
     * Context Map constants
     */
    public static final String MANAGER_TIME = "ManagerTime";
    public static final String TOTAL_JOB_TIME = "TotalJobTime";
    public static final String FILE = "file";
    public static final String JOB_REQUEST_ID = "requestId";
    public static final String QUEUE_JOB_INPUT_REQUEST = "queueJobInputRequest";

}
