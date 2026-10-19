package com.suzhong.njl.domain.support;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.suzhong.njl.domain.enums.RegionLevel;

/**
 * TINYINT 在 H2 上可能以非整型 Integer 返回，按数值读取，避免枚举对不上。
 *
 * <p>解决的问题：{@code region.level} 是 TINYINT，而 {@link RegionLevel} 的 {@code @EnumValue}
 * 是 {@code int}，H2 的 JDBC 驱动可能把该列读成其他数值包装类型，默认枚举转换会失败；
 * 这里统一用 {@link ResultSet#getInt(String)} 取数值，再交给 {@link RegionLevel#fromCode(int)} 还原，
 * 写入时也只用 {@code setInt}，保证读写两侧对称。
 *
 * <p>注册方式：不做全局注册，由实体字段显式声明
 * {@code @TableField(typeHandler = RegionLevelTypeHandler.class)}（见 {@code Region.level}）；
 * 因此只影响 {@code region.level} 一列，其他枚举仍走 MyBatis-Plus 默认的 {@code @EnumValue} 转换。
 *
 * <p>null 处理：写入侧只在参数非空时被调用；读取侧先用 {@code wasNull()} 判断，
 * 数据库为 NULL 时返回 null，不抛异常。非 NULL 但编码不在 1 至 3 之间时，
 * {@code fromCode} 会抛 {@link IllegalArgumentException}，属于数据异常，不做静默兜底。
 */
public class RegionLevelTypeHandler extends BaseTypeHandler<RegionLevel> {

    /** 写入：把枚举的数字编码以 int 绑定到预编译参数。 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RegionLevel parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.code());
    }

    /** 按列名读取：列为 NULL 返回 null，否则按数字编码还原枚举。 */
    @Override
    public RegionLevel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : RegionLevel.fromCode(code);
    }

    /** 按列下标读取：列为 NULL 返回 null，否则按数字编码还原枚举。 */
    @Override
    public RegionLevel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : RegionLevel.fromCode(code);
    }

    /** 存储过程出参读取：列为 NULL 返回 null，否则按数字编码还原枚举。 */
    @Override
    public RegionLevel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : RegionLevel.fromCode(code);
    }
}
