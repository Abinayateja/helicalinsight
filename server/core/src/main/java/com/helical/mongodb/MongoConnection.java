package com.helical.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * MongoConnection
 *
 * Implements java.sql.Connection for MongoDB.
 * This is the piece that was missing: it opens the real MongoClient,
 * picks the target database, and hands out Statement / DatabaseMetaData
 * objects that the rest of the com.helical.mongodb package already expects.
 *
 * Created by <this constructor> whenever MongoJdbcDriver.connect(url, info) is called.
 */
public class MongoConnection implements Connection {

    private final String url;
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private boolean closed = false;

    public MongoConnection(String url, Properties info) throws SQLException {
        this.url = url;
        try {
            MongoClientURI uri = new MongoClientURI(url);
            this.mongoClient = new MongoClient(uri);
            String dbName = uri.getDatabase();
            if (dbName == null || dbName.trim().isEmpty()) {
                // Some valid connection strings (e.g. Atlas's non-SRV multi-host format)
                // omit the database name from the URI itself. Rather than failing,
                // fall back to a sensible default so the connection still succeeds.
                dbName = "test";
            }
            this.database = mongoClient.getDatabase(dbName);
        } catch (Exception e) {
            throw new SQLException("Failed to connect to MongoDB at [" + url + "]: " + e.getMessage(), e);
        }
    }

    // ---- Used directly by MongoStatement / MongoPreparedStatement / MongoDatabaseMetaData ----

    public MongoDatabase getDatabase() {
        return database;
    }

    public String getURL() throws SQLException {
        return url;
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkClosed();
        return new MongoStatement(this, database);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkClosed();
        return new MongoPreparedStatement(this, database, sql);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkClosed();
        return new MongoDatabaseMetaData(this, mongoClient, database.getName());
    }

    @Override
    public void close() throws SQLException {
        if (!closed) {
            mongoClient.close();
            closed = true;
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (closed) return false;
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("This MongoConnection is already closed.");
    }

    // ---- Transactions: MongoDB's single-document writes are atomic by default,
    // so for a read-focused reporting driver these are safe no-ops rather than errors. ----

    @Override public void setAutoCommit(boolean autoCommit) {}
    @Override public boolean getAutoCommit() { return true; }
    @Override public void commit() {}
    @Override public void rollback() {}
    @Override public void rollback(Savepoint savepoint) {}
    @Override public Savepoint setSavepoint() { return null; }
    @Override public Savepoint setSavepoint(String name) { return null; }
    @Override public void releaseSavepoint(Savepoint savepoint) {}
    @Override public void setTransactionIsolation(int level) {}
    @Override public int getTransactionIsolation() { return Connection.TRANSACTION_NONE; }

    // ---- Simple state Helical Insight / JDBC callers may check ----

    @Override public void setReadOnly(boolean readOnly) {}
    @Override public boolean isReadOnly() { return false; }
    @Override public void setCatalog(String catalog) {}
    @Override public String getCatalog() throws SQLException { return database.getName(); }
    @Override public void setSchema(String schema) {}
    @Override public String getSchema() throws SQLException { return database.getName(); }
    @Override public void setHoldability(int holdability) {}
    @Override public int getHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public String nativeSQL(String sql) { return sql; }

    // ---- Statement/PreparedStatement overloads: delegate to the basic versions above.
    // This driver doesn't use scrollable/updatable result sets, so the extra flags are ignored. ----

    @Override public Statement createStatement(int rsType, int rsConcurrency) throws SQLException { return createStatement(); }
    @Override public Statement createStatement(int rsType, int rsConcurrency, int rsHoldability) throws SQLException { return createStatement(); }
    @Override public PreparedStatement prepareStatement(String sql, int rsType, int rsConcurrency) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int rsType, int rsConcurrency, int rsHoldability) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return prepareStatement(sql); }

    // ---- Not applicable to MongoDB / not needed for this driver's read-focused use case ----

    @Override public CallableStatement prepareCall(String sql) throws SQLException { throw new SQLFeatureNotSupportedException("Stored procedures are not applicable to MongoDB."); }
    @Override public CallableStatement prepareCall(String sql, int a, int b) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public CallableStatement prepareCall(String sql, int a, int b, int c) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Map<String, Class<?>> getTypeMap() { return java.util.Collections.emptyMap(); }
    @Override public void setTypeMap(Map<String, Class<?>> map) {}
    @Override public Clob createClob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Blob createBlob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public NClob createNClob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public SQLXML createSQLXML() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClientInfo(String name, String value) {}
    @Override public void setClientInfo(Properties properties) {}
    @Override public String getClientInfo(String name) { return null; }
    @Override public Properties getClientInfo() { return new Properties(); }
    @Override public void abort(Executor executor) throws SQLException { close(); }
    @Override public void setNetworkTimeout(Executor executor, int milliseconds) {}
    @Override public int getNetworkTimeout() { return 0; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface);
    }
    @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }
}
