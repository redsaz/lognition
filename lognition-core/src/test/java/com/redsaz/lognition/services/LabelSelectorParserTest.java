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
package com.redsaz.lognition.services;

import com.redsaz.lognition.api.labelselector.LabelSelectorExpression;
import com.redsaz.lognition.api.labelselector.LabelSelectorExpressionListener;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class LabelSelectorParserTest {

    private final String labels;
    private final String expected;
    private final String description;

    public LabelSelectorParserTest(String labels, String expected, String description) {
        this.labels = labels;
        this.expected = expected;
        this.description = description;
    }

    @Test
    public void testParse() {
        LabelSelectorExpression expression = LabelSelectorParser.parse(labels);
        LabelParsedPrinter lpp = new LabelParsedPrinter();
        expression.consume(lpp);

        String actual = lpp.text();
        assertEquals(description, expected, actual);
    }

    @Parameterized.Parameters(name = "{index}: testParse: {2} - {0}")
    public static Iterable<Object[]> labels() {
        return Arrays.asList(
                new Object[]{"", ";", "Test empty selector"},
                new Object[]{"alpha", "alpha;", "Test single exists"},
                new Object[]{"alpha, beta", "alpha, beta;", "Test multiple exists"},
                new Object[]{"!alpha", "!alpha;", "Test single notExists"},
                new Object[]{"!alpha, !beta", "!alpha, !beta;", "test multiple notExists"},
                new Object[]{"alpha=value", "alpha = value;", "Test single equals"},
                new Object[]{"alpha=value, beta=value2", "alpha = value, beta = value2;", "Test multiple equals"},
                new Object[]{"alpha!=value", "alpha != value;", "Test single notEquals"},
                new Object[]{"alpha!=value, beta!=value2", "alpha != value, beta != value2;", "Test multiple notEquals"},
                new Object[]{"alpha in (1)", "alpha in (1);", "Test single in, single value"},
                new Object[]{"alpha in (1, two, 3, 4)", "alpha in (1, two, 3, 4);", "Test single in, multiple value"},
                new Object[]{"alpha in (1, two, 3, 4), beta in (5, six, 7, 8)", "alpha in (1, two, 3, 4), beta in (5, six, 7, 8);", "Test single in, multiple value"},
                new Object[]{"alpha notin (1)", "alpha notin (1);", "Test single notin, single value"},
                new Object[]{"alpha notin (1, 2, 3, 4)", "alpha notin (1, 2, 3, 4);", "Test single notin, multiple value"},
                new Object[]{"alpha notin (1, two, 3, 4), beta notin (5, six, 7, 8)", "alpha notin (1, two, 3, 4), beta notin (5, six, 7, 8);", "Test single in, multiple value"},
                new Object[]{"1j.2e_3b-4y=6a.7b_8c-9d", "1j.2e_3b-4y = 6a.7b_8c-9d;", "Test names and values can have dots, underscores, and dashes"},
                new Object[]{"12=12", "12 = 12;", "Test names and values can be purely numbers"},
                new Object[]{"alpha in (6a.7b_8c-9d)", "alpha in (6a.7b_8c-9d);", "Test in values can have dots, underscores, and dashes"},
                new Object[]{"fred!=value, 1j2e3b4=honda, alpha,ge.or.ge in (hol_lyw-ood, mike), 1234 notin (mixins),!marty     ,marvin, john, petty, endy1, endy2",
                    "fred != value, 1j2e3b4 = honda, alpha, ge.or.ge in (hol_lyw-ood, mike), 1234 notin (mixins), !marty, marvin, john, petty, endy1, endy2;",
                    "Test all expression types"},
                new Object[]{"alpha\t=\t3", "alpha = 3;", "Test tabs are acceptable whitespace"},
                new Object[]{" alpha \t =\t\t 4\t ", "alpha = 4;", "Test a mixutre of whitespace (including at beginning and end) are acceptable."},
                new Object[]{"alpha\n=\n5", "alpha = 5;", "Turns out newlines are acceptable whitespace too."}
        );
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
            sb.append("!" + labelName);
        }

        @Override
        public void equals(String labelName, String labelValue) {
            sb.append(labelName + " = " + labelValue);
        }

        @Override
        public void notEquals(String labelName, String labelValue) {
            sb.append(labelName + " != " + labelValue);
        }

        @Override
        public void in(String labelName, List<String> labelValues) {
            sb.append(labelName + " in (" + String.join(", ", labelValues) + ")");
        }

        @Override
        public void notIn(String labelName, List<String> labelValues) {
            sb.append(labelName + " notin (" + String.join(", ", labelValues) + ")");
        }

        @Override
        public void finish() {
            // Okay, the semi-colon cannot designate an expression end in real life. But this makes
            // it easier to test that the finish method gets called at the correct time.
            sb.append(";");
        }

    }
}
