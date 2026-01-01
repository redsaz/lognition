package com.redsaz.lognition.convert;

import java.util.List;
import org.apache.avro.Schema;

public record TabSchema(List<? extends TabField> fields) {

  /**
   * Essentially an Avro spec. Kinda. Hmm. Keep it simple.
   *
   * @param spec an Avro spec in string form.
   */
  public static TabSchema of(String spec) {
    return TabSchema.ofAvro((new Schema.Parser()).parse(spec));
  }

  static TabSchema ofAvro(Schema schema) {
    return new TabSchema(schema.getFields().stream().map(TabField::fromAvro).toList());
  }
}
