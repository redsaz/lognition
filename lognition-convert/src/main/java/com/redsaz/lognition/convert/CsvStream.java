package com.redsaz.lognition.convert;

import java.io.Closeable;
import java.util.List;
import java.util.stream.Stream;

public interface CsvStream extends Closeable {
  List<String> headers();
  Stream<List<String>> stream();
}
