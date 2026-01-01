package com.redsaz.lognition.convert;

import java.util.List;
import org.apache.avro.Schema;

public record TabSchema(List<TabField> fields) {

  private static final Schema.Parser PARSER = new Schema.Parser();

  /**
   * Essentially an Avro spec. Kinda. Hmm. Keep it simple.
   *
   * @param spec an Avro spec in string form.
   */
  public static TabSchema of(String spec) {
    return TabSchema.ofAvro(PARSER.parse(spec));
  }

  static TabSchema ofAvro(Schema schema) {
    return new TabSchema(schema.getFields().stream().map(TabField::fromAvro).toList());
  }
}
