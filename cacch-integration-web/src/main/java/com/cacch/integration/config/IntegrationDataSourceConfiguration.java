package com.cacch.integration.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 集成中台主数据源（PostgreSQL），显式声明 {@link Primary}，
 * 避免与 OA 只读 Oracle 数据源并存时 Flyway / MyBatis 误绑到 OA 库。
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
public class IntegrationDataSourceConfiguration {

    /**
     * 集成 PG 主数据源（MyBatis、Flyway 均使用此 Bean）
     *
     * @return 主数据源
     */
    @Bean(name = "integrationDataSource")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource integrationDataSource() {
        return new HikariDataSource();
    }
}
