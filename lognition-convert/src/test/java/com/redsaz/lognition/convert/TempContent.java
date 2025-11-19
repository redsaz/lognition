package com.redsaz.lognition.convert;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates a temporary file with the given comment, automatically deleted at the conclusion of the
 * try block.
 */
public class TempContent implements AutoCloseable {

  private final Path file;

  private TempContent(Path file) {
    this.file = file;
  }

  public static TempContent of(String content) {
    try {
      Path file = Files.createTempFile("temp-content", ".tmp");
      Files.writeString(file, content);
      return new TempContent(file);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public File file() {
    return file.toFile();
  }

  public Path path() {
    return file;
  }

  @Override
  public void close() throws UncheckedIOException {
    try {
      Files.deleteIfExists(file);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
