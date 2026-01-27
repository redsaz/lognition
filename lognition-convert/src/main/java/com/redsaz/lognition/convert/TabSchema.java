package com.redsaz.lognition.convert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.avro.Schema;

public sealed interface TabSchema<T>
    permits TabSchema.StrS,
        TabSchema.IntS,
        TabSchema.LongS,
        TabSchema.FloatS,
        TabSchema.DoubleS,
        TabSchema.BooleanS,
        TabSchema.UnionS,
        TabSchema.StructS {

  String name();

  Class<T> type();

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

  static <U> TabSchema<U> fromAvro(Schema.Field field) {

    return (TabSchema<U>)
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
            yield new UnionS(
                field.name(), Opt.of(field.hasDefaultValue(), field.defaultVal()), types);
          }
          case FIXED ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be fixeds.");
          case STRING ->
              new StrS(field.name(), Opt.of(field.hasDefaultValue(), (String) field.defaultVal()));
          case BYTES ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be bytes.");
          case INT ->
              new IntS(field.name(), Opt.of(field.hasDefaultValue(), (Integer) field.defaultVal()));
          case LONG ->
              new LongS(field.name(), Opt.of(field.hasDefaultValue(), (Long) field.defaultVal()));
          case FLOAT ->
              new FloatS(field.name(), Opt.of(field.hasDefaultValue(), (Float) field.defaultVal()));
          case DOUBLE ->
              new DoubleS(
                  field.name(), Opt.of(field.hasDefaultValue(), (Double) field.defaultVal()));
          case BOOLEAN ->
              new BooleanS(
                  field.name(), Opt.of(field.hasDefaultValue(), (Boolean) field.defaultVal()));
          case NULL ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
        };
  }

  record StrS(String name, Opt<String> opt) implements TabSchema<String> {
    public static StrS optional(String name) {
      return new StrS(name, Opt.nullOpt());
    }

    public static StrS optional(String name, String defVal) {
      return new StrS(name, Opt.of(defVal));
    }

    public static StrS required(String name) {
      return new StrS(name, Opt.required());
    }

    public Class<String> type() {
      return String.class;
    }
  }

  record IntS(String name, Opt<Integer> opt) implements TabSchema<Integer> {
    public static IntS optional(String name) {
      return new IntS(name, Opt.nullOpt());
    }

    public static IntS optional(String name, Integer defVal) {
      return new IntS(name, Opt.of(defVal));
    }

    public static IntS required(String name) {
      return new IntS(name, Opt.required());
    }

    public Class<Integer> type() {
      return Integer.class;
    }
  }

  record LongS(String name, Opt<Long> opt) implements TabSchema<Long> {
    public static LongS optional(String name) {
      return new LongS(name, Opt.nullOpt());
    }

    public static LongS optional(String name, Long defVal) {
      return new LongS(name, Opt.of(defVal));
    }

    public static LongS required(String name) {
      return new LongS(name, Opt.required());
    }

    public Class<Long> type() {
      return Long.class;
    }
  }

  record FloatS(String name, Opt<Float> opt) implements TabSchema<Float> {
    public static FloatS optional(String name) {
      return new FloatS(name, Opt.nullOpt());
    }

    public static FloatS optional(String name, Float defVal) {
      return new FloatS(name, Opt.of(defVal));
    }

    public static FloatS required(String name) {
      return new FloatS(name, Opt.required());
    }

    public Class<Float> type() {
      return Float.class;
    }
  }

  record DoubleS(String name, Opt<Double> opt) implements TabSchema<Double> {
    public static DoubleS optional(String name) {
      return new DoubleS(name, Opt.nullOpt());
    }

    public static DoubleS optional(String name, Double defVal) {
      return new DoubleS(name, Opt.of(defVal));
    }

    public static DoubleS required(String name) {
      return new DoubleS(name, Opt.required());
    }

    public Class<Double> type() {
      return Double.class;
    }
  }

  record BooleanS(String name, Opt<Boolean> opt) implements TabSchema<Boolean> {
    public static BooleanS optional(String name) {
      return new BooleanS(name, Opt.nullOpt());
    }

    public static BooleanS optional(String name, Boolean defVal) {
      return new BooleanS(name, Opt.of(defVal));
    }

    public static BooleanS required(String name) {
      return new BooleanS(name, Opt.required());
    }

    public Class<Boolean> type() {
      return Boolean.class;
    }
  }

  record UnionS(String name, Opt<Object> opt, List<Class<?>> types) implements TabSchema<Object> {
    public static UnionS optional(String name, Class<?>... types) {
      return new UnionS(name, Opt.nullOpt(), List.copyOf(Arrays.asList(types)));
    }

    public static UnionS optional(String name, Object defVal, Class<?>... types) {
      return new UnionS(name, Opt.of(defVal), List.copyOf(Arrays.asList(types)));
    }

    public static UnionS required(String name, Class<?>... types) {
      return new UnionS(name, Opt.required(), List.copyOf(Arrays.asList(types)));
    }

    public Class<Object> type() {
      return Object.class;
    }
  }

  record StructS(String name, List<? extends TabSchema<?>> fields) implements TabSchema<Object> {
    public StructS {
      // Must not have duplicate names.
      Set<String> names = new HashSet<>();
      fields.stream()
          .map(TabSchema::name)
          .forEach(
              fname -> {
                if (!names.add(fname)) {
                  throw new IllegalArgumentException("Schema has duplicate names: " + fname);
                }
              });
    }

    /**
     * Essentially an Avro spec. Kinda. Hmm. Keep it simple.
     *
     * @param spec an Avro spec in string form.
     */
    public static StructS of(String spec) {
      return StructS.ofAvro((new Schema.Parser()).parse(spec));
    }

    public static StructS of(String name, TabSchema<?>... fields) {
      return new StructS(name, List.copyOf(Arrays.asList(fields)));
    }

    static StructS ofAvro(Schema schema) {
      return new StructS(
          schema.getFullName(), schema.getFields().stream().map(TabSchema::fromAvro).toList());
    }

    @Override
    public String name() {
      return "";
    }

    public Class<Object> type() {
      return Object.class;
    }

    @Override
    public Opt<Object> opt() {
      return null;
    }
  }
}
