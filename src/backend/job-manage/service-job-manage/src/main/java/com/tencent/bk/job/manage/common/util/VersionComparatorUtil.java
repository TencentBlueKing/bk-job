/*
 *
 *  * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *  *
 *  * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *  *
 *  * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *  *
 *  * License for BK-JOB蓝鲸智云作业平台:
 *  * --------------------------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 *  * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  * IN THE SOFTWARE.
 *
 */

package com.tencent.bk.job.manage.common.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * 版本号比较工具
 */
public class VersionComparatorUtil implements Comparable<VersionComparatorUtil> {

    private String value;
    private ListItem items;

    private interface Item {
        int INT_ITEM = 1;
        int STRING_ITEM = 2;
        int LIST_ITEM = 3;

        int compareTo(Item item);

        int getType();

        boolean isNull();
    }

    private static class IntItem implements Item {
        private final int value;

        public static final IntItem ZERO = new IntItem();

        private IntItem() {
            this.value = 0;
        }

        IntItem(String str) {
            this.value = Integer.parseInt(str);
        }

        @Override
        public int getType() {
            return INT_ITEM;
        }

        @Override
        public boolean isNull() {
            return value == 0;
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                // 1.0 == 1, 1.1 > 1
                return (value == 0) ? 0 : 1;
            }

            switch (item.getType()) {
                case INT_ITEM:
                    int itemValue = ((IntItem) item).value;
                    return (value < itemValue) ? -1 : ((value == itemValue) ? 0 : 1);
                case STRING_ITEM:
                    // 1.1 > 1-sp
                    return 1;
                case LIST_ITEM:
                    // 1.1 > 1-1
                    return 1;
                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            IntItem intItem = (IntItem) o;

            return value == intItem.value;

        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    private static class StringItem implements Item {
        private static final List<String> QUALIFIERS = Arrays.asList("alpha", "beta", "milestone", "rc", "");

        private static final String RELEASE_VERSION_INDEX = String.valueOf(QUALIFIERS.indexOf(""));

        private final String value;

        StringItem(String value, boolean followedByDigit) {
            if (followedByDigit && value.length() == 1) {
                // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                switch (value.charAt(0)) {
                    case 'a':
                        value = "alpha";
                        break;
                    case 'b':
                        value = "beta";
                        break;
                    case 'm':
                        value = "milestone";
                        break;
                    default:
                }
            }
            this.value = value;
        }

        @Override
        public int getType() {
            return STRING_ITEM;
        }

        @Override
        public boolean isNull() {
            return (comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX) == 0);
        }

        public static String comparableQualifier(String qualifier) {
            int i = QUALIFIERS.indexOf(qualifier);

            return i == -1 ? (QUALIFIERS.size() + "-" + qualifier) : String.valueOf(i);
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                // 1-rc < 1
                return comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX);
            }
            switch (item.getType()) {
                case INT_ITEM:
                    // 1.any < 1.1 ?
                    return -1;
                case STRING_ITEM:
                    return comparableQualifier(value).compareTo(comparableQualifier(((StringItem) item).value));
                case LIST_ITEM:
                    // 1.any < 1-1
                    return -1;
                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StringItem that = (StringItem) o;

            return value.equals(that.value);

        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        public String toString() {
            return value;
        }
    }

    private static class ListItem extends ArrayList<Item> implements Item {
        @Override
        public int getType() {
            return LIST_ITEM;
        }

        @Override
        public boolean isNull() {
            return (size() == 0);
        }

        void normalize() {
            for (int i = size() - 1; i >= 0; i--) {
                Item lastItem = get(i);

                if (lastItem.isNull()) {
                    remove(i);
                } else if (!(lastItem instanceof ListItem)) {
                    break;
                }
            }
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                if (size() == 0) {
                    // 1-0 = 1- (normalize) = 1
                    return 0;
                }
                Item first = get(0);
                return first.compareTo(null);
            }
            switch (item.getType()) {
                case INT_ITEM:
                    // 1-1 < 1.0.x
                    return -1;
                case STRING_ITEM:
                    // 1-1 > 1-sp
                    return 1;
                case LIST_ITEM:
                    Iterator<Item> left = iterator();
                    Iterator<Item> right = ((ListItem) item).iterator();
                    while (left.hasNext() || right.hasNext()) {
                        Item l = left.hasNext() ? left.next() : null;
                        Item r = right.hasNext() ? right.next() : null;
                        int result = l == null ? (r == null ? 0 : -1 * r.compareTo(l)) : l.compareTo(r);
                        if (result != 0) {
                            return result > 0 ? 1 : -1;
                        }
                    }
                    return 0;

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            for (Item item : this) {
                if (buffer.length() > 0) {
                    buffer.append((item instanceof ListItem) ? '-' : '.');
                }
                buffer.append(item);
            }
            return buffer.toString();
        }
    }

    public VersionComparatorUtil(String version) {
        parseVersion(version);
    }

    public final void parseVersion(String version) {
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

        this.value = version;

        items = new ListItem();

        version = version.toLowerCase(Locale.ENGLISH);

        ListItem list = items;

        Deque<Item> stack = new ArrayDeque<>();
        stack.push(list);

        boolean isDigit = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(IntItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                list.add(list = new ListItem());
                stack.push(list);
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    list.add(new StringItem(version.substring(startIndex, i), true));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, version.substring(startIndex, i)));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            list.add(parseItem(isDigit, version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }
    }

    private static Item parseItem(boolean isDigit, String buf) {
        if (isDigit) {
            return new IntItem(buf);
        }
        return new StringItem(buf, false);
    }

    @Override
    public int compareTo(VersionComparatorUtil o) {
        return items.compareTo(o.items);
    }
}
