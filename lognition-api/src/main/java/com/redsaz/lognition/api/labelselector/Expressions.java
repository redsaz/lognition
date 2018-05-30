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
import java.util.Collections;
import java.util.List;

/**
 * Collection of Label Selector expressions.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class Expressions {

    private static final LabelSelectorExpression EMPTY_EXPRESSION = new EmptyExpression();
    private static final LabelSelectorExpression AND_EXPRESSION = new AndExpression();

    public static LabelSelectorExpression empty() {
        return EMPTY_EXPRESSION;
    }

    public static LabelSelectorExpression and() {
        return AND_EXPRESSION;
    }

    public static LabelSelectorExpression exists(String labelName) {
        return new ExistsExpression(labelName);
    }

    public static LabelSelectorExpression notExists(String labelName) {
        return new NotExistsExpression(labelName);
    }

    public static LabelSelectorExpression equals(String labelName, String labelValue) {
        return new EqualsExpression(labelName, labelValue);
    }

    public static LabelSelectorExpression notEquals(String labelName, String labelValue) {
        return new NotEqualsExpression(labelName, labelValue);
    }

    public static LabelSelectorExpression in(String labelName, List<String> labelValues) {
        return new InExpression(labelName, labelValues);
    }

    public static LabelSelectorExpression notIn(String labelName, List<String> labelValues) {
        return new NotInExpression(labelName, labelValues);
    }

    private static class ExistsExpression implements LabelSelectorExpression {

        private final String name;

        public ExistsExpression(String labelName) {
            name = labelName;
        }

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.exists(name);
        }

    }

    private static class NotExistsExpression implements LabelSelectorExpression {

        private final String name;

        public NotExistsExpression(String labelName) {
            name = labelName;
        }

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.notExists(name);
        }

    }

    private static class EqualsExpression implements LabelSelectorExpression {

        private final String name;
        private final String value;

        public EqualsExpression(String labelName, String labelValue) {
            name = labelName;
            value = labelValue;
        }

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.equals(name, value);
        }

    }

    private static class NotEqualsExpression implements LabelSelectorExpression {

        private final String name;
        private final String value;

        public NotEqualsExpression(String labelName, String labelValue) {
            name = labelName;
            value = labelValue;
        }

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.notEquals(name, value);
        }

    }

    private static class InExpression implements LabelSelectorExpression {

        private final String name;
        private final List<String> values;

        public InExpression(String labelName, List<String> labelValues) {
            name = labelName;
            values = Collections.unmodifiableList(new ArrayList<>(labelValues));
        }

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.in(name, values);
        }
    }

    private static class NotInExpression implements LabelSelectorExpression {

        private final String name;
        private final List<String> values;

        public NotInExpression(String labelName, List<String> labelValues) {
            name = labelName;
            values = Collections.unmodifiableList(new ArrayList<>(labelValues));
        }

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.notIn(name, values);
        }
    }

    private static class EmptyExpression implements LabelSelectorExpression {

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.finish();
        }
    }

    private static class AndExpression implements LabelSelectorExpression {

        @Override
        public void consume(LabelSelectorExpressionListener listener) {
            listener.and();
        }
    }
}
