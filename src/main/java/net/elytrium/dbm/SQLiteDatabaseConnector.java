package net.elytrium.dbm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteDatabaseConnector implements DatabaseConnector {

  private static final Map<Connection, Path> TEMP_DATABASES = new ConcurrentHashMap<>();
  private static final Path DIRECTORY;

  static {
    try {
      System.out.println("Initializing SQLite connector...");
      Class.forName("org.sqlite.JDBC");
      DIRECTORY = Files.createTempDirectory("dbm.sqlite");
    } catch (IOException | ClassNotFoundException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Override
  public Connection createTempDatabase() throws SQLException {
    String databaseName = Long.toHexString(System.currentTimeMillis()) + ".db";
    Path path = DIRECTORY.resolve(databaseName).toAbsolutePath();

    Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    TEMP_DATABASES.put(connection, path);

    return connection;
  }

  @Override
  public Connection createConnection(File file) throws SQLException {
    return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
  }

  @Override
  public byte[] finalizeDatabase(Connection connection) throws SQLException, IOException {
    Path path = TEMP_DATABASES.get(connection);
    closeConnection(connection);
    byte[] data = Files.readAllBytes(path);
    Files.deleteIfExists(path);
    return data;
  }

  @Override
  public String getConnectorName() {
    return "SQLite";
  }

  @Override
  public String getMigratedFilename() {
    return "limboauth.db";
  }
}
