package com.helical.mongodb;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * MongoJdbcDriver
 *
 * Implements java.sql.Driver — this is the entry point.
 * This is the second missing piece: databaseDrivers.properties already
 * points at "com.helical.mongodb.MongoJdbcDriver", but that class didn't
 * exist yet, so Class.forName(...) had nothing to find.
 *
 * The static block below is what makes Class.forName("com.helical.mongodb.MongoJdbcDriver")
 * actually register this driver with DriverManager — the same mechanism every
 * JDBC driver (MySQL, Postgres, etc.) uses.
 */
public class MongoJdbcDriver implements Driver {

    static {
        try {
            DriverManager.registerDriver(new MongoJdbcDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register MongoJdbcDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null; // per JDBC spec: return null, not an exception, if this driver doesn't handle the URL
        }
        return new MongoConnection(url, info);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && (url.startsWith("mongodb://") || url.startsWith("mongodb+srv://"));
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        // Honest answer: MongoDB isn't SQL, so this driver only supports a SELECT
        // subset (via MongoSqlParser) rather than the full SQL standard JDBC compliance requires.
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("java.util.logging is not used by this driver.");
    }
}