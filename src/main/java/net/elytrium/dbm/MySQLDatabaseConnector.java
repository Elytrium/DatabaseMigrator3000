package net.elytrium.dbm;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class MySQLDatabaseConnector implements DatabaseConnector {

  private static final Connection ROOT_CONNECTION;

  static {
    try {
      System.out.println("Initializing MySQL connector...");
      Class.forName("com.mysql.cj.jdbc.Driver");
      String password = Config.INSTANCE.mysqlRoot.password;
      password = password == null || password.isBlank() ? null : password;
      ROOT_CONNECTION = DriverManager.getConnection("jdbc:mysql://localhost/mysql" + Config.INSTANCE.mysqlRoot.parameters, Config.INSTANCE.mysqlRoot.username, password);
      System.out.println("Connected to MySQL.");
    } catch (ClassNotFoundException | SQLException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public MySQLDeleteOnCloseConnection createDatabase() throws SQLException {
    String name = "tmp" + Long.toHexString(System.currentTimeMillis());
    String password = UUID.randomUUID().toString();

    Statement statement = ROOT_CONNECTION.createStatement();
    statement.execute("CREATE DATABASE " + name);
    statement.execute("CREATE USER '" + name + "'@'localhost' IDENTIFIED BY '" + password + "'");
    statement.execute("GRANT ALL PRIVILEGES ON " + name + ".* TO '" + name + "'@'localhost'");
    statement.execute("FLUSH PRIVILEGES");
    statement.close();

    return new MySQLDeleteOnCloseConnection(
        DriverManager.getConnection("jdbc:mysql://localhost/" + name, name, password),
        ROOT_CONNECTION, name, password, name
    );
  }

  @Override
  public Connection createTempDatabase() throws SQLException {
    return createDatabase();
  }

  @Override
  public Connection createConnection(File file) throws SQLException, IOException {
    MySQLDeleteOnCloseConnection connection = createDatabase();

    String[] command = new String[]{"/bin/bash", "-c", "mysql -u" + connection.getUsername() + " -p" + connection.getPassword() + " " + connection.getDatabase() + " < " + file.getAbsolutePath()};
    Process runtimeProcess = Runtime.getRuntime().exec(command);

    try {
      runtimeProcess.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return connection;
  }

  @Override
  public byte[] finalizeDatabase(Connection connection) throws SQLException, IOException {
    MySQLDeleteOnCloseConnection mysqlConnection = (MySQLDeleteOnCloseConnection) connection;

    String[] command = new String[]{"/bin/bash", "-c", "mysqldump --skip-comments -u" + mysqlConnection.getUsername() + " -p" + mysqlConnection.getPassword() + " " + mysqlConnection.getDatabase()};
    Process runtimeProcess = Runtime.getRuntime().exec(command);
    byte[] output = runtimeProcess.getInputStream().readAllBytes();

    closeConnection(connection);
    return output;
  }

  @Override
  public String getConnectorName() {
    return "MySQL";
  }

  @Override
  public String getMigratedFilename() {
    return "dump.sql";
  }
}
