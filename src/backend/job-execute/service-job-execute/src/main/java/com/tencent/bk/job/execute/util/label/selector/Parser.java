/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.util.label.selector;

import com.tencent.bk.job.common.validation.FieldError;
import com.tencent.bk.job.common.validation.FieldErrors;
import com.tencent.bk.job.common.validation.Path;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.bk.job.execute.util.label.selector.ParserContext.Values;

/**
 * Kubernetes label selector 解析
 */
public class Parser {
    private final Lexer lexer;
    private final List<ScannedItem> scannedItems;
    private int position;
    private final Path rootPath;

    public Parser(String selector) {
        this.lexer = new Lexer(selector);
        this.scannedItems = new ArrayList<>();
        this.position = 0;
        rootPath = Path.newPath("labelSelector");
    }

    /**
     * scan runs through the input string and stores the ScannedItem in an array
     * Parser can now lookahead and consume the tokens
     */
    private void scan() {
        while (true) {
            ScannedItem scannedItem = lexer.lex();
            scannedItems.add(scannedItem);
            if (scannedItem.getToken() == Token.EndOfStringToken) {
                break;
            }
        }
    }

    /**
     * lookahead func returns the current token and string. No increment of current position
     */
    private ScannedItem lookahead(ParserContext context) {
        Token token = scannedItems.get(position).getToken();
        String literal = scannedItems.get(position).getLiteral();
        if (context == Values) {
            switch (token) {
                case InToken:
                case NotInToken:
                    token = Token.IdentifierToken;
            }
        }
        return new ScannedItem(token, literal);
    }

    /**
     * consume returns current token and string. Increments the position
     */
    private ScannedItem consume(ParserContext context) {
        position++;
        Token token = scannedItems.get(position - 1).getToken();
        String literal = scannedItems.get(position - 1).getLiteral();
        if (context == Values) {
            switch (token) {
                case InToken:
                case NotInToken:
                    token = Token.IdentifierToken;
            }
        }
        return new ScannedItem(token, literal);
    }

    public List<Requirement> parse() throws LabelSelectorParseException {
        scan();

        List<Requirement> requirements = new ArrayList<>();
        while (true) {
            ScannedItem scannedItem = lookahead(Values);
            Token token = scannedItem.getToken();
            switch (token) {
                case IdentifierToken:
                case DoesNotExistToken:
                    Requirement requirement = parseRequirement();
                    requirements.add(requirement);
                    ScannedItem nextScannedItem = consume(Values);
                    Token nextToken = nextScannedItem.getToken();
                    switch (nextToken) {
                        case EndOfStringToken:
                            return requirements;
                        case CommaToken:
                            ScannedItem nextScannedItem2 = lookahead(Values);
                            Token nextToken2 = nextScannedItem2.getToken();
                            if (nextToken2 != Token.IdentifierToken && nextToken2 != Token.DoesNotExistToken) {
                                throw new LabelSelectorParseException(
                                    "found '" + nextScannedItem2.getLiteral() + "', expected: identifier after ','");
                            }
                            break;
                        default:
                            throw new LabelSelectorParseException("found '" + nextScannedItem.getLiteral()
                                + "', expected: ',' or 'end of string'");
                    }
                    break;
                case EndOfStringToken:
                    return requirements;
                default:
                    throw new LabelSelectorParseException("found '" + scannedItems.get(position).getLiteral()
                        + "', expected: !, identifier, or 'end of string'");
            }
        }
    }

    private Requirement parseRequirement() throws LabelSelectorParseException {
        KeyAndOperator keyAndOperator = parseKeyAndInferOperator();
        String key = keyAndOperator.getKey();
        Operator operator = keyAndOperator.getOperator();

        if (operator == Operator.Exists || operator == Operator.DoesNotExist) {
            return newRequirement(key, operator, null);
        }

        Operator op = parseOperator();
        List<String> values;
        switch (op) {
            case In:
            case NotIn:
                values = parseValues();
                break;
            case Equals:
            case DoubleEquals:
            case NotEquals:
            case GreaterThan:
            case LessThan:
                values = parseExactValue();
                break;
            default:
                throw new LabelSelectorParseException("found '" + scannedItems.get(position).getLiteral()
                    + "', expected: " + Arrays.stream(Operator.values())
                    .map(Operator::getSymbol).collect(Collectors.joining(", ")));
        }
        return newRequirement(key, op, values);
    }

    private Requirement newRequirement(String key, Operator op, List<String> vals) {
        FieldErrors fieldErrors = new FieldErrors();

        List<String> labelKeyValidateErrors = LabelValidator.validateLabelKey(key);
        if (CollectionUtils.isNotEmpty(labelKeyValidateErrors)) {
            fieldErrors.add(FieldError.invalid(rootPath.child("key"), vals,
                String.join("; ", labelKeyValidateErrors)));
        }

        Path valuePath = rootPath.child("values");
        switch (op) {
            case In:
            case NotIn:
                if (vals.size() == 0) {
                    fieldErrors.add(FieldError.invalid(valuePath, vals,
                        "for 'in', 'notin' operators, values set can't be empty"));
                }
                break;
            case Equals:
            case DoubleEquals:
            case NotEquals:
                if (vals.size() != 1) {
                    fieldErrors.add(FieldError.invalid(valuePath, vals,
                        "exact-match compatibility requires one single value"));
                }
                break;
            case Exists:
            case DoesNotExist:
                if (CollectionUtils.isNotEmpty(vals)) {
                    fieldErrors.add(FieldError.invalid(valuePath, vals,
                        "values set must be empty for exists and does not exist"));
                }
                break;
            case GreaterThan:
            case LessThan:
                if (vals.size() != 1) {
                    fieldErrors.add(FieldError.invalid(valuePath, vals,
                        "for 'Gt', 'Lt' operators, exactly one value is required"));
                }
                for (int i = 0; i < vals.size(); i++) {
                    String val = vals.get(i);
                    try {
                        Long.parseLong(val);
                    } catch (NumberFormatException e) {
                        fieldErrors.add(FieldError.invalid(valuePath.index(i), val,
                            "for 'Gt', 'Lt' operators, the value must be an integer"));
                    }
                }
                break;
            default:
                fieldErrors.add(FieldError.notSupported(rootPath.child("operator"), op,
                    Operator.allOperators()));
        }

        if (CollectionUtils.isNotEmpty(vals)) {
            for (int i = 0; i < vals.size(); i++) {
                String val = vals.get(i);

                List<String> valueErrors = LabelValidator.validateLabelValue(val);
                if (CollectionUtils.isNotEmpty(valueErrors)) {
                    fieldErrors.add(FieldError.invalid(valuePath.index(i), val,
                        String.join("; ", valueErrors)));
                }
            }
        }

        if (fieldErrors.hasError()) {
            throw new LabelSelectorParseException("Validate label selector fail, errors:" + fieldErrors.toString());
        }
        return new Requirement(key, op, vals);
    }

    private Operator parseOperator() throws LabelSelectorParseException {
        ScannedItem scannedItem = consume(ParserContext.KeyAndOperator);
        Token token = scannedItem.getToken();
        switch (token) {
            case InToken:
                return Operator.In;
            case EqualsToken:
                return Operator.Equals;
            case DoubleEqualsToken:
                return Operator.DoubleEquals;
            case GreaterThanToken:
                return Operator.GreaterThan;
            case LessThanToken:
                return Operator.LessThan;
            case NotInToken:
                return Operator.NotIn;
            case NotEqualsToken:
                return Operator.NotEquals;
            default:
                throw new LabelSelectorParseException("found '" + scannedItem.getLiteral()
                    + "', expected: " + Arrays.stream(Operator.values())
                    .map(Operator::getSymbol).collect(Collectors.joining(", ")));
        }
    }

    private List<String> parseValues() throws LabelSelectorParseException {
        ScannedItem scannedItem = consume(Values);
        Token token = scannedItem.getToken();
        if (token != Token.OpenParToken) {
            throw new LabelSelectorParseException(
                "found '" + scannedItem.getLiteral() + "' expected: '('");
        }
        scannedItem = lookahead(Values);
        token = scannedItem.getToken();
        switch (token) {
            case IdentifierToken:
            case CommaToken:
                List<String> values = parseIdentifiersList();
                token = consume(Values).getToken();
                if (token != Token.ClosedParToken) {
                    throw new LabelSelectorParseException(
                        "found '" + scannedItem.getLiteral() + "', expected: ')'");
                }
                return values;
            case ClosedParToken:
                consume(Values);
                return Collections.emptyList();
            default:
                throw new LabelSelectorParseException(
                    "found '" + scannedItems.get(position).getLiteral() + "', expected: ',', ')' or identifier");
        }
    }

    private List<String> parseIdentifiersList() throws LabelSelectorParseException {
        List<String> values = new ArrayList<>();
        while (true) {
            Token token = consume(Values).getToken();
            switch (token) {
                case IdentifierToken:
                    values.add(scannedItems.get(position - 1).getLiteral());
                    token = lookahead(Values).getToken();
                    switch (token) {
                        case CommaToken:
                            continue;
                        case ClosedParToken:
                            return values;
                        default:
                            throw new LabelSelectorParseException("found '" + scannedItems.get(position).getLiteral()
                                + "', expected: ',' or ')'");
                    }
                case CommaToken:
                    if (values.isEmpty()) {
                        values.add("");
                    }
                    Token token2 = lookahead(Values).getToken();
                    if (token2 == Token.ClosedParToken) {
                        values.add("");
                        return values;
                    }
                    if (token2 == Token.CommaToken) {
                        consume(Values);
                        values.add("");
                    }
                    break;
                default:
                    return values;
            }
        }
    }

    private List<String> parseExactValue() throws LabelSelectorParseException {
        List<String> values = new ArrayList<>();
        Token token = lookahead(Values).getToken();
        if (token == Token.EndOfStringToken || token == Token.CommaToken) {
            values.add("");
            return values;
        }
        token = consume(Values).getToken();
        if (token == Token.IdentifierToken) {
            values.add(scannedItems.get(position - 1).getLiteral());
            return values;
        }
        throw new LabelSelectorParseException("found '" + scannedItems.get(position).getLiteral()
            + "', expected: identifier");
    }


    /**
     * parseKeyAndInferOperator parses literals.
     * in case of no operator '!, in, notin, ==, =, !=' are found
     * the 'exists' operator is inferred
     */
    private KeyAndOperator parseKeyAndInferOperator() throws LabelSelectorParseException {
        Operator operator = null;
        ScannedItem scannedItem = consume(Values);
        Token tok = scannedItem.getToken();
        String literal = scannedItem.getLiteral();

        if (tok == Token.DoesNotExistToken) {
            operator = Operator.DoesNotExist;
            scannedItem = consume(Values);
            tok = scannedItem.getToken();
            literal = scannedItem.getLiteral();
        }

        if (tok != Token.IdentifierToken) {
            throw new LabelSelectorParseException(String.format("found '%s', expected: identifier", literal));
        }

        List<String> labelKeyValidateErrors = LabelValidator.validateLabelKey(literal);
        if (CollectionUtils.isNotEmpty(labelKeyValidateErrors)) {
            throw new LabelSelectorParseException("Invalid label key, errors:"
                + String.join("; ", labelKeyValidateErrors));
        }

        ScannedItem lookaheadScanItem = lookahead(Values);
        Token t = lookaheadScanItem.getToken();

        if (t == Token.EndOfStringToken || t == Token.CommaToken) {
            if (operator != Operator.DoesNotExist) {
                operator = Operator.Exists;
            }
        }

        return new KeyAndOperator(literal, operator);
    }

    @Data
    private static class KeyAndOperator {
        private String key;
        private Operator operator;

        public KeyAndOperator(String key, Operator operator) {
            this.key = key;
            this.operator = operator;
        }
    }
}
