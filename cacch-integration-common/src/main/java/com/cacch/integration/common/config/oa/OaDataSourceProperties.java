package com.cacch.integration.common.config.oa;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OA 数据库第二数据源配置（直连 formmain_4070 / formson_5464，严格只读）
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "oa.datasource")
public class OaDataSourceProperties {

    /**
     * JDBC URL，如 jdbc:oracle:thin:@//host:1521/orcl 或 jdbc:sqlserver://host:1433;databaseName=oa
     */
    private final String url;

    private final String username;

    private final String password;

    /**
     * JDBC 驱动类名；未配置时按 url 前缀推断（Oracle 优先）
     */
    private final String driverClassName;

    public OaDataSourceProperties(String url, String username, String password, String driverClassName) {
        this.url = url != null ? url.trim() : "";
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password : "";
        this.driverClassName = blankToDefault(driverClassName, inferDriverClassName(this.url));
    }

    public boolean isConfigured() {
        return !url.isBlank();
    }

    /**
     * 根据 JDBC URL 推断驱动类名
     *
     * @param jdbcUrl JDBC URL
     * @return 驱动类名
     */
    public static String inferDriverClassName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "oracle.jdbc.OracleDriver";
        }
        String lower = jdbcUrl.toLowerCase();
        if (lower.startsWith("jdbc:oracle")) {
            return "oracle.jdbc.OracleDriver";
        }
        if (lower.startsWith("jdbc:sqlserver")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }
        return "oracle.jdbc.OracleDriver";
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
