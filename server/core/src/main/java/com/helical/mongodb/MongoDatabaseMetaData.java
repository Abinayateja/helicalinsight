package com.helical.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class MongoDatabaseMetaData implements DatabaseMetaData {

    private final MongoConnection connection;
    private final MongoClient mongoClient;
    private final String currentDatabase;

    public MongoDatabaseMetaData(MongoConnection connection, MongoClient mongoClient, String currentDatabase) {
        this.connection = connection;
        this.mongoClient = mongoClient;
        this.currentDatabase = currentDatabase != null ? currentDatabase : "test";
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        String dbName = (catalog != null && !catalog.isEmpty()) ? catalog : currentDatabase;
        MongoDatabase db = mongoClient.getDatabase(dbName);

        List<String> cols = Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS",
                "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION");
        List<Integer> colTypes = Arrays.asList(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR);

        List<List<Object>> rows = new ArrayList<>();
        try {
            MongoIterable<String> collections = db.listCollectionNames();
            for (String colName : collections) {
                if (colName.startsWith("system.")) {
                    continue;
                }
                if (tableNamePattern != null && !tableNamePattern.isEmpty() && !"%".equals(tableNamePattern)) {
                    String regex = tableNamePattern.replace("%", ".*");
                    if (!colName.matches("(?i)" + regex)) {
                        continue;
                    }
                }
                List<Object> row = new ArrayList<>();
                row.add(dbName);       // TABLE_CAT
                row.add(null);         // TABLE_SCHEM
                row.add(colName);      // TABLE_NAME
                row.add("TABLE");      // TABLE_TYPE
                row.add("");           // REMARKS
                row.add(null);
                row.add(null);
                row.add(null);
                row.add(null);
                row.add(null);
                rows.add(row);
            }
        } catch (Exception e) {
            throw new SQLException("Error listing tables for catalog " + dbName + ": " + e.getMessage(), e);
        }

        return new MongoResultSet(null, cols, colTypes, rows);
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        String dbName = (catalog != null && !catalog.isEmpty()) ? catalog : currentDatabase;
        MongoDatabase db = mongoClient.getDatabase(dbName);

        List<String> cols = Arrays.asList(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME",
                "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE",
                "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
                "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE",
                "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN"
        );

        List<Integer> colTypes = Arrays.asList(
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR,
                Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER,
                Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER,
                Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.SMALLINT, Types.VARCHAR, Types.VARCHAR
        );

        List<List<Object>> rows = new ArrayList<>();

        try {
            List<String> targetCollections = new ArrayList<>();
            if (tableNamePattern != null && !tableNamePattern.isEmpty() && !"%".equals(tableNamePattern)) {
                targetCollections.add(tableNamePattern.replaceAll("^[`\"'\\[]+|[`\"'\\]]+$", ""));
            } else {
                for (String c : db.listCollectionNames()) {
                    if (!c.startsWith("system.")) {
                        targetCollections.add(c);
                    }
                }
            }

            for (String colName : targetCollections) {
                MongoCollection<Document> coll = db.getCollection(colName);
                Map<String, Class<?>> fieldTypes = new LinkedHashMap<>();
                // Always ensure _id is present
                fieldTypes.put("_id", ObjectId.class);

                // Sample up to 50 documents to discover fields
                for (Document doc : coll.find().limit(50)) {
                    for (Map.Entry<String, Object> entry : doc.entrySet()) {
                        String field = entry.getKey();
                        Object val = entry.getValue();
                        if (!fieldTypes.containsKey(field) || fieldTypes.get(field) == Object.class) {
                            if (val != null) {
                                fieldTypes.put(field, val.getClass());
                            } else if (!fieldTypes.containsKey(field)) {
                                fieldTypes.put(field, Object.class);
                            }
                        }
                    }
                }

                int position = 1;
                for (Map.Entry<String, Class<?>> entry : fieldTypes.entrySet()) {
                    String fieldName = entry.getKey();
                    if (columnNamePattern != null && !columnNamePattern.isEmpty() && !"%".equals(columnNamePattern)) {
                        String regex = columnNamePattern.replace("%", ".*");
                        if (!fieldName.matches("(?i)" + regex)) {
                            continue;
                        }
                    }

                    int sqlType = getSqlTypeForClass(entry.getValue());
                    String typeName = getSqlTypeNameForType(sqlType);
                    int colSize = (sqlType == Types.VARCHAR) ? 255 : 10;

                    List<Object> row = new ArrayList<>();
                    row.add(dbName);          // TABLE_CAT
                    row.add(null);            // TABLE_SCHEM
                    row.add(colName);         // TABLE_NAME
                    row.add(fieldName);       // COLUMN_NAME
                    row.add(sqlType);         // DATA_TYPE
                    row.add(typeName);        // TYPE_NAME
                    row.add(colSize);         // COLUMN_SIZE
                    row.add(0);               // BUFFER_LENGTH
                    row.add(0);               // DECIMAL_DIGITS
                    row.add(10);              // NUM_PREC_RADIX
                    row.add(DatabaseMetaData.columnNullable); // NULLABLE
                    row.add("");              // REMARKS
                    row.add(null);            // COLUMN_DEF
                    row.add(0);               // SQL_DATA_TYPE
                    row.add(0);               // SQL_DATETIME_SUB
                    row.add(colSize);         // CHAR_OCTET_LENGTH
                    row.add(position++);      // ORDINAL_POSITION
                    row.add("YES");           // IS_NULLABLE
                    row.add(null);
                    row.add(null);
                    row.add(null);
                    row.add(null);
                    row.add("_id".equals(fieldName) ? "YES" : "NO"); // IS_AUTOINCREMENT
                    row.add("NO");            // IS_GENERATEDCOLUMN
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            throw new SQLException("Error introspecting columns: " + e.getMessage(), e);
        }

        return new MongoResultSet(null, cols, colTypes, rows);
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        List<String> cols = Collections.singletonList("TABLE_CAT");
        List<Integer> colTypes = Collections.singletonList(Types.VARCHAR);
        List<List<Object>> rows = new ArrayList<>();
        try {
            for (String db : mongoClient.listDatabaseNames()) {
                rows.add(Collections.singletonList((Object) db));
            }
        } catch (Exception e) {
            rows.add(Collections.singletonList((Object) currentDatabase));
        }
        return new MongoResultSet(null, cols, colTypes, rows);
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        return getSchemas(null, null);
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        List<String> cols = Arrays.asList("TABLE_SCHEM", "TABLE_CATALOG");
        List<Integer> colTypes = Arrays.asList(Types.VARCHAR, Types.VARCHAR);
        List<List<Object>> rows = new ArrayList<>();
        String cat = (catalog != null && !catalog.isEmpty()) ? catalog : currentDatabase;
        rows.add(Arrays.asList((Object) cat, (Object) null));
        return new MongoResultSet(null, cols, colTypes, rows);
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        List<String> cols = Collections.singletonList("TABLE_TYPE");
        List<Integer> colTypes = Collections.singletonList(Types.VARCHAR);
        List<List<Object>> rows = Arrays.asList(
                Collections.singletonList((Object) "TABLE"),
                Collections.singletonList((Object) "VIEW")
        );
        return new MongoResultSet(null, cols, colTypes, rows);
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        List<String> cols = Arrays.asList("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ", "PK_NAME");
        List<Integer> colTypes = Arrays.asList(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.VARCHAR);
        List<List<Object>> rows = new ArrayList<>();
        String dbName = (catalog != null && !catalog.isEmpty()) ? catalog : currentDatabase;
        rows.add(Arrays.asList((Object) dbName, (Object) null, (Object) table, (Object) "_id", (Object) 1, (Object) "PRIMARY"));
        return new MongoResultSet(null, cols, colTypes, rows);
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        return "MongoDB";
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        try {
            Document buildInfo = mongoClient.getDatabase("admin").runCommand(new Document("buildInfo", 1));
            return buildInfo.getString("version");
        } catch (Exception e) {
            return "8.0";
        }
    }

    @Override
    public String getDriverName() throws SQLException {
        return "Helical MongoDB JDBC Driver";
    }

    @Override
    public String getDriverVersion() throws SQLException {
        return "1.0.0";
    }

    @Override
    public int getDriverMajorVersion() {
        return 1;
    }

    @Override
    public int getDriverMinorVersion() {
        return 0;
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        return "\"";
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        return "";
    }

    @Override
    public String getNumericFunctions() throws SQLException { return ""; }

    @Override
    public String getStringFunctions() throws SQLException { return ""; }

    @Override
    public String getSystemFunctions() throws SQLException { return ""; }

    @Override
    public String getTimeDateFunctions() throws SQLException { return ""; }

    @Override
    public String getSearchStringEscape() throws SQLException { return "\\"; }

    @Override
    public String getExtraNameCharacters() throws SQLException { return ""; }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException { return false; }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException { return false; }

    @Override
    public boolean supportsColumnAliasing() throws SQLException { return true; }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException { return true; }

    @Override
    public boolean supportsConvert() throws SQLException { return false; }

    @Override
    public boolean supportsConvert(int fromType, int toType) throws SQLException { return false; }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException { return true; }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException { return false; }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException { return true; }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException { return true; }

    @Override
    public boolean supportsGroupBy() throws SQLException { return true; }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException { return true; }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException { return true; }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException { return true; }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException { return false; }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException { return false; }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException { return false; }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException { return true; }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException { return true; }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException { return false; }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException { return true; }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException { return false; }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException { return false; }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException { return false; }

    @Override
    public boolean supportsOuterJoins() throws SQLException { return true; }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException { return false; }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException { return true; }

    @Override
    public String getSchemaTerm() throws SQLException { return "schema"; }

    @Override
    public String getProcedureTerm() throws SQLException { return "procedure"; }

    @Override
    public String getCatalogTerm() throws SQLException { return "database"; }

    @Override
    public boolean isCatalogAtStart() throws SQLException { return true; }

    @Override
    public String getCatalogSeparator() throws SQLException { return "."; }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException { return true; }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException { return false; }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException { return false; }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException { return false; }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException { return false; }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException { return true; }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException { return false; }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException { return false; }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException { return false; }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException { return false; }

    @Override
    public boolean supportsPositionedDelete() throws SQLException { return false; }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException { return false; }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException { return false; }

    @Override
    public boolean supportsStoredProcedures() throws SQLException { return false; }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException { return true; }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException { return true; }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException { return true; }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException { return false; }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException { return false; }

    @Override
    public boolean supportsUnion() throws SQLException { return true; }

    @Override
    public boolean supportsUnionAll() throws SQLException { return true; }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException { return false; }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException { return false; }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException { return false; }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException { return false; }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException { return 0; }

    @Override
    public int getMaxCharLiteralLength() throws SQLException { return 0; }

    @Override
    public int getMaxColumnNameLength() throws SQLException { return 255; }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException { return 0; }

    @Override
    public int getMaxColumnsInIndex() throws SQLException { return 0; }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException { return 0; }

    @Override
    public int getMaxColumnsInSelect() throws SQLException { return 0; }

    @Override
    public int getMaxColumnsInTable() throws SQLException { return 0; }

    @Override
    public int getMaxConnections() throws SQLException { return 0; }

    @Override
    public int getMaxCursorNameLength() throws SQLException { return 0; }

    @Override
    public int getMaxIndexLength() throws SQLException { return 0; }

    @Override
    public int getMaxSchemaNameLength() throws SQLException { return 255; }

    @Override
    public int getMaxProcedureNameLength() throws SQLException { return 0; }

    @Override
    public int getMaxCatalogNameLength() throws SQLException { return 255; }

    @Override
    public int getMaxRowSize() throws SQLException { return 16777216; }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException { return true; }

    @Override
    public int getMaxStatementLength() throws SQLException { return 0; }

    @Override
    public int getMaxStatements() throws SQLException { return 0; }

    @Override
    public int getMaxTableNameLength() throws SQLException { return 255; }

    @Override
    public int getMaxTablesInSelect() throws SQLException { return 0; }

    @Override
    public int getMaxUserNameLength() throws SQLException { return 255; }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException { return Connection.TRANSACTION_NONE; }

    @Override
    public boolean supportsTransactions() throws SQLException { return false; }

    @Override
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException { return level == Connection.TRANSACTION_NONE; }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException { return false; }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException { return false; }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException { return false; }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException { return false; }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        return emptyResultSet();
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        return emptyResultSet();
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException { return true; }

    @Override
    public boolean allTablesAreSelectable() throws SQLException { return true; }

    @Override
    public String getURL() throws SQLException { return connection.getURL(); }

    @Override
    public String getUserName() throws SQLException { return ""; }

    @Override
    public boolean isReadOnly() throws SQLException { return false; }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException { return false; }

    @Override
    public boolean nullsAreSortedLow() throws SQLException { return true; }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException { return false; }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException { return false; }

    @Override
    public boolean usesLocalFiles() throws SQLException { return false; }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException { return false; }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException { return true; }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException { return false; }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException { return false; }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException { return true; }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException { return true; }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException { return false; }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException { return false; }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException { return true; }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException { return type == ResultSet.TYPE_FORWARD_ONLY || type == ResultSet.TYPE_SCROLL_INSENSITIVE; }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException { return concurrency == ResultSet.CONCUR_READ_ONLY; }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException { return false; }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException { return false; }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException { return false; }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException { return false; }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException { return false; }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException { return false; }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException { return false; }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException { return false; }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException { return false; }

    @Override
    public boolean supportsBatchUpdates() throws SQLException { return false; }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException { return emptyResultSet(); }

    @Override
    public boolean supportsSavepoints() throws SQLException { return false; }

    @Override
    public boolean supportsNamedParameters() throws SQLException { return false; }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException { return false; }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException { return false; }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException { return emptyResultSet(); }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException { return emptyResultSet(); }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException { return emptyResultSet(); }

    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException { return holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT; }

    @Override
    public int getResultSetHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }

    @Override
    public int getDatabaseMajorVersion() throws SQLException { return 8; }

    @Override
    public int getDatabaseMinorVersion() throws SQLException { return 0; }

    @Override
    public int getJDBCMajorVersion() throws SQLException { return 4; }

    @Override
    public int getJDBCMinorVersion() throws SQLException { return 2; }

    @Override
    public int getSQLStateType() throws SQLException { return sqlStateSQL; }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException { return false; }

    @Override
    public boolean supportsStatementPooling() throws SQLException { return false; }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException { return RowIdLifetime.ROWID_UNSUPPORTED; }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException { return false; }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException { return false; }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException { return emptyResultSet(); }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException { return emptyResultSet(); }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException { return emptyResultSet(); }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException { return emptyResultSet(); }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException { return false; }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    private int getSqlTypeForClass(Class<?> clazz) {
        if (clazz == null) return Types.VARCHAR;
        if (String.class.isAssignableFrom(clazz) || ObjectId.class.isAssignableFrom(clazz)) return Types.VARCHAR;
        if (Integer.class.isAssignableFrom(clazz)) return Types.INTEGER;
        if (Long.class.isAssignableFrom(clazz)) return Types.BIGINT;
        if (Double.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) return Types.DOUBLE;
        if (Boolean.class.isAssignableFrom(clazz)) return Types.BOOLEAN;
        if (Date.class.isAssignableFrom(clazz)) return Types.TIMESTAMP;
        return Types.VARCHAR;
    }

    private String getSqlTypeNameForType(int sqlType) {
        switch (sqlType) {
            case Types.INTEGER: return "INTEGER";
            case Types.BIGINT: return "BIGINT";
            case Types.DOUBLE: return "DOUBLE";
            case Types.BOOLEAN: return "BOOLEAN";
            case Types.TIMESTAMP: return "TIMESTAMP";
            default: return "VARCHAR";
        }
    }

    private ResultSet emptyResultSet() {
        return new MongoResultSet(null, Collections.<String>emptyList(), Collections.<Integer>emptyList(), Collections.<List<Object>>emptyList());
    }
}
