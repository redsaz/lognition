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

  /**
   * Returns a required, null, or defval opt depending on the params
   *
   * @param isOptional if true, then the result is either a null or defval. If false, result is
   *     required.
   * @param valueIfOptional If isOptional is true, then use this for the default value. May be null.
   * @return Required, Null, or DefVal.
   */
  @SuppressWarnings("unchecked")
  static <U> Opt<U> of(boolean isOptional, U valueIfOptional) {
    if (isOptional) {
      return of(valueIfOptional);
    }
    return (Required<U>) REQUIRED;
  }

  /**
   * Returns either a default value opt or a null opt.
   *
   * @param defVal the default value, can be null.
   * @return a DefVal or a Null opt, depending on the param value.
   */
  @SuppressWarnings("unchecked")
  static <U> Opt<U> of(U defVal) {
    if (defVal == null) {
      return (Opt.Null<U>) NULL;
    }
    return new DefVal<>(defVal);
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
