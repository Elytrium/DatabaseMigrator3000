package net.elytrium.dbm;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public interface DatabaseConnector {

  Connection createTempDatabase() throws SQLException;

  Connection createConnection(File file) throws SQLException, IOException;

  default ResultSet importData(Connection connection, String sql) throws SQLException {
    return connection.createStatement().executeQuery(sql);
  }

  default void exportData(Connection connection, ResultSet set) throws SQLException {
    Statement statement = connection.createStatement();
    statement.executeUpdate("CREATE TABLE IF NOT EXISTS AUTH " +
        "(NICKNAME VARCHAR(255) NOT NULL," +
        " LOWERCASENICKNAME VARCHAR(255)," +
        " HASH VARCHAR(255) NOT NULL," +
        " IP VARCHAR(255)," +
        " TOTPTOKEN VARCHAR(255)," +
        " REGDATE BIGINT," +
        " UUID VARCHAR(255)," +
        " PREMIUMUUID VARCHAR(255)," +
        " LOGINIP VARCHAR(255)," +
        " LOGINDATE BIGINT," +
        " ISSUEDTIME BIGINT," +
        " PRIMARY KEY (LOWERCASENICKNAME))");
    statement.close();

    PreparedStatement insert = connection.prepareStatement("INSERT INTO AUTH (NICKNAME, LOWERCASENICKNAME, HASH, IP, TOTPTOKEN, REGDATE, UUID, PREMIUMUUID, LOGINIP, LOGINDATE, ISSUEDTIME) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    while (set.next()) {
      insert.setString(1, set.getString(1));
      insert.setString(2, set.getString(2));
      insert.setString(3, set.getString(3));
      insert.setString(4, set.getString(4));
      insert.setString(5, set.getString(5));
      insert.setBigDecimal(6, set.getBigDecimal(6));
      insert.setString(7, set.getString(7));
      insert.setString(8, set.getString(8));
      insert.setString(9, set.getString(9));
      insert.setBigDecimal(10, set.getBigDecimal(10));
      insert.setBigDecimal(11, set.getBigDecimal(11));
      insert.executeUpdate();
    }

    insert.close();
  }

  default void closeSet(ResultSet set) throws SQLException {
    Statement statement = set.getStatement();
    set.close();
    statement.close();
  }

  byte[] finalizeDatabase(Connection connection) throws SQLException, IOException;

  default void closeConnection(Connection connection) throws SQLException {
    connection.close();
  }

  String getConnectorName();

  String getMigratedFilename();
}
