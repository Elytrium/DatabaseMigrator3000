package net.elytrium.dbm;

public class NamedDatabaseBinary {

  private final String inputType;
  private final String outputType;
  private final String queryType;
  private final String filename;
  private final byte[] data;

  public NamedDatabaseBinary(String inputType, String outputType, String queryType, String filename, byte[] data) {
    this.inputType = inputType;
    this.outputType = outputType;
    this.queryType = queryType;
    this.filename = filename;
    this.data = data;
  }

  public String getInputType() {
    return inputType;
  }

  public String getOutputType() {
    return outputType;
  }

  public String getQueryType() {
    return queryType;
  }

  public String getFilename() {
    return filename;
  }

  public byte[] getData() {
    return data;
  }
}
