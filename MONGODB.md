# MongoDB Driver Support

## Changes Made

Added MongoDB JDBC driver recognition to the Helical Insight database connectivity flow.

The MongoDB JDBC driver:

com.helical.mongodb.MongoJdbcDriver

is now recognized by `MongoConnectionFactory`, allowing MongoDB connections to use the existing database connectivity mechanism.

Existing MongoDB configuration entries in `databaseDrivers.properties` are retained and used by the application.

## Configuration

Configure MongoDB as a database connection using the MongoDB JDBC driver:

Driver:
com.helical.mongodb.MongoJdbcDriver

Connection URL:
mongodb://{{hostName}}:{{port}}/{{database}}

Replace the host, port and database values with the MongoDB server details.

For MongoDB Atlas, use the connection details provided by the Atlas cluster and configure authentication/TLS as required by the environment.

## Build

The project was successfully built using:

mvn clean package -DskipTests

The generated application WAR is produced under:

server/presentation/target/

## Verification

MongoDB Atlas connectivity was verified independently using MongoDB Shell.

The source changes were committed and pushed to GitHub.
