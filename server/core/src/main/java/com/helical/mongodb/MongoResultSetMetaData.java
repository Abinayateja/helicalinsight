package com.helical.mongodb;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class MongoResultSetMetaData implements ResultSetMetaData {

    private final List<String> columnNames;
    private final List<Integer> columnTypes;
    private String tableName = "";

    public MongoResultSetMetaData(List<String> columnNames, List<Integer> columnTypes) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
    }

    public MongoResultSetMetaData(List<String> columnNames, List<Integer> columnTypes, String tableName) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
        this.tableName = tableName != null ? tableName : "";
    }

    @Override
    public int getColumnCount() throws SQLException {
        return columnNames != null ? columnNames.size() : 0;
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        return columnNullable;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        int type = getColumnType(column);
        return type == Types.INTEGER || type == Types.BIGINT || type == Types.DOUBLE || type == Types.FLOAT;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return 255;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return getColumnName(column);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        checkColumnIndex(column);
        return columnNames.get(column - 1);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return 10;
    }

    @Override
    public int getScale(int column) throws SQLException {
        return 0;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return tableName;
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return "";
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        checkColumnIndex(column);
        if (columnTypes != null && column - 1 < columnTypes.size()) {
            return columnTypes.get(column - 1);
        }
        return Types.VARCHAR;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        int type = getColumnType(column);
        switch (type) {
            case Types.INTEGER:
                return "INTEGER";
            case Types.BIGINT:
                return "BIGINT";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
            case Types.DATE:
                return "DATE";
            default:
                return "VARCHAR";
        }
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        int type = getColumnType(column);
        switch (type) {
            case Types.INTEGER:
                return Integer.class.getName();
            case Types.BIGINT:
                return Long.class.getName();
            case Types.DOUBLE:
                return Double.class.getName();
            case Types.BOOLEAN:
                return Boolean.class.getName();
            case Types.TIMESTAMP:
            case Types.DATE:
                return java.sql.Timestamp.class.getName();
            default:
                return String.class.getName();
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    private void checkColumnIndex(int column) throws SQLException {
        if (column < 1 || column > getColumnCount()) {
            throw new SQLException("Invalid column index: " + column + ". Total columns: " + getColumnCount());
        }
    }
}
