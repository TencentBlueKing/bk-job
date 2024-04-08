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

/**
 * Lexer represents the Lexer struct for label selector.
 * It contains necessary information to tokenize the input string
 */
public class Lexer {
    /**
     * s stores the string to be tokenized
     */
    private final String s;
    /**
     * pos is the position currently tokenized
     */
    private int pos;

    public Lexer(String s) {
        this.s = s;
        this.pos = 0;
    }

    /**
     * read returns the character currently lexed
     * increment the position and check the buffer overflow
     */
    private char read() {
        char ch = 0;
        if (pos < s.length()) {
            ch = s.charAt(pos);
            pos++;
        }
        return ch;
    }

    /**
     * unread 'undoes' the last read character
     */
    private void unread() {
        pos--;
    }

    /**
     * isWhitespace returns true if the rune is a space, tab, or newline
     */
    private boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
    }

    /**
     * isSpecialSymbol detects if the character ch can be an operator
     */
    private boolean isSpecialSymbol(char ch) {
        switch (ch) {
            case '=':
            case '!':
            case '(':
            case ')':
            case ',':
            case '>':
            case '<':
                return true;
        }
        return false;
    }

    public ScannedItem lex() {
        char ch = skipWhiteSpaces(read());
        if (ch == 0) {
            return new ScannedItem(Token.EndOfStringToken, "");
        }
        if (isSpecialSymbol(ch)) {
            unread();
            return scanSpecialSymbol();
        } else {
            unread();
            return scanIDOrKeyword();
        }
    }

    /**
     * scanIDOrKeyword scans string to recognize literal token (for example 'in') or an identifier.
     */
    private ScannedItem scanIDOrKeyword() {
        StringBuilder buffer = new StringBuilder();
        while (true) {
            char ch = read();
            if (ch == 0) {
                break;
            }
            if (isSpecialSymbol(ch) || isWhitespace(ch)) {
                unread();
                break;
            } else {
                buffer.append(ch);
            }
        }
        String s = buffer.toString();
        if (Token.string2token.containsKey(s)) {
            return new ScannedItem(Token.string2token.get(s), s);
        }
        return new ScannedItem(Token.IdentifierToken, s);
    }

    /**
     * scanSpecialSymbol scans string starting with special symbol.
     * special symbol identify non literal operators. "!=", "==", "="
     */
    private ScannedItem scanSpecialSymbol() {
        ScannedItem lastScannedItem = new ScannedItem();
        StringBuilder buffer = new StringBuilder();
        while (true) {
            char ch = read();
            if (ch == 0) {
                break;
            }
            if (isSpecialSymbol(ch)) {
                buffer.append(ch);
                if (Token.string2token.containsKey(buffer.toString())) {
                    lastScannedItem = new ScannedItem(Token.string2token.get(buffer.toString()), buffer.toString());
                } else if (lastScannedItem.getToken() != null) {
                    unread();
                    break;
                }
            } else {
                unread();
                break;
            }
        }
        if (lastScannedItem.getToken() == null) {
            return new ScannedItem(Token.ErrorToken, "");
        }
        return lastScannedItem;
    }

    /**
     * skipWhiteSpaces consumes all blank characters
     * returning the first non blank character
     */
    private char skipWhiteSpaces(char ch) {
        while (true) {
            if (!isWhitespace(ch)) {
                return ch;
            }
            ch = read();
        }
    }
}
