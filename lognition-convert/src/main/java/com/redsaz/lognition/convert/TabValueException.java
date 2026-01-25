package com.redsaz.lognition.convert;

/** Thrown when encountering an exception reading/writing value for TabRecords. */
public sealed class TabValueException extends TabException
    permits TabValueMistypedException, TabValueRequiredException {
  private final TabField<?> schema;

  public TabValueException(String message, TabField<?> schema) {
    super(message);
    this.schema = schema;
  }

  public TabField<?> schema() {
    return schema;
  }
}
