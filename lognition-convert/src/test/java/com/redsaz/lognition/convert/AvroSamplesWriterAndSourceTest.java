package com.redsaz.lognition.convert;

import static org.testng.Assert.assertEquals;

import com.redsaz.lognition.api.model.Sample;
import java.io.IOException;
import org.testng.annotations.Test;

public class AvroSamplesWriterAndSourceTest {

  @Test
  public void testWrite() throws IOException {
    // Test that samples that are written to file, result in the same values read from the file.
    try (TempContent temp = TempContent.empty()) {
      AvroSamplesWriter writer = new AvroSamplesWriter();
      Samples samples =
          ListSamples.builder()
              .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
              .add(
                  Sample.of(
                      1367L,
                      12L,
                      "GET fail/{id}",
                      "1",
                      "NonHttpStatusCode",
                      "Connection Refused",
                      false,
                      123L,
                      2))
              .add(Sample.of(1607L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2))
              .build();
      writer.write(samples, temp.file());

      Samples result = AvroSamplesReader.readSamples(temp.path());
      assertEquals(result, samples);
    }
  }

  @Test
  public void testWriteRealisticAndBack() throws IOException {
    // This JTL data was (mostly) taken from a real 10-thread jmeter run.
    String content =
        """
        timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
        1766362285195,104,GET /logs/test,200,OK,Thread Group 1-1,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key2=val2,104,0,1
        1766362285191,111,PUT /logs/test,200,OK,Thread Group 1-5,text,true,,538,287,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key0=val0,111,0,1
        1766362285205,101,GET /logs/test,200,OK,Thread Group 1-10,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key6=val6,101,0,0
        1766362285195,112,GET /logs/test,200,OK,Thread Group 1-3,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,111,0,3
        1766362285197,112,GET /logs/test,200,OK,Thread Group 1-4,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key3=val3,112,0,1
        1766362285202,108,GET /logs/test,200,OK,Thread Group 1-9,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key5=val5,108,0,1
        1766362285197,115,GET /logs/test,200,OK,Thread Group 1-2,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key4=val4,115,0,1
        1766362285284,103,GET /logs/test,200,OK,Thread Group 1-6,text,true,,468,231,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key7=val7,103,0,0
        1766362285292,101,GET /logs/test,400,Bad Request,Thread Group 1-7,text,false,,502,242,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key8=val8&status=400,100,0,0
        1766362285297,102,POST /logs/test,200,OK,Thread Group 1-8,text,true,,539,288,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key9=val9,102,0,0
        1766362285303,97,PUT /logs/test,200,OK,Thread Group 1-5,text,true,,547,296,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key11=val11,97,0,1
        1766362285308,98,GET /logs/test,500,Internal Server Error,Thread Group 1-10,text,false,,514,244,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key12=val12&status=500,98,0,0
        1766362285300,110,GET /logs/test,404,Not Found,Thread Group 1-1,text,false,,502,244,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key10=val10&status=404,110,0,1
        1766362285310,1,GET /logs/test,Non HTTP response code: org.apache.http.conn.HttpHostConnectException,Non HTTP response message: Connect to 127.0.0.1:8080 [/127.0.0.1] failed: Connection refused,Thread Group 1-4,text,false,,2546,0,10,10,http://127.0.0.1:8080/logs/test?delay=100&delayrange=20&key1=val1,0,0,1
        """;

    try (TempContent jtlFile = TempContent.of(content);
        TempContent avroFile = TempContent.withName("converted", ".avro")) {
      // Given samples read from a Jmeter test results log (JTL) file (and sorted),
      Samples samples = CsvSamplesReader.readSamples(jtlFile.path());

      // When the data is saved to an Avro file and read back out,
      AvroSamplesWriter writer = new AvroSamplesWriter();
      writer.write(samples, avroFile.file());
      Samples result = AvroSamplesReader.readSamples(avroFile.path());

      // Then the samples fetched from the Avro file should be identical to the samples that were
      // saved to the file.
      assertEquals(result, samples);
    }
  }

  @Test
  public void testWriteUnorderedInputIsOrdered() throws IOException {
    // Test that samples that are written to file, result in the same values read from the file, but
    // ordered by sample starting timestamp.
    try (TempContent temp = TempContent.empty()) {
      AvroSamplesWriter writer = new AvroSamplesWriter();
      Samples samples =
          ListSamples.builder()
              .add(Sample.of(1607L, 20L, "GET example/{id}", "2", "200", "OK", true, 123L, 2))
              .add(
                  Sample.of(
                      1367L,
                      12L,
                      "GET fail/{id}",
                      "1",
                      "NonHttpStatusCode",
                      "Connection Refused",
                      false,
                      123L,
                      2))
              .add(Sample.of(1254L, 10L, "GET example/{id}", "1", "200", "OK", true, 123L, 2))
              .build();
      writer.write(samples, temp.file());

      Samples result = AvroSamplesReader.readSamples(temp.path());
      assertEquals(result.getSamples().get(0).getOffset(), 0L);
      assertEquals(result.getSamples().get(1).getOffset(), 113L);
      assertEquals(result.getSamples().get(2).getOffset(), 353L);
      assertEquals(result, samples);
    }
  }
}
