/*
 * Copyright 2018 Redsaz <redsaz@gmail.com>.
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
package com.redsaz.lognition.api.labelselector;

import java.util.List;

/**
 * Given a label expression, formats it to a human readable output.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class LabelSelectorExpressionFormatter {

    // Do not allow Util class to be instantiated.
    private LabelSelectorExpressionFormatter() {
    }

    /**
     * Given a label expression, formats it to a human readable output.
     *
     * @param lse the expression
     * @return the formated expression.
     */
    public static String format(LabelSelectorExpression lse) {
        if (lse == null) {
            return null;
        }
        LabelParsedPrinter lpp = new LabelParsedPrinter();
        lse.consume(lpp);
        return lpp.text();
    }

    private static class LabelParsedPrinter implements LabelSelectorExpressionListener {

        private StringBuilder sb = new StringBuilder();

        public String text() {
            return sb.toString();
        }

        @Override
        public void and() {
            sb.append(", ");
        }

        @Override
        public void exists(String labelName) {
            sb.append(labelName);
        }

        @Override
        public void notExists(String labelName) {
            sb.append("!").append(labelName);
        }

        @Override
        public void equals(String labelName, String labelValue) {
            sb.append(labelName).append(" = ").append(labelValue);
        }

        @Override
        public void notEquals(String labelName, String labelValue) {
            sb.append(labelName).append(" != ").append(labelValue);
        }

        @Override
        public void in(String labelName, List<String> labelValues) {
            sb.append(labelName).append(" in (").append(String.join(", ", labelValues) + ")");
        }

        @Override
        public void notIn(String labelName, List<String> labelValues) {
            sb.append(labelName).append(" notin (").append(String.join(", ", labelValues) + ")");
        }

        @Override
        public void finish() {
        }

    }

}
