package com.cacch.integration.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * 集成中台主数据源（PostgreSQL），显式声明 {@link Primary}，
 * 避免与 OA 只读 Oracle 数据源并存时 Flyway / MyBatis 误绑到 OA 库。
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
public class IntegrationDataSourceConfiguration {

    private static final String PG_JDBC_PREFIX = "jdbc:postgresql:";

    /**
     * 集成 PG 主数据源（MyBatis、Flyway 均使用此 Bean）
     *
     * @param url             {@code spring.datasource.url}，必须为 PostgreSQL
     * @param username        数据库用户名
     * @param password        数据库密码
     * @param driverClassName JDBC 驱动类名
     * @return 主数据源
     */
    @Bean(name = "integrationDataSource")
    @Primary
    public DataSource integrationDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException(
                    "spring.datasource.url 未配置；集成库请使用 PostgreSQL，OA Oracle 仅配置在 oa.datasource.url");
        }
        if (!url.trim().toLowerCase().startsWith(PG_JDBC_PREFIX)) {
            throw new IllegalStateException(
                    "spring.datasource.url 必须指向 PostgreSQL 集成库（" + PG_JDBC_PREFIX + "…），"
                            + "当前为 " + url + "；OA Oracle 请配置在 oa.datasource.url，勿写入 spring.datasource");
        }
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url.trim());
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("integration-hikari");
        return dataSource;
    }
}
