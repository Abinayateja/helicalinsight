package com.helical.mongodb;

import org.junit.Test;

import java.sql.*;

/**
 * MongoDriverManualTest
 *
 * This is NOT a real automated unit test (no fixed assertions) — it's a manual,
 * one-off check to prove the MongoDB driver actually works end-to-end:
 * Driver -> Connection -> Statement -> ResultSet, against a real database.
 *
 * HOW TO USE:
 * 1. Replace MONGO_URL below with your real connection string.
 *    - Local MongoDB:      mongodb://localhost:27017/yourDbName
 *    - MongoDB Atlas:      mongodb+srv://<user>:<password>@<cluster-url>/yourDbName
 * 2. Replace COLLECTION_NAME with a real collection name in that database.
 * 3. Run just this test:
 *      mvn test -pl core "-Dtest=MongoDriverManualTest" -DfailIfNoTests=false
 * 4. Read the console output.
 *
 * Delete this file once you're done — it's a throwaway verification script,
 * not part of the permanent test suite.
 */
public class MongoDriverManualTest {

    // ---- EDIT THESE TWO LINES ----
    private static final String MONGO_URL = "mongodb://gaddamabinayateja_db_user:Abinayateja1974@ac-dpsko55-shard-00-00.3itgt0h.mongodb.net:27017,ac-dpsko55-shard-00-01.3itgt0h.mongodb.net:27017,ac-dpsko55-shard-00-02.3itgt0h.mongodb.net:27017/?ssl=true&replicaSet=atlas-11qlzi-shard-0&authSource=admin&appName=Cluster0";
    private static final String COLLECTION_NAME = "histories";
    // -------------------------------

    @Test
    public void testDriverConnectsAndQueries() {
        try {
            System.out.println("STEP 1: Loading driver class...");
            Class.forName("com.helical.mongodb.MongoJdbcDriver");
            System.out.println("  -> OK, driver class loaded and registered.");

            System.out.println("STEP 2: Opening connection to: " + MONGO_URL);
            Connection conn = DriverManager.getConnection(MONGO_URL);
            System.out.println("  -> OK, connection opened. isClosed=" + conn.isClosed());

            System.out.println("STEP 3: Checking connection is valid (ping)...");
            boolean valid = conn.isValid(5);
            System.out.println("  -> isValid: " + valid);

            System.out.println("STEP 4: Reading database metadata...");
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("  -> Connected to database: " + meta.getURL());

            System.out.println("STEP 5: Running a query against collection '" + COLLECTION_NAME + "'...");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + COLLECTION_NAME + " LIMIT 5");

            int colCount = rs.getMetaData().getColumnCount();
            System.out.println("  -> Query ran. Column count: " + colCount);

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                StringBuilder row = new StringBuilder("  Row " + rowCount + ": ");
                for (int i = 1; i <= colCount; i++) {
                    row.append(rs.getMetaData().getColumnName(i)).append("=").append(rs.getObject(i)).append(" | ");
                }
                System.out.println(row);
            }
            System.out.println("  -> Total rows returned: " + rowCount);

            rs.close();
            stmt.close();
            conn.close();

            System.out.println();
            System.out.println("========================================");
            System.out.println("RESULT: DRIVER WORKS END-TO-END.");
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("RESULT: FAILED -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("========================================");
            e.printStackTrace();
        }
    }
}