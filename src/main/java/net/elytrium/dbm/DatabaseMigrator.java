package net.elytrium.dbm;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

public class DatabaseMigrator {

  public static final Map<String, DatabaseConnector> DATABASE_CONNECTORS = Map.of(
      "h2", new H2DatabaseConnector(),
      "sqlite", new SQLiteDatabaseConnector(),
      "mysql", new MySQLDatabaseConnector()
  );

  public static NamedDatabaseBinary migrate(File inputFile, String inputType, String outputType, String query) throws SQLException, IOException {
    Config.NamedQuery namedQuery = Config.INSTANCE.presets.get(query.toLowerCase(Locale.ROOT));
    if (namedQuery == null) {
      namedQuery = new Config.NamedQuery("Custom Query", query);
    }

    DatabaseConnector inputConnector = DATABASE_CONNECTORS.get(inputType);
    DatabaseConnector outputConnector = DATABASE_CONNECTORS.get(outputType);

    if (inputConnector == null || outputConnector == null) {
      throw new IllegalArgumentException("Тип бд не поддерживается: " + (inputConnector == null ? inputType : outputType));
    }

    try (Connection outputConnection = outputConnector.createTempDatabase();
         Connection inputConnection = inputConnector.createConnection(inputFile)) {

      ResultSet set = inputConnector.importData(inputConnection, namedQuery.query);
      outputConnector.exportData(outputConnection, set);
      inputConnector.closeSet(set);

      inputConnector.closeConnection(inputConnection);

      byte[] data = outputConnector.finalizeDatabase(outputConnection);
      return new NamedDatabaseBinary(
          inputConnector.getConnectorName(),
          outputConnector.getConnectorName(),
          namedQuery.name,
          outputConnector.getMigratedFilename(),
          data
      );
    }
  }
}
