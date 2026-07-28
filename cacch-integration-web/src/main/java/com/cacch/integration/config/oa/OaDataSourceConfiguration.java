package com.cacch.integration.config.oa;

import com.cacch.integration.common.config.oa.OaDataSourceProperties;
import com.cacch.integration.integration.oa.support.ReadOnlyOaJdbcTemplate;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * OA 数据库第二数据源配置（严格只读）
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(OaDataSourceProperties.class)
@ConditionalOnProperty(prefix = "oa.datasource", name = "url")
public class OaDataSourceConfiguration {

    /**
     * OA 库只读数据源
     *
     * @param properties OA 数据源配置
     * @return 只读数据源 Bean
     */
    @Bean(name = "oaDataSource")
    public DataSource oaDataSource(OaDataSourceProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setPoolName("oa-hikari-readonly");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);
        dataSource.setReadOnly(true);
        return dataSource;
    }

    /**
     * OA 库只读 JdbcTemplate（拦截一切写操作）
     *
     * @param oaDataSource OA 只读数据源
     * @return 只读 JdbcTemplate
     */
    @Bean(name = "oaJdbcTemplate")
    public JdbcTemplate oaJdbcTemplate(@Qualifier("oaDataSource") DataSource oaDataSource) {
        return new ReadOnlyOaJdbcTemplate(oaDataSource);
    }
}
