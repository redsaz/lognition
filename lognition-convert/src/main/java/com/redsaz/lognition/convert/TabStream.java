package com.redsaz.lognition.convert;

import java.io.Closeable;
import java.util.List;
import java.util.stream.Stream;

public interface TabStream extends Closeable {
  List<String> fieldNames();

  TabSchema schema();

  Stream<TabRecord> stream();
}
