package com.redsaz.lognition.convert;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Csvs {
  private Csvs() {}

  /**
   * Returns a stream to get csv records from a file.
   *
   * @apiNote Similar to {@link java.nio.file.Files#lines(Path)}, this should be used within a
   *     try-with-resources statement or similar to ensure the stream's file is closed promptly.
   * @param csvFile the file to read CSV data from
   * @return A CsvStream which has the headers and the stream to read the lines from.
   * @throws IOException if the file was not found or could not be opened.
   */
  public static CsvStream records(Path csvFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(csvFile.toFile()));
    CsvParserSettings settings = new CsvParserSettings();
    CsvParser parser = new CsvParser(settings);
    IterableResult<String[], ParsingContext> iterable = parser.iterate(br);
    Spliterator<String[]> iter = iterable.spliterator();
    HeadersGetter headersGetter = new HeadersGetter();
    iter.tryAdvance(headersGetter.fetcher());
    return new ReaderCsvStream(
        headersGetter.headers(),
        StreamSupport.stream(iter, false).onClose(uncheckedCloser(br)).map(Arrays::asList));
  }

  public static String write(Path dest, List<String> headers, Stream<List<String>> rows)
      throws IOException {
    try (HashingOutputStream hos =
        new HashingOutputStream(
            Hashing.sha256(), new BufferedOutputStream(new FileOutputStream(dest.toFile())))) {
      try (BufferedWriter bw =
          new BufferedWriter(new OutputStreamWriter(hos, StandardCharsets.UTF_8))) {
        CsvWriter writer = null;
        try {
          writer = new CsvWriter(bw, new CsvWriterSettings());
          if (headers != null && !headers.isEmpty()) {
            // Only write the headers if there are any.
            writer.writeHeaders(headers);
          }
          rows.forEach(writer::writeRow);
        } finally {
          if (writer != null) {
            writer.close(); // Looks like it could be put in try-with-resources, but nope.
          }
        }
      }
      return hos.hash().toString();
    }
  }

  private static Runnable uncheckedCloser(Closeable closeable) {
    return () -> {
      try {
        closeable.close();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    };
  }

  private record ReaderCsvStream(List<String> headers, Stream<List<String>> stream)
      implements CsvStream {
    @Override
    public void close() throws IOException {
      stream().close();
    }
  }

  private static class HeadersGetter {
    private List<String> headers = List.of();

    public Consumer<? super String[]> fetcher() {
      return (row) -> headers = List.copyOf(Arrays.asList(row));
    }

    public List<String> headers() {
      return headers;
    }
  }
}
