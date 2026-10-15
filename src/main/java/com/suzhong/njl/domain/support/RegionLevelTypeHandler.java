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
 */
public class RegionLevelTypeHandler extends BaseTypeHandler<RegionLevel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RegionLevel parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.code());
    }

    @Override
    public RegionLevel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : RegionLevel.fromCode(code);
    }

    @Override
    public RegionLevel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : RegionLevel.fromCode(code);
    }

    @Override
    public RegionLevel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : RegionLevel.fromCode(code);
    }
}
