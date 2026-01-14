package com.redsaz.lognition.convert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.avro.Schema;

public sealed interface TabField<T>
    permits TabField.StrF,
        TabField.IntF,
        TabField.LongF,
        TabField.FloatF,
        TabField.DoubleF,
        TabField.BooleanF,
        TabField.UnionF {

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

    return (TabField<U>)
        switch (field.schema().getType()) {
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
            List<Class<?>> types =
                field.schema().getTypes().stream()
                    .map(
                        t ->
                            switch (t.getType()) {
                              case STRING -> (Class<?>) String.class;
                              case INT -> (Class<?>) Integer.class;
                              case LONG -> (Class<?>) Long.class;
                              case FLOAT -> (Class<?>) Float.class;
                              case DOUBLE -> (Class<?>) Double.class;
                              case BOOLEAN -> (Class<?>) Boolean.class;
                              case NULL -> null;
                              default ->
                                  throw new IllegalArgumentException(
                                      "Tabular union cannot have type \"" + t.getType() + "\".");
                            })
                    .filter(Objects::nonNull)
                    .toList();
            yield new UnionF(
                field.name(), Opt.of(field.hasDefaultValue(), field.defaultVal()), types);
          }
          case FIXED ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be fixeds.");
          case STRING ->
              new StrF(field.name(), Opt.of(field.hasDefaultValue(), (String) field.defaultVal()));
          case BYTES ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be bytes.");
          case INT ->
              new IntF(field.name(), Opt.of(field.hasDefaultValue(), (Integer) field.defaultVal()));
          case LONG ->
              new LongF(field.name(), Opt.of(field.hasDefaultValue(), (Long) field.defaultVal()));
          case FLOAT ->
              new FloatF(field.name(), Opt.of(field.hasDefaultValue(), (Float) field.defaultVal()));
          case DOUBLE ->
              new DoubleF(
                  field.name(), Opt.of(field.hasDefaultValue(), (Double) field.defaultVal()));
          case BOOLEAN ->
              new BooleanF(
                  field.name(), Opt.of(field.hasDefaultValue(), (Boolean) field.defaultVal()));
          case NULL ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
        };
  }

  record StrF(String name, Opt<String> opt) implements TabField<String> {
    public static StrF optional(String name) {
      return new StrF(name, Opt.nullOpt());
    }

    public static StrF optional(String name, String defVal) {
      return new StrF(name, Opt.of(defVal));
    }

    public static StrF required(String name) {
      return new StrF(name, Opt.required());
    }
  }

  record IntF(String name, Opt<Integer> opt) implements TabField<Integer> {
    public static IntF optional(String name) {
      return new IntF(name, Opt.nullOpt());
    }

    public static IntF optional(String name, Integer defVal) {
      return new IntF(name, Opt.of(defVal));
    }

    public static IntF required(String name) {
      return new IntF(name, Opt.required());
    }
  }

  record LongF(String name, Opt<Long> opt) implements TabField<Long> {
    public static LongF optional(String name) {
      return new LongF(name, Opt.nullOpt());
    }

    public static LongF optional(String name, Long defVal) {
      return new LongF(name, Opt.of(defVal));
    }

    public static LongF required(String name) {
      return new LongF(name, Opt.required());
    }
  }

  record FloatF(String name, Opt<Float> opt) implements TabField<Float> {
    public static FloatF optional(String name) {
      return new FloatF(name, Opt.nullOpt());
    }

    public static FloatF optional(String name, Float defVal) {
      return new FloatF(name, Opt.of(defVal));
    }

    public static FloatF required(String name) {
      return new FloatF(name, Opt.required());
    }
  }

  record DoubleF(String name, Opt<Double> opt) implements TabField<Double> {
    public static DoubleF optional(String name) {
      return new DoubleF(name, Opt.nullOpt());
    }

    public static DoubleF optional(String name, Double defVal) {
      return new DoubleF(name, Opt.of(defVal));
    }

    public static DoubleF required(String name) {
      return new DoubleF(name, Opt.required());
    }
  }

  record BooleanF(String name, Opt<Boolean> opt) implements TabField<Boolean> {
    public static BooleanF optional(String name) {
      return new BooleanF(name, Opt.nullOpt());
    }

    public static BooleanF optional(String name, Boolean defVal) {
      return new BooleanF(name, Opt.of(defVal));
    }

    public static BooleanF required(String name) {
      return new BooleanF(name, Opt.required());
    }
  }

  record UnionF(String name, Opt<Object> opt, List<Class<?>> types) implements TabField<Object> {
    public static UnionF optional(String name, Class<?>... types) {
      return new UnionF(name, Opt.nullOpt(), List.copyOf(Arrays.asList(types)));
    }

    public static UnionF optional(String name, Object defVal, Class<?>... types) {
      return new UnionF(name, Opt.of(defVal), List.copyOf(Arrays.asList(types)));
    }

    public static UnionF required(String name, Class<?>... types) {
      return new UnionF(name, Opt.required(), List.copyOf(Arrays.asList(types)));
    }
  }
}
