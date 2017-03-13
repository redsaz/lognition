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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a condensed form of all standard status codes. These are so common
 * that they can be hard0coded. For the weird web servers that do not follow
 * these standard response codes, they will be added to a custom lookup. This
 * mapping will need stored along with the sample data in order to be
 * successfully retrieved.
 *
 * This takes advantage of compact integer encoders for avro or protobuf that
 * use zig-zag variable-length integers. The normal status codes all have
 * reference values of 0 to 63, and custom status codes go from -64 to -127 (and
 * further, if need be). This ensures that the status code will fit in a single
 * byte, instead of 4 like a normal integer would be (otherwise we're actually
 * WASTING a byte).
 */
class StatusCodeLookup {

    private static final List<String> REF_TO_CODE;
    private static final List<String> REF_TO_MESSAGE;
    private static final Map<String, Integer> CODE_TO_REF = new HashMap<>();
    private static final Map<String, String> MESSAGE_TO_CODE = new HashMap<>();
    // What reference to start counting the customer status codes from.
    private static final int CUSTOM_START_REF = -64;
    // How many integers to skip for each new ref. Negative means going down.
    private static final int CUSTOM_SPAN = -1;
    // Key=code+message Value=Ref
    private final Map<String, Integer> customLookup = new HashMap<>();
    private final List<CharSequence> customCodes;
    private final List<CharSequence> customMessages;

    static {
        REF_TO_CODE = Arrays.asList(
                "0", "100", "101", "102", "200", "201", "202", "203", "204", "205",
                "206", "207", "208", "226", "300", "301", "302", "303", "304", "305",
                "307", "308", "400", "401", "402", "403", "404", "405", "406", "407",
                "408", "409", "410", "411", "412", "413", "414", "415", "416", "417",
                "418", "421", "422", "423", "424", "426", "428", "429", "431", "444",
                "451", "499", "500", "501", "502", "503", "504", "505", "506", "507",
                "508", "510", "511", "599");
        REF_TO_MESSAGE = Arrays.asList(
                "Unspecified", "Continue", "Switching Protocols", "Processing", "OK", "Created", "Accepted", "Non-authoritative Information", "No Content", "Reset Content",
                "Partial Content", "Multi-Status", "Already Reported", "IM Used", "Multiple Choices", "Moved Permanently", "Found", "See Other", "Not Modified", "Use Proxy",
                "Temporary Redirect", "Permanent Redirect", "Bad Request", "Unauthorized", "Payment Required", "Forbidden", "Not Found", "Method Not Allowed", "Not Acceptable", "Proxy Authentication Required",
                "Request Timeout", "Conflict", "Gone", "Length Required", "Precondition Failed", "Payload Too Large", "Request-URI Too Long", "Unsupported Media Type", "Requested Range Not Satisfiable", "Expectation Failed",
                "I'm a teapot", "Misdirected Request", "Unprocessable Entity", "Locked", "Failed Dependency", "Upgrade Required", "Precondition Required", "Too Many Requests", "Request Header Fields Too Large", "Connection Closed Without Response",
                "Unavailable For Legal Reasons", "Client Closed Request", "Internal Server Error", "Not Implemented", "Bad Gateway", "Service Unavailable", "Gateway Timeout", "HTTP Version Not Supported", "Variant Also Negotiates", "Insufficient Storage",
                "Loop Detected", "Not Extended", "Network Authentication Required", "Network Connect Timeout Error");
        for (int i = 0; i < REF_TO_CODE.size(); ++i) {
            String code = REF_TO_CODE.get(i);
            String message = REF_TO_MESSAGE.get(i);
            CODE_TO_REF.put(code, i);
            MESSAGE_TO_CODE.put(message, code);
        }
    }

    public StatusCodeLookup() {
        customCodes = new ArrayList<>();
        customMessages = new ArrayList<>();
    }

    public StatusCodeLookup(Collection<CharSequence> inCustomCodes, Collection<CharSequence> inCustomMessages) {
        if ((inCustomCodes == null || inCustomCodes.isEmpty())
                && inCustomMessages == null || inCustomMessages.isEmpty()) {
            customCodes = new ArrayList<>();
            customMessages = new ArrayList<>();
            return;
        } else if (inCustomCodes == null || inCustomMessages == null) {
            throw new IllegalArgumentException("Custom Codes and Messages are uneven.");
        } else if (inCustomCodes.size() != inCustomMessages.size()) {
            throw new IllegalArgumentException("Custom Codes and Messages are uneven.");
        }
        customCodes = new ArrayList<>(inCustomCodes);
        customMessages = new ArrayList<>(inCustomMessages);
        // TODO if StatusCodeLookup is split into a builder and a reader, then
        // we won't have to build this map, which basically go unused when
        // getting back codes and messages from refs.
        for (int i = 0; i < customCodes.size(); ++i) {
            int ref = CUSTOM_START_REF + (i * CUSTOM_SPAN);
            String key = calcCodeAndMessage(customCodes.get(i), customMessages.get(i));
            customLookup.put(key, ref);
        }
    }

    public Integer getRef(CharSequence code, CharSequence message) {
        // Don't attempt to do any lookup if there is no code or message.
        if (code == null && message == null) {
            return -1;
        } else if (code == null) {
            // If we have the message but not code, then look up the code.
            code = MESSAGE_TO_CODE.getOrDefault(message.toString(), "");
        }
        Integer ref = CODE_TO_REF.get(code.toString());
        // If there is no ref, or if there is a ref, but the message is
        // different than the normal message that accompanies the status, then
        // add it to the custom list of statuses and messages.
        if (ref == null || (message != null && !message.toString().equals(REF_TO_MESSAGE.get(ref)))) {
            String msg;
            if (message != null) {
                msg = message.toString();
            } else {
                msg = "";
            }
            String codeAndMessage = calcCodeAndMessage(code, msg);
            ref = customLookup.get(codeAndMessage);
            if (ref == null) {
                ref = CUSTOM_START_REF + (customCodes.size() * CUSTOM_SPAN);
                customLookup.put(codeAndMessage, ref);
                customCodes.add(code.toString());
                customMessages.add(msg);
            }
        }
        return ref;
    }

    public CharSequence getCode(Integer ref) {
        if (ref == null || ref == -1) {
            return "";
        }
        if (0 <= ref && ref <= REF_TO_CODE.size()) {
            return REF_TO_CODE.get(ref);
        }
        return customCodes.get((ref - CUSTOM_START_REF) / CUSTOM_SPAN);
    }

    public CharSequence getMessage(Integer ref) {
        if (ref == null || ref == -1) {
            return "";
        }
        if (0 <= ref && ref <= REF_TO_CODE.size()) {
            return REF_TO_MESSAGE.get(ref);
        }
        return customMessages.get((ref - CUSTOM_START_REF) / CUSTOM_SPAN);
    }

    public List<CharSequence> getCustomCodes() {
        return Collections.unmodifiableList(customCodes);
    }

    public List<CharSequence> getCustomMessages() {
        return Collections.unmodifiableList(customMessages);
    }

    private static String calcCodeAndMessage(CharSequence code, CharSequence message) {
        return code.toString() + " " + message.toString();
    }
}
