package com.helical.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class MongoStatement implements Statement {

    protected final MongoConnection connection;
    protected final MongoDatabase database;
    protected ResultSet currentResultSet;
    protected boolean closed = false;

    public MongoStatement(MongoConnection connection, MongoDatabase database) {
        this.connection = connection;
        this.database = database;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();
        MongoSqlParser.ParsedQuery parsed = MongoSqlParser.parse(sql);

        if (parsed.isSelectOne) {
            List<String> cols = Collections.singletonList("1");
            List<Integer> types = Collections.singletonList(Types.INTEGER);
            List<List<Object>> rows = Collections.singletonList(Collections.singletonList((Object) 1));
            this.currentResultSet = new MongoResultSet(this, cols, types, rows);
            return this.currentResultSet;
        }

        if (parsed.collection == null || parsed.collection.isEmpty()) {
            // Check if ping or test query
            if (sql.toLowerCase().contains("ping") || sql.toLowerCase().contains("select 1")) {
                List<String> cols = Collections.singletonList("1");
                List<Integer> types = Collections.singletonList(Types.INTEGER);
                List<List<Object>> rows = Collections.singletonList(Collections.singletonList((Object) 1));
                this.currentResultSet = new MongoResultSet(this, cols, types, rows);
                return this.currentResultSet;
            }
            throw new SQLException("Could not determine MongoDB collection name from SQL: " + sql);
        }

        MongoCollection<Document> collection = database.getCollection(parsed.collection);

        if (parsed.isCount) {
            long count = collection.countDocuments(parsed.filter);
            List<String> cols = Collections.singletonList("count");
            List<Integer> types = Collections.singletonList(Types.BIGINT);
            List<List<Object>> rows = Collections.singletonList(Collections.singletonList((Object) count));
            this.currentResultSet = new MongoResultSet(this, cols, types, rows);
            return this.currentResultSet;
        }

        FindIterable<Document> iterable = collection.find(parsed.filter);
        if (!parsed.sort.isEmpty()) {
            iterable.sort(parsed.sort);
        }
        if (parsed.limit > 0) {
            iterable.limit(parsed.limit);
        } else {
            iterable.limit(1000); // default safety limit for BI queries
        }

        List<Document> documents = new ArrayList<>();
        for (Document doc : iterable) {
            documents.add(doc);
        }

        // Determine column list
        List<String> columnNames = new ArrayList<>();
        List<Integer> columnTypes = new ArrayList<>();

        if (parsed.projectedColumns != null && !parsed.projectedColumns.isEmpty()) {
            columnNames.addAll(parsed.projectedColumns);
            for (String col : columnNames) {
                columnTypes.add(inferTypeFromDocs(documents, col));
            }
        } else {
            Set<String> fieldSet = new LinkedHashSet<>();
            fieldSet.add("_id");
            for (Document doc : documents) {
                fieldSet.addAll(doc.keySet());
            }
            columnNames.addAll(fieldSet);
            for (String col : columnNames) {
                columnTypes.add(inferTypeFromDocs(documents, col));
            }
        }

        List<List<Object>> rows = new ArrayList<>();
        for (Document doc : documents) {
            List<Object> row = new ArrayList<>();
            for (String col : columnNames) {
                Object val = doc.get(col);
                if (val instanceof ObjectId) {
                    val = ((ObjectId) val).toHexString();
                } else if (val instanceof Document) {
                    val = ((Document) val).toJson();
                } else if (val instanceof List) {
                    val = val.toString();
                }
                row.add(val);
            }
            rows.add(row);
        }

        this.currentResultSet = new MongoResultSet(this, columnNames, columnTypes, rows);
        return this.currentResultSet;
    }

    private int inferTypeFromDocs(List<Document> docs, String field) {
        if ("_id".equals(field)) return Types.VARCHAR;
        for (Document doc : docs) {
            Object val = doc.get(field);
            if (val != null) {
                if (val instanceof Integer) return Types.INTEGER;
                if (val instanceof Long) return Types.BIGINT;
                if (val instanceof Double || val instanceof Float) return Types.DOUBLE;
                if (val instanceof Boolean) return Types.BOOLEAN;
                if (val instanceof Date) return Types.TIMESTAMP;
                return Types.VARCHAR;
            }
        }
        return Types.VARCHAR;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        checkOpen();
        return 0;
    }

    @Override
    public void close() throws SQLException {
        this.closed = true;
        if (currentResultSet != null) {
            currentResultSet.close();
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException { return 0; }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {}

    @Override
    public int getMaxRows() throws SQLException { return 0; }

    @Override
    public void setMaxRows(int max) throws SQLException {}

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {}

    @Override
    public int getQueryTimeout() throws SQLException { return 0; }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {}

    @Override
    public void cancel() throws SQLException {}

    @Override
    public SQLWarning getWarnings() throws SQLException { return null; }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public void setCursorName(String name) throws SQLException {}

    @Override
    public boolean execute(String sql) throws SQLException {
        this.currentResultSet = executeQuery(sql);
        return true;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return currentResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return -1;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {}

    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {}

    @Override
    public int getFetchSize() throws SQLException { return 100; }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    @Override
    public void addBatch(String sql) throws SQLException {}

    @Override
    public void clearBatch() throws SQLException {}

    @Override
    public int[] executeBatch() throws SQLException { return new int[0]; }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException { return false; }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return new MongoResultSet(this, Collections.<String>emptyList(), Collections.<Integer>emptyList(), Collections.<List<Object>>emptyList());
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return 0; }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return 0; }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException { return 0; }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return execute(sql);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {}

    @Override
    public boolean isPoolable() throws SQLException { return false; }

    @Override
    public void closeOnCompletion() throws SQLException {}

    @Override
    public boolean isCloseOnCompletion() throws SQLException { return false; }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    protected void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Statement is closed");
        }
    }
}
