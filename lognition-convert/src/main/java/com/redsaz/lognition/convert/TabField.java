package com.redsaz.lognition.convert;

import java.util.Optional;
import org.apache.avro.Schema;

public sealed interface TabField<T>
    permits TabField.StrF,
        TabField.IntF,
        TabField.LongF,
        TabField.FloatF,
        TabField.DoubleF,
        TabField.BooleanF {

  String name();

  /**
   * @return true if the value does not have to be provided, and the opt value can be used.
   */
  default boolean isOptional() {
    return opt().isOptional();
  }

  /** Opposite of isOptional. */
  default boolean isRequired() {
    return opt().isRequired();
  }

  Opt<T> opt();

  /**
   * @return the default value to use if one was not provided from a source. May be null if required
   *     or if the default value is null.
   */
  default Object defVal() {
    return opt().defVal();
  }

  static <U> TabField<U> fromAvro(Schema.Field field) {

    return (TabField<U>) switch (field.schema().getType()) {
      case RECORD ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be records.");
      case ENUM ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be enums.");
      case ARRAY ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be arrays.");
      case MAP ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be maps.");
      case UNION -> {
        throw new IllegalArgumentException(
            "Tabular values (\"" + field.name() + "\") cannot be unions.");
      }
      case FIXED ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be fixeds.");
      case STRING -> new StrF(field.name(), Opt.of(field.hasDefaultValue(), (String) field.defaultVal()));
      case BYTES ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be bytes.");
      case INT -> new IntF(field.name(), Opt.of(field.hasDefaultValue(), (Integer) field.defaultVal()));
      case LONG -> new LongF(field.name(), Opt.of(field.hasDefaultValue(), (Long) field.defaultVal()));
      case FLOAT -> new FloatF(field.name(), Opt.of(field.hasDefaultValue(), (Float) field.defaultVal()));
      case DOUBLE -> new DoubleF(field.name(), Opt.of(field.hasDefaultValue(), (Double) field.defaultVal()));
      case BOOLEAN -> new BooleanF(field.name(), Opt.of(field.hasDefaultValue(), (Boolean) field.defaultVal()));
      case NULL ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
    };
  }

  record StrF(String name, Opt<String> opt) implements TabField<String> {
    public static StrF optional(String name) {
      return new StrF(name, Opt.nullOpt());
    }
  }

  record IntF(String name, Opt<Integer> opt) implements TabField<Integer> {
    public static IntF optional(String name) {
      return new IntF(name, Opt.nullOpt());
    }
  }

  record LongF(String name, Opt<Long> opt) implements TabField<Long> {
    public static LongF optional(String name) {
      return new LongF(name, Opt.nullOpt());
    }
  }

  record FloatF(String name, Opt<Float> opt) implements TabField<Float> {
    public static FloatF optional(String name) {
      return new FloatF(name, Opt.nullOpt());
    }
  }

  record DoubleF(String name, Opt<Double> opt) implements TabField<Double> {
    public static DoubleF optional(String name) {
      return new DoubleF(name, Opt.nullOpt());
    }
  }

  record BooleanF(String name, Opt<Boolean> opt) implements TabField<Boolean> {
    public static BooleanF optional(String name) {
      return new BooleanF(name, Opt.nullOpt());
    }
  }
}
