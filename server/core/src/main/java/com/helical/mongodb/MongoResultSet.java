package com.helical.mongodb;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class MongoResultSet implements ResultSet {

    private final Statement statement;
    private final List<String> columnNames;
    private final List<Integer> columnTypes;
    private final List<List<Object>> rows;
    private final Map<String, Integer> columnNameToIndex = new HashMap<>();

    private int currentIndex = -1;
    private boolean wasNull = false;
    private boolean closed = false;

    public MongoResultSet(Statement statement, List<String> columnNames, List<Integer> columnTypes, List<List<Object>> rows) {
        this.statement = statement;
        this.columnNames = columnNames != null ? columnNames : new ArrayList<>();
        this.columnTypes = columnTypes != null ? columnTypes : new ArrayList<>();
        this.rows = rows != null ? rows : new ArrayList<>();

        for (int i = 0; i < this.columnNames.size(); i++) {
            columnNameToIndex.put(this.columnNames.get(i).toLowerCase(), i + 1);
        }
    }

    @Override
    public boolean next() throws SQLException {
        checkOpen();
        if (currentIndex + 1 < rows.size()) {
            currentIndex++;
            return true;
        }
        currentIndex = rows.size();
        return false;
    }

    @Override
    public void close() throws SQLException {
        this.closed = true;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return wasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        return val != null ? val.toString() : null;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        return Boolean.parseBoolean(val.toString());
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        Number n = getNumber(columnIndex);
        return n != null ? n.byteValue() : 0;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        Number n = getNumber(columnIndex);
        return n != null ? n.shortValue() : 0;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        Number n = getNumber(columnIndex);
        return n != null ? n.intValue() : 0;
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        Number n = getNumber(columnIndex);
        return n != null ? n.longValue() : 0L;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        Number n = getNumber(columnIndex);
        return n != null ? n.floatValue() : 0.0f;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        Number n = getNumber(columnIndex);
        return n != null ? n.doubleValue() : 0.0;
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getBigDecimal(columnIndex);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (val instanceof byte[]) return (byte[]) val;
        return val.toString().getBytes();
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (val instanceof java.util.Date) {
            return new Date(((java.util.Date) val).getTime());
        }
        try {
            return Date.valueOf(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (val instanceof java.util.Date) {
            return new Time(((java.util.Date) val).getTime());
        }
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (val instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) val).getTime());
        }
        try {
            return Timestamp.valueOf(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(findColumn(columnLabel));
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(findColumn(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(findColumn(columnLabel));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(findColumn(columnLabel));
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(findColumn(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(findColumn(columnLabel));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(findColumn(columnLabel));
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(findColumn(columnLabel));
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public String getCursorName() throws SQLException {
        return null;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new MongoResultSetMetaData(columnNames, columnTypes);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        checkRow();
        if (columnIndex < 1 || columnIndex > columnNames.size()) {
            throw new SQLException("Column index out of bounds: " + columnIndex);
        }
        List<Object> row = rows.get(currentIndex);
        Object val = (columnIndex - 1 < row.size()) ? row.get(columnIndex - 1) : null;
        wasNull = (val == null);
        return val;
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        if (columnLabel != null) {
            Integer idx = columnNameToIndex.get(columnLabel.toLowerCase());
            if (idx != null) {
                return idx;
            }
        }
        throw new SQLException("Column not found: " + columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(findColumn(columnLabel));
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return currentIndex == -1 && !rows.isEmpty();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return currentIndex >= rows.size() && !rows.isEmpty();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return currentIndex == 0 && !rows.isEmpty();
    }

    @Override
    public boolean isLast() throws SQLException {
        return currentIndex == rows.size() - 1 && !rows.isEmpty();
    }

    @Override
    public void beforeFirst() throws SQLException {
        currentIndex = -1;
    }

    @Override
    public void afterLast() throws SQLException {
        currentIndex = rows.size();
    }

    @Override
    public boolean first() throws SQLException {
        if (rows.isEmpty()) return false;
        currentIndex = 0;
        return true;
    }

    @Override
    public boolean last() throws SQLException {
        if (rows.isEmpty()) return false;
        currentIndex = rows.size() - 1;
        return true;
    }

    @Override
    public int getRow() throws SQLException {
        return (currentIndex >= 0 && currentIndex < rows.size()) ? currentIndex + 1 : 0;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (row > 0 && row <= rows.size()) {
            currentIndex = row - 1;
            return true;
        } else if (row < 0 && (rows.size() + row) >= 0) {
            currentIndex = rows.size() + row;
            return true;
        }
        return false;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        int target = currentIndex + rows;
        if (target >= 0 && target < this.rows.size()) {
            currentIndex = target;
            return true;
        }
        return false;
    }

    @Override
    public boolean previous() throws SQLException {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        currentIndex = -1;
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {}

    @Override
    public int getFetchDirection() throws SQLException {
        return FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {}

    @Override
    public int getFetchSize() throws SQLException {
        return rows.size();
    }

    @Override
    public int getType() throws SQLException {
        return TYPE_SCROLL_INSENSITIVE;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() throws SQLException { return false; }

    @Override
    public boolean rowInserted() throws SQLException { return false; }

    @Override
    public boolean rowDeleted() throws SQLException { return false; }

    @Override
    public void updateNull(int columnIndex) throws SQLException {}

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {}

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {}

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {}

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {}

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {}

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {}

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {}

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {}

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {}

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {}

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {}

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {}

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {}

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {}

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {}

    @Override
    public void updateNull(String columnLabel) throws SQLException {}

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {}

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {}

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {}

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {}

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {}

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {}

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {}

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {}

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {}

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {}

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {}

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {}

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {}

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {}

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {}

    @Override
    public void insertRow() throws SQLException {}

    @Override
    public void updateRow() throws SQLException {}

    @Override
    public void deleteRow() throws SQLException {}

    @Override
    public void refreshRow() throws SQLException {}

    @Override
    public void cancelRowUpdates() throws SQLException {}

    @Override
    public void moveToInsertRow() throws SQLException {}

    @Override
    public void moveToCurrentRow() throws SQLException {}

    @Override
    public Statement getStatement() throws SQLException {
        return statement;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException { return null; }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException { return null; }

    @Override
    public Clob getClob(int columnIndex) throws SQLException { return null; }

    @Override
    public Array getArray(int columnIndex) throws SQLException { return null; }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnLabel);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException { return null; }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException { return null; }

    @Override
    public Clob getClob(String columnLabel) throws SQLException { return null; }

    @Override
    public Array getArray(String columnLabel) throws SQLException { return null; }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(columnIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getTime(columnIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(columnLabel);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException { return null; }

    @Override
    public URL getURL(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {}

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {}

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {}

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {}

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {}

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {}

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {}

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {}

    @Override
    public RowId getRowId(int columnIndex) throws SQLException { return null; }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {}

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {}

    @Override
    public int getHoldability() throws SQLException {
        return HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {}

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {}

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {}

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {}

    @Override
    public NClob getNClob(int columnIndex) throws SQLException { return null; }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException { return null; }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException { return null; }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {}

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {}

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException { return null; }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {}

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {}

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {}

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {}

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {}

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {}

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {}

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {}

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        Object obj = getObject(columnIndex);
        if (obj == null) return null;
        return type.cast(obj);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return getObject(findColumn(columnLabel), type);
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

    private Number getNumber(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (val instanceof Number) return (Number) val;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("ResultSet is closed");
        }
    }

    private void checkRow() throws SQLException {
        checkOpen();
        if (currentIndex < 0 || currentIndex >= rows.size()) {
            throw new SQLException("Invalid cursor position: " + currentIndex);
        }
    }
}
