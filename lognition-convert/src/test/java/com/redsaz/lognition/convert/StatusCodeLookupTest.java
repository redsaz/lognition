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
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the StatusCodeLookup class.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class StatusCodeLookupTest {

    @Test
    public void testStatusCodeLookup() {
        StatusCodeLookup scl = new StatusCodeLookup();
        Integer ref = scl.getRef("200", "OK");
        assertEquals(ref.intValue(), 4, "Wrong status code for lookup.");
    }

    /**
     * Check that when a StatusCodeLookup is created with codes and messages,
     * that we're able to retrieve those codes and messages correctly.
     */
    @Test
    public void testStatusCodeLookupCollectionCollection() {
        List<CharSequence> codes = Arrays.asList("1000", "200");
        List<CharSequence> messages = Arrays.asList("Super awesome status", "It sure is ok.");
        StatusCodeLookup scl = new StatusCodeLookup(codes, messages);
        Integer ref1 = scl.getRef("1000", "Super awesome status");
        Integer ref2 = scl.getRef("200", "It sure is ok.");
        assertEquals(ref1.intValue(), -64, "Custom code had wrong lookup.");
        assertEquals(ref2.intValue(), -65, "Custom code had wrong lookup.");
        assertEquals(scl.getCode(ref1), "1000", "Retrieved wrong status code.");
        assertEquals(scl.getCode(ref2), "200", "Retrieved wrong status code.");
        assertEquals(scl.getMessage(ref1), "Super awesome status", "Retrieved wrong status code.");
        assertEquals(scl.getMessage(ref2), "It sure is ok.", "Retrieved wrong status code.");
    }

    /**
     * Check that when a StatusCodeLookup is created with uneven number of codes
     * and messages, an IllegalArgumentException is thrown.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStatusCodeLookupCollectionCollection_NegativeUnevenCollections() {
        List<CharSequence> codes = Arrays.asList("1000", "200");
        List<CharSequence> messages = Arrays.asList("Super awesome status");
        StatusCodeLookup scl = new StatusCodeLookup(codes, messages);
    }

    @Test(dataProvider = "refsCodesAndMessagesDp")
    public void testGetRef(int expectedRef, String code, String message) {
        StatusCodeLookup scl = new StatusCodeLookup();
        Integer actual = scl.getRef(code, message);
        assertEquals(actual.intValue(), expectedRef, "Wrong ref returned.");
    }

    @Test(dataProvider = "refsCodesAndMessagesDp")
    public void testGetCode(int ref, String expectedCode, String unused) {
        StatusCodeLookup scl = new StatusCodeLookup();
        CharSequence actual = scl.getCode(ref);
        assertEquals(actual, expectedCode, "Code is incorrect.");
    }

    @Test(dataProvider = "refsCodesAndMessagesDp")
    public void testGetMessage(int ref, String unused, String expectedMessage) {
        StatusCodeLookup scl = new StatusCodeLookup();
        CharSequence actual = scl.getMessage(ref);
        assertEquals(actual, expectedMessage, "Message is incorrect.");
    }

    @Test
    public void testGetCustomCodes() {
        List<CharSequence> codes = Arrays.asList("1000", "200");
        List<CharSequence> messages = Arrays.asList("Super awesome status", "It sure is ok.");
        StatusCodeLookup scl = new StatusCodeLookup(codes, messages);
        assertEquals(scl.getCustomCodes(), codes, "Custom codes incorrect.");
    }

    @Test
    public void testGetCustomMessages() {
        List<CharSequence> codes = Arrays.asList("1000", "200");
        List<CharSequence> messages = Arrays.asList("Super awesome status", "It sure is ok.");
        StatusCodeLookup scl = new StatusCodeLookup(codes, messages);
        assertEquals(scl.getCustomMessages(), messages, "Custom messages incorrect.");
    }

    @DataProvider(name = "refsCodesAndMessagesDp", parallel = true)
    public Object[][] codesAndMessagesDp() {
        List<String> codes = Arrays.asList(
                "0", "100", "101", "102", "200", "201", "202", "203", "204", "205",
                "206", "207", "208", "226", "300", "301", "302", "303", "304", "305",
                "307", "308", "400", "401", "402", "403", "404", "405", "406", "407",
                "408", "409", "410", "411", "412", "413", "414", "415", "416", "417",
                "418", "421", "422", "423", "424", "426", "428", "429", "431", "444",
                "451", "499", "500", "501", "502", "503", "504", "505", "506", "507",
                "508", "510", "511", "599");
        List<String> messages = Arrays.asList(
                "Unspecified", "Continue", "Switching Protocols", "Processing", "OK", "Created", "Accepted", "Non-authoritative Information", "No Content", "Reset Content",
                "Partial Content", "Multi-Status", "Already Reported", "IM Used", "Multiple Choices", "Moved Permanently", "Found", "See Other", "Not Modified", "Use Proxy",
                "Temporary Redirect", "Permanent Redirect", "Bad Request", "Unauthorized", "Payment Required", "Forbidden", "Not Found", "Method Not Allowed", "Not Acceptable", "Proxy Authentication Required",
                "Request Timeout", "Conflict", "Gone", "Length Required", "Precondition Failed", "Payload Too Large", "Request-URI Too Long", "Unsupported Media Type", "Requested Range Not Satisfiable", "Expectation Failed",
                "I'm a teapot", "Misdirected Request", "Unprocessable Entity", "Locked", "Failed Dependency", "Upgrade Required", "Precondition Required", "Too Many Requests", "Request Header Fields Too Large", "Connection Closed Without Response",
                "Unavailable For Legal Reasons", "Client Closed Request", "Internal Server Error", "Not Implemented", "Bad Gateway", "Service Unavailable", "Gateway Timeout", "HTTP Version Not Supported", "Variant Also Negotiates", "Insufficient Storage",
                "Loop Detected", "Not Extended", "Network Authentication Required", "Network Connect Timeout Error");
        Object[][] params = new Object[codes.size()][];
        for (int i = 0; i < codes.size(); ++i) {
            params[i] = new Object[]{i, codes.get(i), messages.get(i)};
        }
        return params;
    }
}
