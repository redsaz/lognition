package com.redsaz.lognition.convert;

/** Thrown whenever a value was provided but was the wrong type as specified by the schema. */
public final class TabValueMistypedException extends TabValueException {
  private final Object valueInError;

  public TabValueMistypedException(TabField<?> schema, Object valueInError) {
    super("Expected value of type " + schema + " but got " + valueInError.getClass(), schema);
    this.valueInError = valueInError;
  }

  public Object valueInError() {
    return valueInError;
  }
}
