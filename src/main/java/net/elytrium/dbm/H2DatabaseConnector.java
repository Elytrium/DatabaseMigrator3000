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

public class H2DatabaseConnector implements DatabaseConnector {

  private static final Map<Connection, Path> TEMP_DATABASES = new ConcurrentHashMap<>();
  private static final Path DIRECTORY;

  static {
    try {
      System.out.println("Initializing H2 connector...");
      Class.forName("org.h2.Driver");
      DIRECTORY = Files.createTempDirectory("dbm.h2");
    } catch (IOException | ClassNotFoundException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Override
  public Connection createTempDatabase() throws SQLException {
    String databaseName = Long.toHexString(System.currentTimeMillis());
    String path = DIRECTORY.resolve(databaseName).toAbsolutePath().toString();

    Connection connection = DriverManager.getConnection("jdbc:h2:" + path + ";TRACE_LEVEL_FILE=4");
    Path databasePath = DIRECTORY.resolve(databaseName + ".mv.db");
    TEMP_DATABASES.put(connection, databasePath);

    return connection;
  }

  @Override
  public Connection createConnection(File file) throws SQLException {
    String path = file.getAbsolutePath();
    if (path.endsWith(".mv.db")) {
      path = path.substring(0, path.length() - 6);
    }

    return DriverManager.getConnection("jdbc:h2:" + path + ";TRACE_LEVEL_FILE=4");
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
    return "H2";
  }

  @Override
  public String getMigratedFilename() {
    return "limboauth-v2.mv.db";
  }
}
