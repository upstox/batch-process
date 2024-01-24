package com.batch.process.config;

import javax.sql.DataSource;

/**
 * Spring batch needs a primary data source to work. The implementor of this
 * class should provide the primary data source.
 * Provides primary data source for spring batch.
 * 
 * @author sameer.chawdhary
 */

public interface BatchDataSourceConfigurator {

    /**
     * Provide primary DataSource.
     * 
     * @return The {@link DataSource}
     *         <br>
     *         <b>Note:</b>must be marked as @Bean and @Primary
     */
    DataSource batchDataSource();

}
