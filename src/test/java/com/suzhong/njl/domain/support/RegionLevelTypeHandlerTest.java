package com.suzhong.njl.domain.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.domain.enums.RegionLevel;

class RegionLevelTypeHandlerTest {

    private final RegionLevelTypeHandler handler = new RegionLevelTypeHandler();

    @Test
    void writesTheNumericCode() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);

        handler.setNonNullParameter(statement, 2, RegionLevel.CITY, null);

        verify(statement).setInt(2, RegionLevel.CITY.code());
    }

    @Test
    void readsByColumnNameAndTreatsNullAsNull() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("level")).thenReturn(RegionLevel.DISTRICT.code());
        when(resultSet.wasNull()).thenReturn(false, true);

        assertThat(handler.getNullableResult(resultSet, "level")).isEqualTo(RegionLevel.DISTRICT);
        assertThat(handler.getNullableResult(resultSet, "level")).isNull();
    }

    @Test
    void readsByColumnIndexAndCallableStatement() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt(1)).thenReturn(RegionLevel.PROVINCE.code());
        when(resultSet.wasNull()).thenReturn(false);

        CallableStatement callable = mock(CallableStatement.class);
        when(callable.getInt(3)).thenReturn(0);
        when(callable.wasNull()).thenReturn(true);

        assertThat(handler.getNullableResult(resultSet, 1)).isEqualTo(RegionLevel.PROVINCE);
        assertThat(handler.getNullableResult(callable, 3)).isNull();
    }
}
