package com.redsaz.lognition.convert;

/** Thrown when encountering an exception reading/writing value for TabRecords. */
public sealed class TabValueException extends TabException
    permits TabValueMistypedException, TabValueRequiredException {
  private final TabSchema<?> schema;

  public TabValueException(String message, TabSchema<?> schema) {
    super(message);
    this.schema = schema;
  }

  public TabSchema<?> schema() {
    return schema;
  }
}
