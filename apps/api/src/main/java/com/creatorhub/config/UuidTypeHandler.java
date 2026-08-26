package com.creatorhub.config;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * PostgreSQL UUID 与 Java {@link UUID} 的显式 MyBatis 类型映射。
 *
 * <p>PostgreSQL JDBC 使用 {@code Types.OTHER} 传输 UUID；显式处理器避免依赖驱动或 MyBatis 版本的隐式推断，并同时覆盖参数绑定与结果读取。
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(
      PreparedStatement statement, int index, UUID parameter, JdbcType jdbcType)
      throws SQLException {
    statement.setObject(index, parameter, Types.OTHER);
  }

  @Override
  public UUID getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
    return resultSet.getObject(columnName, UUID.class);
  }

  @Override
  public UUID getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
    return resultSet.getObject(columnIndex, UUID.class);
  }

  @Override
  public UUID getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
    return statement.getObject(columnIndex, UUID.class);
  }
}
