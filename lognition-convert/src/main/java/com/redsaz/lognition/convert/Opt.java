package com.redsaz.lognition.convert;

public sealed interface Opt<T> permits Opt.DefVal, Opt.Null, Opt.Required {

  boolean isOptional();

  default boolean isRequired() {
    return !isOptional();
  }

  /**
   * @return if the field is required, returns null, otherwise if optional returns the value to use
   *     instead (which could be null).
   */
  default T defVal() {
    return null;
  }

  Opt.Null<Object> NULL = new Null<>();
  Opt.Required<Object> REQUIRED = new Required<>();

  @SuppressWarnings("unchecked")
  static <U> Opt<U> of(boolean isOptional, U valueIfOptional) {
    if (isOptional) {
      if (valueIfOptional == null) {
        return (Opt.Null<U>) NULL;
      }
    }
    return (Required<U>) REQUIRED;
  }

  @SuppressWarnings("unchecked")
  static <U> Opt.Null<U> nullOpt() {
    return (Opt.Null<U>) NULL;
  }

  @SuppressWarnings("unchecked")
  static <U> Opt.Required<U> required() {
    return (Opt.Required<U>) REQUIRED;
  }

  record Required<T>() implements Opt<T> {
    @Override
    public boolean isOptional() {
      return false;
    }
  }

  record Null<T>() implements Opt<T> {
    @Override
    public boolean isOptional() {
      return true;
    }
  }

  record DefVal<T>(T defVal) implements Opt<T> {
    @Override
    public boolean isOptional() {
      return true;
    }
  }
}
