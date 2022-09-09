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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
public class ComposibleLabelSelectorExpression implements LabelSelectorExpression {

  List<LabelSelectorExpression> expressions = new ArrayList<>();

  @Override
  public void consume(LabelSelectorExpressionListener listener) {
    if (expressions.isEmpty()) {
      listener.finish();
      return;
    }
    expressions.stream()
        .forEach(
            (t) -> {
              t.consume(listener);
            });
  }

  public void addExpression(LabelSelectorExpression expression) {
    expressions.add(expression);
  }
}
