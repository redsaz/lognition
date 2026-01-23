package com.redsaz.lognition.convert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.avro.Schema;

// TODO: TabSchema shouldn't be a record, but an interface.
// TabSchema should be a RecordF
public record TabSchema(List<? extends TabField<?>> fields) {
  public TabSchema {
    // Must not have duplicate names.
    Set<String> names = new HashSet<>();
    fields.stream()
        .map(TabField::name)
        .forEach(
            name -> {
              if (!names.add(name)) {
                throw new IllegalArgumentException("Schema has duplicate names: " + name);
              }
            });
  }

  /**
   * Essentially an Avro spec. Kinda. Hmm. Keep it simple.
   *
   * @param spec an Avro spec in string form.
   */
  public static TabSchema of(String spec) {
    return TabSchema.ofAvro((new Schema.Parser()).parse(spec));
  }

  public static TabSchema of(TabField<?>... fields) {
    return new TabSchema(List.copyOf(Arrays.asList(fields)));
  }

  static TabSchema ofAvro(Schema schema) {
    return new TabSchema(schema.getFields().stream().map(TabField::fromAvro).toList());
  }
}
