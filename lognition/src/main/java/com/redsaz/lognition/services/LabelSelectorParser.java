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

import com.redsaz.lognition.api.labelselector.ComposibleLabelSelectorExpression;
import com.redsaz.lognition.api.labelselector.Expressions;
import com.redsaz.lognition.api.labelselector.LabelSelectorExpression;
import com.redsaz.lognition.api.labelselector.LabelSelectorExpressionListener;
import com.redsaz.lognition.api.labelselector.LabelSelectorSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Given a Label Selector string like "key1 = value1, key2 = value2", transforms the string into a
 * form that can be used to find logs with those keys and values.
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class LabelSelectorParser {

    // Do not allow utility classes to be instanciated.
    private LabelSelectorParser() {
    }

    public static void main(String[] args) {
        String body = "fred!=value, 1j2e3b4=honda, ge.or.ge in (hol_lyw-ood, mike), 1234 notin (mixins),!marty     ,marvin";
        List<Token> tokens = tokenize(body);
        LabelSelectorExpression expression = parsize(tokens);
        expression.consume(new LabelSelectorExpressionListener() {
            @Override
            public void and() {
                System.out.print(", ");
            }

            @Override
            public void exists(String labelName) {
                System.out.print(labelName);
            }

            @Override
            public void notExists(String labelName) {
                System.out.print("!" + labelName);
            }

            @Override
            public void equals(String labelName, String labelValue) {
                System.out.print(labelName + " = " + labelValue);
            }

            @Override
            public void notEquals(String labelName, String labelValue) {
                System.out.print(labelName + " != " + labelValue);
            }

            @Override
            public void in(String labelName, List<String> labelValues) {
                System.out.print(labelName + " in (" + String.join(", ", labelValues) + ")");
            }

            @Override
            public void notIn(String labelName, List<String> labelValues) {
                System.out.print(labelName + " notin (" + String.join(", ", labelValues) + ")");
            }

            @Override
            public void finish() {
                System.out.println();
            }
        });
    }
    private static final Pattern LABEL_SELECTOR_PATTERN = Pattern.compile("(!=|[,()=!]|in|notin)|([-._a-zA-Z0-9]+)|(\\S)", Pattern.CASE_INSENSITIVE);

    private static final Map<String, Token> OP_LOOKUP = initOpLookup();

    public static LabelSelectorExpression parse(String labelSelector) {
        return parsize(tokenize(labelSelector));
    }

    private static Map<String, Token> initOpLookup() {
        Map<String, Token> map = new HashMap<>();
        map.put("!=", Tokens.NOT_EQUALS);
        map.put("=", Tokens.EQUALS);
        map.put(",", Tokens.AND);
        map.put("(", Tokens.LIST_OPEN);
        map.put(")", Tokens.LIST_CLOSE);
        map.put("!", Tokens.NOT_EXISTS);
        map.put("in", Tokens.IN);
        map.put("notin", Tokens.NOT_IN);

        return map;
    }

    private static List<Token> tokenize(String selector) {
        Matcher matcher = LABEL_SELECTOR_PATTERN.matcher(selector);
        List<Token> tokens = new ArrayList<>();
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value != null) {
                Token token = OP_LOOKUP.get(value);
                tokens.add(token);
                if (token == null) {
                    throw new LabelSelectorSyntaxException("Illegal character: " + value);
                }
                continue;
            }
            value = matcher.group(2);
            if (value != null) {
                tokens.add(new KeyOrValueToken(value));
                continue;
            }
            value = matcher.group(3);
            if (value != null) {
                throw new LabelSelectorSyntaxException("Illegal character: " + value);
            }
        }

        return tokens;
    }

    private static LabelSelectorExpression parsize(List<Token> tokens) {
        /*
           Types:
           equality/inequality - key {=,!=} value
           set - key {in,notin} ( [value1[, value2[,valueN]]] )
           exists/not-exists - [!]key
           and - above-statement, above-statement
           empty - does nothing
         */
        // If there are no tokens, then the empty case means no logs
        if (tokens.isEmpty()) {
            return Expressions.empty();
        }

        State state = States.START;
        ComposibleLabelSelectorExpression expression = new ComposibleLabelSelectorExpression();
        for (Token token : tokens) {
            state = state.expressionize(expression, token);
        }
        // If we're in the middle of an expression, then it needs to be concluded properly:
        // If this an "exists" state, and we got the label name, then it can be finished.
        // If this a "chainable" state, meaning the last expression is completed and can accept
        // a comma, then we're finished. Any other state should be an error.
        state = state.expressionize(expression, Tokens.END);
        if (state != States.FINISH) {
            throw new LabelSelectorSyntaxException("Label Selector is malformed.");
        }

        return expression;
        // key1 = value1
        // In SQL:
        // SELECT DISTINCT log_id FROM label WHERE key = 'key1' AND value = 'value1'
        //
        // key2 in (value2a, value2b)
        // In SQL:
        // SELECT DISTINCT log_id FROM label WHERE key = 'key2' AND value in ('value2a', 'value2b')
        //
        // key3
        // In SQL:
        // SELECT DISTINCT log_id FROM label WHERE key = 'key3'
        //
        // key1 != value1
        // In SQL:
        // SELECT DISTINCT log_id FROM label WHERE key = 'key1' AND value <> 'value1'
        //
        // key2 notin (value2a, value2b)
        // In SQL:
        // SELECT DISTINCT log_id FROM label WHERE key = 'key1' AND value NOT IN 'value1'
        //
        // !key3
        // In SQL:
        // SELECT DISTINCT log_id FROM label l1 LEFT JOIN label l2 ON l1.log_id = l2.log_id AND l2.key = 'key1' WHERE l2.key IS NULL;
        // Or:
        // SELECT DISTINCT log_id FROM label WHERE NOT EXISTS (SELECT log_id FROM label WHERE key = 'key3')
        //
        // key1 = value1, key2 = value2
        // In SQL:
        // SELECT DISTICT log_id FROM label AS l
        // WHERE l.log_id IN
        // (SELECT DISTINCT log_id
        //   FROM label
        //   WHERE key = 'key1' AND value = 'value1')
        // AND l.log_id IN
        // (SELECT DISTINCT log_id
        //   FROM label
        //   WHERE key = 'key2' AND value = 'value2')
    }

    private static interface Token {

        String value();

    }

    /**
     * Determines how to parse the token by determining what point in the expression we are.
     * <p>
     * That is, given this text <code>key1 = value1</code>, split into these strings: "key1", "=",
     * "value1", and we're at the START state, we know that we can only accept a few patterns of
     * strings. We can't accept "=", "!=", "(", or ")" because those are only acceptable at specific
     * points later in the expression, but not the first. However, we CAN accept [0-9a-zA-Z_-.]+
     * because that fits what can be a KEY token. But, that same pattern is also a valid VALUE
     * token. We know that it isn't a VALUE token because we're at the START state, where only a KEY
     * token or a NOT_EXISTS token are possible.
     * <p>
     * To advance from START state, we can go to LABEL_KEY state. Acceptable pretokens can now be
     * "=", "!=", "in", "notin", ",", or absolutely nothing (if nothing, the key is part of an
     * "exists" expression).
     * <p>
     * So, to use States, start with the START state, feed in a pretoken to get the token and state,
     * then parse the next token based on the new state, and repeat until all pretokens are
     * converted:
     * <pre>
     * State state = States.START;
     * List<Token> tokens = new ArrayList<>();
     * for (String pretoken : pretokens) {
     *     TokenAndState ts = state.toToken(pretoken);
     *     tokens.add(ts.token());
     *     state = ts.state();
     * }
     * </pre>
     */
    private static interface State {

        /**
         * Returns a token and parsing state based on the state of parsing and the pretoken value.
         *
         * @param pretoken subject to convert
         * @return a token and new state if pretoken was valid, or throws an exception if not.
         */
        State expressionize(ComposibleLabelSelectorExpression expression, Token token);
    }

    private static class KeyOrValueToken implements Token {

        private final String value;

        public KeyOrValueToken(String inValue) {
            value = inValue;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private static enum Tokens implements Token {
        END {
            @Override
            public String value() {
                return "";
            }
        },
        EQUALS {
            @Override
            public String value() {
                return "=";
            }
        },
        NOT_EQUALS {
            @Override
            public String value() {
                return "!=";
            }
        },
        IN {
            @Override
            public String value() {
                return "=";
            }
        },
        NOT_IN {
            @Override
            public String value() {
                return "!=";
            }
        },
        NOT_EXISTS {
            @Override
            public String value() {
                return "!";
            }
        },
        LIST_OPEN {
            @Override
            public String value() {
                return "(";
            }
        },
        LIST_CLOSE {
            @Override
            public String value() {
                return ")";
            }
        },
        AND {
            @Override
            public String value() {
                return ",";
            }
        };

    }

    /**
     * At this point in parsing, the given token is a label name. It's possible to be an
     * ExistsExpression or one of the *OpExpression.
     */
    private static class NameState implements State {

        private final Token name;

        public NameState(Token inName) {
            name = inName;
        }

        @Override
        public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
            State state;
            if (token == Tokens.EQUALS || token == Tokens.NOT_EQUALS) {
                state = new ScalarOpState(this, token);
            } else if (token == Tokens.IN || token == Tokens.NOT_IN) {
                state = new VectorOpState(this, token);
            } else if (token == Tokens.AND) {
                expression.addExpression(Expressions.exists(name.value()));
                state = States.CHAINABLE;
            } else if (token == Tokens.END) {
                expression.addExpression(Expressions.exists(name.value()));
                state = States.FINISH;
            } else {
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + Tokens.NOT_EXISTS + " or label key but got " + token + " instead.");
            }

            return state;
        }
    }

    /**
     * At this point in parsing, we have a label name, an op. Now ready for the label value.
     */
    private static class ScalarOpState implements State {

        private final NameState name;

        private final Token op;

        public ScalarOpState(NameState inName, Token inOp) {
            name = inName;
            op = inOp;
        }

        @Override
        public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
            State state;
            if (token instanceof KeyOrValueToken) {
                if (op == Tokens.EQUALS) {
                    expression.addExpression(Expressions.equals(name.name.value(), token.value()));
                } else {
                    expression.addExpression(Expressions.notEquals(name.name.value(), token.value()));
                }
                state = States.CHAINABLE;
            } else {
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + "label value but got " + token + " instead.");
            }

            return state;
        }
    }

    /**
     * At this point in parsing, we have a label name, an op. Now ready for the label value.
     */
    private static class VectorOpState implements State {

        private final NameState name;

        private final Token op;

        private final List<String> items;

        private State listState;

        public VectorOpState(NameState inName, Token inOp) {
            name = inName;
            op = inOp;
            items = new ArrayList<>();
        }

        @Override
        public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
            if (listState != null) {
                listState = listState.expressionize(expression, token);
            } else if (token == Tokens.LIST_OPEN) {
                listState = States.LIST_START;
            } else {
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + Tokens.LIST_OPEN + " but got " + token + " instead.");
            }
            State state = this;
            if (listState == States.LIST_CHAINABLE) {
                items.add(token.value());
            } else if (listState == States.CHAINABLE) {
                if (op == Tokens.IN) {
                    expression.addExpression(Expressions.in(name.name.value(), items));
                } else {
                    expression.addExpression(Expressions.notIn(name.name.value(), items));
                }
                state = States.CHAINABLE;
            }
            return state;
        }
    }

    private static enum States implements State {
        START {
            @Override
            public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
                if (Tokens.NOT_EXISTS == token) {
                    return States.NOT_EXISTS;
                } else if (token instanceof KeyOrValueToken) {
                    return new NameState(token);
                }
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + Tokens.NOT_EXISTS + " or label key but got " + token + " instead.");
            }

        },
        FINISH {
            @Override
            public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
                if (Tokens.END == token) {
                    // It's weird that YET ANOTHER "END" token was encountered, but whatever.
                    return States.FINISH;
                }
                throw new LabelSelectorSyntaxException("Label Selector is finished, yet "
                        + "encountered: " + token);
            }
        },
        NOT_EXISTS {
            @Override
            public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
                if (token instanceof KeyOrValueToken) {
                    expression.addExpression(Expressions.notExists(token.value()));
                    return States.CHAINABLE;
                }
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + " label value but got " + token + " instead.");
            }
        },
        LIST_START {
            @Override
            public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
                if (token instanceof KeyOrValueToken) {
                    return States.LIST_CHAINABLE;
                } else if (token == Tokens.LIST_CLOSE) {
                    throw new LabelSelectorSyntaxException("Label Selector syntax error. Cannot "
                            + "close list when there are no items in list or list ends with comma.");
                }
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + "label value but got " + token + " instead.");
            }
        },
        LIST_CHAINABLE {
            @Override
            public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
                if (token == Tokens.AND) {
                    return States.LIST_START;
                } else if (token == Tokens.LIST_CLOSE) {
                    return States.CHAINABLE;
                }
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + Tokens.AND + " or " + Tokens.LIST_CLOSE + " but got "
                        + token + " instead.");
            }
        },
        CHAINABLE {
            @Override
            public State expressionize(ComposibleLabelSelectorExpression expression, Token token) {
                if (token == Tokens.AND) {
                    expression.addExpression(Expressions.and());
                    return States.START;
                } else if (token == Tokens.END) {
                    return States.FINISH;
                }
                throw new LabelSelectorSyntaxException("Label Selector syntax error. Expected "
                        + Tokens.AND + " or end of line but got "
                        + token + " instead.");
            }
        };

    }

}
