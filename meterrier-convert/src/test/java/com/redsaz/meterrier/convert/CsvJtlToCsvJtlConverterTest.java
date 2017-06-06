/*
 * Copyright 2017 Redsaz <redsaz@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redsaz.meterrier.convert;

import com.redsaz.meterrier.api.exceptions.AppServerException;
import static com.redsaz.meterrier.convert.ConverterBaseTest.assertAvroContentEquals;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/**
 * Test the CSV-JTL to CSV-JTL converter (Usually this operation is one that
 * simply removes columns, but it could be a straight 1-to-1 if needed.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class CsvJtlToCsvJtlConverterTest extends ConverterBaseTest {

    @Test
    public void testConvert() {
        // The orginal CSV to import from will have more columns than we'll
        // actually use.
        MockPerfData mpd = defaultMockData();
        File source = createTempFile("source", ".csv");
        mpd.createImportCsvFile(source, true);

        File expectedDest = createTempFile("expected", ".csv");
        String expectedHash = mpd.createExportedCsvFile(expectedDest);

        Converter conv = new CsvJtlToCsvJtlConverter();
        File actualDest = createTempFile("actual", ".csv");
        String actualHash = conv.convert(source, actualDest);

        assertContentEquals(actualDest, expectedDest, "The converter did not convert in the way expected.");
        assertBytesEquals(actualDest, expectedDest, "The conversions are not byte-for-byte equal.");
        assertEquals(actualHash, expectedHash, "Hashes differed.");
    }

    @Test
    public void testConvertNoHeaderButDefaultColumns() throws IOException {
        MockPerfData mpd = defaultMockData();
        File source = createTempFile("source", ".jtl");
        mpd.createImportCsvFile(source, false);

        File expectedDest = createTempFile("expected", ".avro");
        mpd.createAvroFile(expectedDest);

        Converter conv = new CsvJtlToAvroUnorderedConverter();
        File actualDest = createTempFile("actual", ".avro");
        conv.convert(source, actualDest);

        assertAvroContentEquals(actualDest, expectedDest, "The converter did not convert in the way expected.");
    }

    @Test
    public void testConvertRowWithMissingColumnSkipped() throws IOException {
        // If a row is encountered that has a missing column, then skip the
        // defective row.
        File source = createTempFile("sourceMissingColumn", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
            pw.println("600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599");
        }
        // This jtl is the same as above, but the row with the missing column
        // is removed. The resulting avro files from each should be equal.
        File sourceEffective = createTempFile("sourceEffective", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(sourceEffective.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
        }

        Converter conv = new CsvJtlToAvroUnorderedConverter();
        File actualDest = createTempFile("actual", ".avro");
        conv.convert(source, actualDest);
        File effectiveDest = createTempFile("effective", ".avro");
        conv.convert(source, effectiveDest);

        assertAvroContentEquals(actualDest, effectiveDest, "The converter did not handle a malformed row in the way expected.");
    }

    @Test
    public void testConvertRowWithExtraColumnSkipped() throws IOException {
        // If a row is encountered that has an extra column, then skip the
        // defective row.
        File source = createTempFile("sourceMissingColumn", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
            pw.println("extra,1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599");
        }
        // This jtl is the same as above, but the row with the extra column
        // is removed. The resulting avro files from each should be equal.
        File sourceEffective = createTempFile("sourceEffective", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(sourceEffective.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
        }

        Converter conv = new CsvJtlToAvroUnorderedConverter();
        File actualDest = createTempFile("actual", ".avro");
        conv.convert(source, actualDest);
        File effectiveDest = createTempFile("effective", ".avro");
        conv.convert(source, effectiveDest);

        assertAvroContentEquals(actualDest, effectiveDest, "The converter did not handle a malformed row in the way expected.");
    }

    @Test(dataProvider = "nonNumericColumnsDp")
    public void testConvertRowNumericColumnsAreNonNumericAreSkipped(String badRow, String whyBad) throws IOException {
        // If a row is encountered that has an extra column, then skip the
        // defective row.
        File source = createTempFile("sourceMissingColumn", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
            pw.println(badRow);
        }
        // This jtl is the same as above, but the row with the extra column
        // is removed. The resulting avro files from each should be equal.
        File sourceEffective = createTempFile("sourceEffective", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(sourceEffective.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency");
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495");
        }

        Converter conv = new CsvJtlToAvroUnorderedConverter();
        File actualDest = createTempFile("actual", ".avro");
        conv.convert(source, actualDest);
        File effectiveDest = createTempFile("effective", ".avro");
        conv.convert(source, effectiveDest);

        assertAvroContentEquals(actualDest, effectiveDest,
                "The converter did not handle a malformed row, \"" + whyBad
                + "\" in the way expected.");
    }

    @DataProvider(name = "nonNumericColumnsDp", parallel = true)
    public static Object[][] nonNumericColoumnsDp() {
        return new Object[][]{
            new Object[]{"words,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599", "Bad timeStamp"},
            new Object[]{"9223372036854775808,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599", "timeStamp is longer than a long"},
            new Object[]{"1469546803889,blah,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599", "Bad elapsed"},
            new Object[]{"1469546803889,9223372036854775808,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599", "elapsed is longer than a long"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,bogus,2,2,599", "bad bytes"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,9223372036854775808,2,2,599", "bytes is longer than a long"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,bogus,2,599", "bad grpThreads"},
            //            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,9223372036854775808,2,599", "grpThreads longer than a long"},
            //            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,bogus,599", "bad allThreads"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,9223372036854775808,599", "allThreads longer than a long"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,bogus", "bad Latency"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,9223372036854775808", "Latency longer than a long"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,yes,280,2,2,599", "success is not a boolean"},
            new Object[]{"1469546803889,600,GET test/thing,200,OK,example 1-1,text,0,280,2,2,599", "success is not a boolean"},};
    }

    @Test(expectedExceptions = {AppServerException.class})
    public void testConvertNoHeaderNotDefaultColumns() throws IOException {
        File source = createTempFile("source", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            // The thread name is removed. This should not be convertable
            // without a header row.
            pw.println("1469546803634,496,GET test/thing,200,OK,text,true,280,2,2,495");
            pw.println("1469546803889,600,GET test/thing,200,OK,text,true,280,2,2,599");
        }

        Converter conv = new CsvJtlToCsvJtlConverter();
        File actualDest = createTempFile("actual", ".csv");
        conv.convert(source, actualDest);
    }

    @Test(expectedExceptions = {AppServerException.class})
    public void testConvertNoHeaderMoreColumnsThanDefault() throws IOException {
        File source = createTempFile("source", ".jtl");
        try (BufferedWriter bw = Files.newBufferedWriter(source.toPath());
                PrintWriter pw = new PrintWriter(bw)) {
            // An extra column is added at the end. This should not be
            // convertable without a header row.
            pw.println("1469546803634,496,GET test/thing,200,OK,example 1-1,text,true,280,2,2,495,extra");
            pw.println("1469546803889,600,GET test/thing,200,OK,example 1-1,text,true,280,2,2,599,extra");
        }

        Converter conv = new CsvJtlToCsvJtlConverter();
        File actualDest = createTempFile("actual", ".csv");
        conv.convert(source, actualDest);
    }

    private static MockPerfData defaultMockData() {
        // The orginal CSV to import from will have more columns than we'll
        // actually use.
        MockPerfData mpd = new MockPerfData(System.currentTimeMillis(),
                240L,
                Arrays.asList(
                        "Another-call-2",
                        "Howdy there this is a call as well",
                        "example-call-1"
                ),
                Arrays.asList(
                        "thread-1",
                        "thread-2",
                        "thread-3",
                        "thread-4",
                        "thread-5"
                ),
                Arrays.asList(
                        "1001",
                        "200"
                ),
                Arrays.asList(
                        "Non Standard code",
                        "Normally we don't see these"
                ));
        return mpd;
    }

}
