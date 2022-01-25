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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.redsaz.lognition.api.labelselector.LabelSelectorExpressionListener;
import com.redsaz.lognition.api.labelselector.LabelSelectorSyntaxException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = Parameterized.class)
public class LabelSelectorParserNegativeTest {

  private static final Logger LOG = LoggerFactory.getLogger(LabelSelectorParserNegativeTest.class);

  private final String labels;
  private final String reason;

  public LabelSelectorParserNegativeTest(String labels, String reason) {
    this.labels = labels;
    this.reason = reason;
  }

  @Test
  public void testParseFail() {
    try {
      LabelSelectorParser.parse(labels);
      fail("Expected exception for expression: " + labels + " because: " + reason);
    } catch (LabelSelectorSyntaxException ex) {
      LOG.info(ex.getMessage());
      assertEquals(reason, ex.getMessage());
    }
  }

  @Parameterized.Parameters(name = "{index}: testParse: {0}")
  public static Object[][] badLabels() {
    return new Object[][] {
      {
        "alpha beta",
        "Label Selector syntax error. Expected one of: [\",\", \"=\", \"!=\", \"in\", \"notin\", end-of-line] but got: beta instead."
      },
      {
        "!alpha !beta,", "Label Selector syntax error. Expected , or end-of-line but got ! instead."
      },
      {"!", "Label Selector syntax error. Expected label key but got end-of-line instead."},
      {
        "alpha!",
        "Label Selector syntax error. Expected one of: [\",\", \"=\", \"!=\", \"in\", \"notin\", end-of-line] but got: ! instead."
      },
      {"alpha =", "Label Selector syntax error. Expected label value but got end-of-line instead."},
      {"alpha ==", "Label Selector syntax error. Expected label value but got = instead."},
      {"=", "Label Selector syntax error. Expected ! or label key but got = instead."},
      {
        "alpha !=", "Label Selector syntax error. Expected label value but got end-of-line instead."
      },
      {"alpha !==", "Label Selector syntax error. Expected label value but got = instead."},
      {"!=", "Label Selector syntax error. Expected ! or label key but got != instead."},
      {"alpha in 1", "Label Selector syntax error. Expected ( but got 1 instead."},
      {"alpha in in (1)", "Label Selector syntax error. Expected ( but got in instead."},
      {"alpha in )", "Label Selector syntax error. Expected ( but got ) instead."},
      {
        "alpha in (",
        "Label Selector syntax error. Expected label value but got end-of-line instead."
      },
      {"alpha in (,)", "Label Selector syntax error. Expected label value but got , instead."},
      {"alpha in (1", "Label Selector syntax error. Expected , or ) but got end-of-line instead."},
      {
        "alpha in ()",
        "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."
      },
      {
        "alpha in (1,)",
        "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."
      },
      {
        "alpha (1)",
        "Label Selector syntax error. Expected one of: [\",\", \"=\", \"!=\", \"in\", \"notin\", end-of-line] but got: ( instead."
      },
      {"in", "Label Selector syntax error. Expected ! or label key but got in instead."},
      {"(1)", "Label Selector syntax error. Expected ! or label key but got ( instead."},
      {"alpha notin 1", "Label Selector syntax error. Expected ( but got 1 instead."},
      {"alpha notin notin (1)", "Label Selector syntax error. Expected ( but got notin instead."},
      {"alpha notin )", "Label Selector syntax error. Expected ( but got ) instead."},
      {
        "alpha notin (",
        "Label Selector syntax error. Expected label value but got end-of-line instead."
      },
      {"alpha notin (,)", "Label Selector syntax error. Expected label value but got , instead."},
      {
        "alpha notin (1",
        "Label Selector syntax error. Expected , or ) but got end-of-line instead."
      },
      {
        "alpha notin ()",
        "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."
      },
      {
        "alpha notin (1,)",
        "Label Selector syntax error. Cannot close list when there are no items in list or list ends with comma."
      },
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
      {
        "@#$%^", "Illegal character: @"
      } // There can be a string of characters in violation, print first one.
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
