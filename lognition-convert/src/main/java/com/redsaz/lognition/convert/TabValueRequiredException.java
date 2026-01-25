package com.redsaz.lognition.convert;

/** Thrown whenever the schema requires a value, but null was provided. */
public final class TabValueRequiredException extends TabValueException {
  public TabValueRequiredException(TabField<?> schema) {
    super("Not optional, but was null: " + schema, schema);
  }
}
