package com.redsaz.lognition.convert;

import org.apache.avro.Schema;

public sealed interface TabField
    permits TabField.String,
        TabField.Int,
        TabField.Long,
        TabField.Float,
        TabField.Double,
        TabField.Boolean {

  java.lang.String name();

  static TabField fromAvro(Schema.Field field) {
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
        throw new IllegalArgumentException(
            "Tabular values (\"" + field.name() + "\") cannot be unions.");
      }
      case FIXED ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be fixeds.");
      case STRING -> new TabField.String(field.name());
      case BYTES ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be bytes.");
      case INT -> new TabField.Int(field.name());
      case LONG -> new TabField.Long(field.name());
      case FLOAT -> new TabField.Float(field.name());
      case DOUBLE -> new TabField.Double(field.name());
      case BOOLEAN -> new TabField.Boolean(field.name());
      case NULL ->
          throw new IllegalArgumentException(
              "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
    };
  }

  record String(java.lang.String name) implements TabField {}

  record Int(java.lang.String name) implements TabField {}

  record Long(java.lang.String name) implements TabField {}

  record Float(java.lang.String name) implements TabField {}

  record Double(java.lang.String name) implements TabField {}

  record Boolean(java.lang.String name) implements TabField {}
}
