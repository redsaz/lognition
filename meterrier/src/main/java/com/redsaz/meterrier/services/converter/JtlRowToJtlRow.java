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
package com.redsaz.meterrier.services.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts a JTL Row into a different JTL Row with potentially different
 * columns.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
class JtlRowToJtlRow {

    private final List<Integer> cols = new ArrayList<>();
    private final List<JtlType> jtlTypes = new ArrayList<>();

    public JtlRowToJtlRow(String[] headers, JtlType... columns) {
        Set<JtlType> colSet = new HashSet<>(Arrays.asList(columns));
        for (int i = 0; i < headers.length; ++i) {
            String header = headers[i];
            JtlType jtlType = JtlType.fromHeader(header);
            if (jtlType != null && colSet.contains(jtlType)) {
                cols.add(i);
                jtlTypes.add(jtlType);
            }
        }
    }

    public String[] getHeaders() {
        String[] outputRow = new String[cols.size()];
        for (int i = 0; i < cols.size(); ++i) {
            outputRow[i] = jtlTypes.get(i).csvName();
        }
        return outputRow;
    }

    public String[] convert(String[] row) {
        String[] outputRow = new String[cols.size()];
        for (int i = 0; i < cols.size(); ++i) {
            int col = cols.get(i);
            String value = row[col];
            outputRow[i] = value;
        }
        return outputRow;
    }

}
