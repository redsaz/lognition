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
package com.redsaz.lognition.convert;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks if the CSV (JTL) row is a header row or not. Sometimes (in JMeter versions 2.12 and
 * possibly earlier), when running JMeter in remote mode, the CSV JTL file will not have a header
 * row! The CSV JTL generated in this fashion usually has the following defaults:
 *
 * <pre>
 * timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,bytes,grpThreads,allThreads,Latency
 * </pre>
 *
 * So, this utility can be used to check the first row of a CSV (JTL) file and for the converter to
 * act appropriately.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
/*package protected*/ class HeaderCheckUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderCheckUtil.class);

  public static final List<JtlType> DEFAULT_HEADERS =
      Arrays.asList(
          JtlType.TIMESTAMP,
          JtlType.ELAPSED,
          JtlType.LABEL,
          JtlType.RESPONSE_CODE,
          JtlType.RESPONSE_MESSAGE,
          JtlType.THREAD_NAME,
          JtlType.DATA_TYPE,
          JtlType.SUCCESS,
          JtlType.BYTES,
          JtlType.GRP_THREADS,
          JtlType.ALL_THREADS,
          JtlType.LATENCY);
  public static final String[] DEFAULT_HEADERS_TEXT =
      new String[] {
        "timeStamp", "elapsed", "label", "responseCode", "responseMessage",
        "threadName", "dataType", "success", "bytes", "grpThreads",
        "allThreads", "Latency"
      };

  // Don't make utility classes instantiable.
  private HeaderCheckUtil() {}

  public static boolean isJtlHeaderRow(String[] header) {
    int numUnknownCols = 0;
    for (String headerCol : header) {
      JtlType type = JtlType.fromHeader(headerCol);
      if (type == null) {
        ++numUnknownCols;
      }
    }
    // If there are no unknown columns, or if there are unknown columns,
    // but there are still some salvagable, then it is a JTL header row.
    return numUnknownCols == 0 || header.length - numUnknownCols >= 3;
  }

  public static boolean canUseDefaultHeaderRow(String[] header) {
    // This should be called after calling isJtlHeaderRow returns false,
    // to make sure that the default header can be used instead. IF the # of
    // columns is exactly 12, then we may have hit a bug with Jmeter 2.12
    // in which the JTL CSV headers are not output by default in remote
    // mode. Thankfully it can be fixable.
    return (header.length == 12
        && isNumber(header[0])
        && isNumber(header[1])
        && isBoolean(header[7])
        && isNumber(header[8])
        && isNumber(header[9])
        && isNumber(header[10])
        && isNumber(header[11]));
  }

  private static boolean isNumber(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    char first = text.charAt(0);
    if (first != '-' && first != '+' && (first < '0' || first > '9')) {
      return false;
    }
    for (int i = 1; i < text.length(); ++i) {
      char c = text.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  private static boolean isBoolean(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    return "true".equalsIgnoreCase("true") || "false".equalsIgnoreCase("false");
  }
}
