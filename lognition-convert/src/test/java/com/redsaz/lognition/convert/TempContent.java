package com.redsaz.lognition.convert;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Creates a temporary file (optionally with content), automatically deleted at the conclusion of
 * the try block.
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

  public static TempContent empty() {
    try {
      Path file = Files.createTempFile("temp-content", ".tmp");
      return new TempContent(file);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Creates a temporary file with the given prefix and suffix.
   *
   * @param prefix File prefix
   * @param suffix File suffix
   */
  public static TempContent withName(String prefix, String suffix) {
    try {
      Path file = Files.createTempFile(prefix, suffix);
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

  public String content() {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public List<String> contentAllLines() {
    try {
      return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
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
