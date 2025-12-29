package com.redsaz.lognition.convert;

import static com.redsaz.lognition.convert.ConverterBaseTest.assertContentEquals;

import java.io.IOException;
import org.testng.annotations.Test;

public class CsvsTest {

  @Test
  public void testReadWrite() throws IOException {
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

    // Given a normal, everyday CSV file (not necessarily a Jmeter JTL or Loady log file),
    // When the source CSV is read in and written back out,
    try (TempContent sourceFile = TempContent.of(content);
         TempContent destFile = TempContent.withName("converted", ".csv");
         CsvStream records = Csvs.records( sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.headers(), records.stream());

      // Then the source CSV file and the result CSV file contents are functionally the same.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test
  public void testReadWriteEmpty() throws IOException {
    String content = "";

    // Given an empty CSV file,
    // When the source CSV is read in,
    try (TempContent sourceFile = TempContent.of(content);
         TempContent destFile = TempContent.withName("converted", ".csv");
         CsvStream records = Csvs.records( sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.headers(), records.stream());

      // Then the source CSV file and the result CSV file contents should be empty.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }

  @Test
  public void testReadWriteHeaderOnly() throws IOException {
    String content = "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect";

    // Given a CSV file with only a header line,
    // When the source CSV is read in,
    try (TempContent sourceFile = TempContent.of(content);
         TempContent destFile = TempContent.withName("converted", ".csv");
         CsvStream records = Csvs.records( sourceFile.path())) {

      // and written back out into a CSV,
      Csvs.write(destFile.path(), records.headers(), records.stream());

      // Then the source CSV file and the result CSV file contents should be empty.
      assertContentEquals(destFile.content(), content, "Reconstituted CSV data");
    }
  }


}
