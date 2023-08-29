package net.elytrium.dbm;

import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.Map;

public class Config extends YamlSerializable {

  public static final Config INSTANCE = new Config();

  public Config() {
    super(SerializerConfig.DEFAULT);
  }

  public String token = "";

  public MySQLRoot mysqlRoot = new MySQLRoot();

  public static class MySQLRoot {

    public String username = "root";

    public String password = "";

    public String parameters = "?autoReconnect=true&initialTimeout=1&useSSL=false";
  }

  public Map<String, NamedQuery> presets = Map.of(
      "limboauth", new NamedQuery("LimboAuth", "SELECT NICKNAME, LOWERCASENICKNAME, HASH, IP, TOTPTOKEN, REGDATE, UUID, PREMIUMUUID, LOGINIP, LOGINDATE, ISSUEDTIME FROM AUTH"),
      "jpremium", new NamedQuery("JPremium", "SELECT lastNickname, LOWER(lastNickname), IF(hashedPassword IS NULL, \"\", IF(premiumId IS NULL, hashedPassword, \"\")), COALESCE(firstAddress, \"\"), \"\", (UNIX_TIMESTAMP(firstSeen) * 1000), CONCAT(SUBSTR(uniqueId, 1, 8), '-', SUBSTR(uniqueId, 9, 4), '-', SUBSTR(uniqueId, 13, 4), '-',  SUBSTR(uniqueId, 17, 4), '-', SUBSTR(uniqueId, 21)), IF(premiumId IS NULL, \"\", CONCAT(SUBSTR(premiumId, 1, 8), '-', SUBSTR(premiumId, 9, 4), '-', SUBSTR(premiumId, 13, 4), '-',  SUBSTR(premiumId, 17, 4), '-', SUBSTR(premiumId, 21))), NULL, NULL, NULL FROM user_profiles WHERE lastNickname IS NOT NULL"),
      "nlogin", new NamedQuery("nLogin", "SELECT realname, name, COALESCE(password, \"\"), COALESCE(address, \"\"), \"\", 0, CONCAT(SUBSTR(uniqueId, 1, 8), '-', SUBSTR(uniqueId, 9, 4), '-', SUBSTR(uniqueId, 13, 4), '-',  SUBSTR(uniqueId, 17, 4), '-', SUBSTR(uniqueId, 21)), IF(premiumId IS NULL, \"\", CONCAT(SUBSTR(premiumId, 1, 8), '-', SUBSTR(premiumId, 9, 4), '-', SUBSTR(premiumId, 13, 4), '-',  SUBSTR(premiumId, 17, 4), '-', SUBSTR(premiumId, 21))), NULL, NULL, NULL FROM table"),
      "moonvkauth", new NamedQuery("MoonVKAuth", "SELECT username, LOWER(username), IF(password IS NULL, \"\", IF(premium = 1, \"\", password)), COALESCE(register_ip, \"\"), \"\", reg_date, \"\", \"\", NULL, NULL, NULL FROM auth WHERE username IS NOT NULL"),
      "authme", new NamedQuery("AuthMe", "SELECT realname, username, COALESCE(password, \"\"), COALESCE(ip, \"\"), COALESCE(totp, \"\"), regdate, \"\", \"\", NULL, NULL, NULL FROM authme WHERE username IS NOT NULL"),
      "dba", new NamedQuery("DBA", "SELECT name, LOWER(name), IF(password IS NOT NULL, CONCAT(\"SHA512$\", salt, \"$\", password), \"\"), COALESCE(reg_ip, \"\"), \"\", (UNIX_TIMESTAMP(firstjoin) * 1000), uuid, \"\", NULL, NULL, NULL FROM playerdata WHERE name IS NOT NULL")
  );

  public static class NamedQuery {

    public String name = "";
    public String query = "";

    public NamedQuery(String name, String query) {
      this.name = name;
      this.query = query;
    }

    public NamedQuery() {
    }
  }
}
