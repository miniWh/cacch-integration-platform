package com.cacch.integration.dao.typehandler;

import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PostgreSQL jsonb 列类型处理器。
 * <p>
 * Jackson3TypeHandler 默认以 varchar 写入，PostgreSQL jsonb 列需要显式绑定为 jsonb 类型。
 *
 * @author hongfu_zhou@cacch.com
 */
public class PostgreSqlJsonbTypeHandler extends Jackson3TypeHandler {

    public PostgreSqlJsonbTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(toJson(parameter));
        ps.setObject(i, jsonObject);
    }
}
