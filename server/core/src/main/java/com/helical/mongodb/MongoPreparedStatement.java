package com.helical.mongodb;

import com.mongodb.client.MongoDatabase;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class MongoPreparedStatement extends MongoStatement implements PreparedStatement {

    private final String sql;
    private final Map<Integer, Object> parameters = new TreeMap<>();

    public MongoPreparedStatement(MongoConnection connection, MongoDatabase database, String sql) {
        super(connection, database);
        this.sql = sql;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return executeQuery(getEffectiveSql());
    }

    @Override
    public int executeUpdate() throws SQLException {
        return executeUpdate(getEffectiveSql());
    }

    @Override
    public boolean execute() throws SQLException {
        return execute(getEffectiveSql());
    }

    private String getEffectiveSql() {
        if (parameters.isEmpty()) {
            return sql;
        }
        StringBuilder sb = new StringBuilder();
        int paramIdx = 1;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?') {
                Object val = parameters.get(paramIdx++);
                if (val == null) {
                    sb.append("NULL");
                } else if (val instanceof Number || val instanceof Boolean) {
                    sb.append(val);
                } else {
                    sb.append("'").append(val.toString().replace("'", "''")).append("'");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        parameters.put(parameterIndex, null);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {}

    @Override
    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {}

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {}

    @Override
    public void clearParameters() throws SQLException {
        parameters.clear();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void addBatch() throws SQLException {}

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {}

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {}

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {}

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {}

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {}

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        parameters.put(parameterIndex, null);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {}

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        parameters.put(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {}

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {}

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {}

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {}

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        parameters.put(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {}

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {}

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {}

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {}

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {}

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {}

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {}
}
