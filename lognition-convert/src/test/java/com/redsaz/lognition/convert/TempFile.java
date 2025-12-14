package com.redsaz.lognition.convert;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates a temporary file and deletes it on close. */
class TempFile implements Closeable {
  private Path path;

  public TempFile() {
    try {
      this.path = Files.createTempFile("lognition-test-temp-file", ".tmp");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public File file() {
    return path.toFile();
  }

  public Path path() {
    return path;
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(path);
  }
}
