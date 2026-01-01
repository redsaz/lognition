package com.redsaz.lognition.convert;

import java.util.function.Predicate;
import org.apache.avro.Schema;

public record TabFieldOld(String name, Class<?> type, Object defaultVal) {

  static TabFieldOld fromAvro(Schema.Field field) {
    return switch (field.schema().getType()) {
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
        if (field.schema().getTypes().stream()
                .filter(Predicate.not(Schema.Type.NULL::equals))
                .count()
            != 1) {
          throw new IllegalArgumentException(
              "Tabular values (\""
                  + field.name()
                  + "\") can only be unions of null and one of: string, bytes, int, long, float, double, or boolean.");
        }
        Schema.Type type =
            field.schema().getTypes().stream()
                .filter(Predicate.not(Schema.Type.NULL::equals))
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException("Union is missing a non-null type."))
                .getType();
        yield new TabFieldOld(field.name(), String.class, field.defaultVal());
      }
      case FIXED ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be fixeds.");
      case STRING -> new TabFieldOld(field.name(), String.class, field.defaultVal());
      case BYTES -> new TabFieldOld(field.name(), byte[].class, field.defaultVal());
      case INT -> new TabFieldOld(field.name(), Integer.class, field.defaultVal());
      case LONG -> new TabFieldOld(field.name(), Long.class, field.defaultVal());
      case FLOAT -> new TabFieldOld(field.name(), Float.class, field.defaultVal());
      case DOUBLE -> new TabFieldOld(field.name(), Double.class, field.defaultVal());
      case BOOLEAN -> new TabFieldOld(field.name(), Boolean.class, field.defaultVal());
      case NULL ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
    };
  }

  private static Class<?> calcType(String name, Schema.Type type) {
    return null;
    // Man, I don't know.
    //    return switch (type) {
    //      case RECORD ->
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + name + "\") cannot be records.");
    //      case ENUM ->
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + name + "\") cannot be enums.");
    //      case ARRAY ->
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + name + "\") cannot be arrays.");
    //      case MAP ->
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + name + "\") cannot be maps.");
    //      case UNION -> {
    //
    //        if
    // (field.schema().getTypes().stream().filter(Predicate.not(Schema.Type.NULL::equals)).count()
    // != 1) {
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + name + "\") can only be unions of null and one of:
    // string, bytes, int, long, float, double, or boolean.");
    //        }
    //        Schema.Type type =
    // field.schema().getTypes().stream().filter(Predicate.not(Schema.Type.NULL::equals)).findFirst().orElseThrow(() -> new IllegalArgumentException("Union is missing a non-null type.")).getType();
    //        new TabFieldOld(field.name(), String.class, field.defaultVal());
    //      }
    //      case FIXED ->
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + name + "\") cannot be fixeds.");
    //      case STRING -> new TabFieldOld(name, String.class, field.defaultVal());
    //      case BYTES -> new TabFieldOld(name, byte[].class, field.defaultVal());
    //      case INT -> new TabFieldOld(name, Integer.class, field.defaultVal());
    //      case LONG -> new TabFieldOld(name Long.class, field.defaultVal());
    //      case FLOAT -> new TabFieldOld(name, Float.class, field.defaultVal());
    //      case DOUBLE -> new TabFieldOld(name, Double.class, field.defaultVal());
    //      case BOOLEAN -> new TabFieldOld(name, Boolean.class, field.defaultVal());
    //      case NULL ->
    //          throw new IllegalArgumentException(
    //              "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
    //    };
  }
}
