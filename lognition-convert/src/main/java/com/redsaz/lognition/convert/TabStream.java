package com.redsaz.lognition.convert;

import java.io.Closeable;
import java.util.stream.Stream;

public interface TabStream extends Closeable {
  TabSchema.StructS schema();

  Stream<TabRecord> stream();
}
