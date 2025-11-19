package com.redsaz.lognition.convert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO: This should be an interface
public record TabRecord(List<?> values) {
  Object get(int i) {
    return values.get(i);
  }

  public static TabRecord of(Object... values) {
    return new TabRecord(Collections.unmodifiableList(Arrays.asList(values)));
  }
}
