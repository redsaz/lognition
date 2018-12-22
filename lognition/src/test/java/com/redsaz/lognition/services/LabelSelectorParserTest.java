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
import com.redsaz.lognition.api.labelselector.LabelSelectorSyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LabelSelectorParserTest extends Assert {

    private static final Logger LOG = LoggerFactory.getLogger(LabelSelectorParserTest.class);

    @Test(dataProvider = "labels")
    public void testParse(String labels, String expected, String description) {
        LabelSelectorExpression expression = LabelSelectorParser.parse(labels);
        LabelParsedPrinter lpp = new LabelParsedPrinter();
        expression.consume(lpp);

        String actual = lpp.text();
        assertEquals(actual, expected, description);
    }

    @Test(dataProvider = "badLabels")
    public void testParseFail(String labels, String reason) {
        try {
            LabelSelectorParser.parse(labels);
            fail("Expected exception for expression: " + labels + " because: " + reason);
        } catch (LabelSelectorSyntaxException ex) {
            LOG.info(ex.getMessage());
            assertEquals(ex.getMessage(), reason);
        }
    }

    @DataProvider(parallel = true, name = "labels")
    public static Object[][] labels() {
        return new Object[][]{
            {"", ";", "Test empty selector"},
            {"alpha", "alpha;", "Test single exists"},
            {"alpha, beta", "alpha, beta;", "Test multiple exists"},
            {"!alpha", "!alpha;", "Test single notExists"},
            {"!alpha, !beta", "!alpha, !beta;", "test multiple notExists"},
            {"alpha=value", "alpha = value;", "Test single equals"},
            {"alpha=value, beta=value2", "alpha = value, beta = value2;", "Test multiple equals"},
            {"alpha!=value", "alpha != value;", "Test single notEquals"},
            {"alpha!=value, beta!=value2", "alpha != value, beta != value2;", "Test multiple notEquals"},
            {"alpha in (1)", "alpha in (1);", "Test single in, single value"},
            {"alpha in (1, two, 3, 4)", "alpha in (1, two, 3, 4);", "Test single in, multiple value"},
            {"alpha in (1, two, 3, 4), beta in (5, six, 7, 8)", "alpha in (1, two, 3, 4), beta in (5, six, 7, 8);", "Test single in, multiple value"},
            {"alpha notin (1)", "alpha notin (1);", "Test single notin, single value"},
            {"alpha notin (1, 2, 3, 4)", "alpha notin (1, 2, 3, 4);", "Test single notin, multiple value"},
            {"alpha notin (1, two, 3, 4), beta notin (5, six, 7, 8)", "alpha notin (1, two, 3, 4), beta notin (5, six, 7, 8);", "Test single in, multiple value"},
            {"1j.2e_3b-4y=6a.7b_8c-9d", "1j.2e_3b-4y = 6a.7b_8c-9d;", "Test names and values can have dots, underscores, and dashes"},
            {"12=12", "12 = 12;", "Test names and values can be purely numbers"},
            {"alpha in (6a.7b_8c-9d)", "alpha in (6a.7b_8c-9d);", "Test in values can have dots, underscores, and dashes"},
            {"fred!=value, 1j2e3b4=honda, alpha,ge.or.ge in (hol_lyw-ood, mike), 1234 notin (mixins),!marty     ,marvin, john, petty, endy1, endy2",
                "fred != value, 1j2e3b4 = honda, alpha, ge.or.ge in (hol_lyw-ood, mike), 1234 notin (mixins), !marty, marvin, john, petty, endy1, endy2;",
                "Test all expression types"},
            {"alpha\t=\t3", "alpha = 3;", "Test tabs are acceptable whitespace"},
            {" alpha \t =\t\t 4\t ", "alpha = 4;", "Test a mixutre of whitespace (including at beginning and end) are acceptable."},
            {"alpha\n=\n5", "alpha = 5;", "Turns out newlines are acceptable whitespace too."}
        };
    }

    @DataProvider(parallel = true, name = "badLabels")
    public static Object[][] badLabels() {
        return new Object[][]{
            {"alpha beta", "Label Selector syntax error. Expected one of: [\",\", \"=\", \"!=\", \"in\", \"notin\", end-of-line] but got: beta instead."},
            {"!alpha !beta,", "Label Selector syntax error. Expected , or end-of-line but got ! instead."},
            {"!", "Label Selector syntax error. Expected label key but got end-of-line instead."},
            {"alpha!", "Label Selector syntax error. Expected one of: [\",\", \"=\", \"!=\", \"in\", \"notin\", end-of-line] but got: ! instead."},
            {"alpha =", "Label Selector syntax error. Expected label value but got end-of-line instead."},
            {"alpha ==", "Label Selector syntax error. Expected label value but got = instead."},
            {"=", "Label Selector syntax error. Expected ! or label key but got = instead."},
            {"alpha !=", "Label Selector syntax error. Expected label value but got end-of-line instead."},
            {"alpha !==", "Label Selector syntax error. Expected label value but got = instead."},
            {"!=", "Label Selector syntax error. Expected ! or label key but got != instead."},
            {"alpha in 1", "Label Selector syntax error. Expected ( but got 1 instead."},
            {"alpha in in (1)", "Label Selector syntax error. Expected ( but got in instead."},
            {"alpha in )", "Label Selector syntax error. Expected ( but got ) instead."},
            {"alpha in (", "Label Selector syntax error. Expected label value but got end-of-line instead."},
            {"alpha in (,)", "Label Selector syntax error. Expected label value but got , instead."},
            {"alpha in (1", "Label Selector syntax error. Expected , or ) but got end-of-line instead."},
            {"alpha in ()", "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."},
            {"alpha in (1,)", "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."},
            {"alpha (1)", "Label Selector syntax error. Expected one of: [\",\", \"=\", \"!=\", \"in\", \"notin\", end-of-line] but got: ( instead."},
            {"in", "Label Selector syntax error. Expected ! or label key but got in instead."},
            {"(1)", "Label Selector syntax error. Expected ! or label key but got ( instead."},
            {"alpha notin 1", "Label Selector syntax error. Expected ( but got 1 instead."},
            {"alpha notin notin (1)", "Label Selector syntax error. Expected ( but got notin instead."},
            {"alpha notin )", "Label Selector syntax error. Expected ( but got ) instead."},
            {"alpha notin (", "Label Selector syntax error. Expected label value but got end-of-line instead."},
            {"alpha notin (,)", "Label Selector syntax error. Expected label value but got , instead."},
            {"alpha notin (1", "Label Selector syntax error. Expected , or ) but got end-of-line instead."},
            {"alpha notin ()", "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."},
            {"alpha notin (1,)", "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."},
            {"notin", "Label Selector syntax error. Expected ! or label key but got notin instead."},
            {"alpha,,", "Label Selector syntax error. Expected ! or label key but got , instead."},
            {",", "Label Selector syntax error. Expected ! or label key but got , instead."},
            {"@", "Illegal character: @"},
            {"#", "Illegal character: #"},
            {"$", "Illegal character: $"},
            {"%", "Illegal character: %"},
            {"^", "Illegal character: ^"},
            {"&", "Illegal character: &"},
            {"*", "Illegal character: *"},
            {"+", "Illegal character: +"},
            {"{", "Illegal character: {"},
            {"}", "Illegal character: }"},
            {"[", "Illegal character: ["},
            {"]", "Illegal character: ]"},
            {"|", "Illegal character: |"},
            {"\\", "Illegal character: \\"},
            {";", "Illegal character: ;"},
            {":", "Illegal character: :"},
            {"'", "Illegal character: '"},
            {"\"", "Illegal character: \""},
            {"<", "Illegal character: <"},
            {">", "Illegal character: >"},
            {"?", "Illegal character: ?"},
            {"/", "Illegal character: /"},
            {"`", "Illegal character: `"},
            {"~", "Illegal character: ~"},
            {"\u00E4", "Illegal character: \u00E4"}, // Only a-zA-Z0-9.-_ allowed
            {"@#$%^", "Illegal character: @"} // There can be a string of characters in violation, print first one.
        };
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
