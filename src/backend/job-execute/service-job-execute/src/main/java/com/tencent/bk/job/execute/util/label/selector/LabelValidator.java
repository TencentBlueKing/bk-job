/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LabelValidator {

    private static final String dns1123LabelFmt = "[a-z0-9]([-a-z0-9]*[a-z0-9])?";
    private static final String dns1123SubdomainFmt = dns1123LabelFmt + "(\\." + dns1123LabelFmt + ")*";
    private static final String dns1123SubdomainErrorMsg = "a lowercase RFC 1123 subdomain must consist of lower case" +
        " alphanumeric characters, '-' or '.', and must start and end with an alphanumeric character";
    private static final int DNS1123SubdomainMaxLength = 253;
    private static final Pattern dns1123SubdomainRegexp = Pattern.compile("^" + dns1123SubdomainFmt + "$");
    private static final int qualifiedNameMaxLength = 63;
    private static final String qnameCharFmt = "[a-zA-Z0-9]";
    private static final String qnameExtCharFmt = "[-A-Za-z0-9_.]";
    private static final String qualifiedNameFmt = "(" + qnameCharFmt + qnameExtCharFmt + "*)?" + qnameCharFmt;
    private static final String qualifiedNameErrMsg = "must consist of alphanumeric characters, '-', '_' or '.', and " +
        "must start and end with an alphanumeric character";
    private static final Pattern qualifiedNameRegexp = Pattern.compile("^" + qualifiedNameFmt + "$");


    private static final int LabelValueMaxLength = 63;
    private static final String labelValueFmt = "(" + qualifiedNameFmt + ")?";
    private static final Pattern labelValueRegexp = Pattern.compile("^" + labelValueFmt + "$");
    private static final String labelValueErrMsg = "a valid label must be an empty string or consist of alphanumeric " +
        "characters, '-', '_' or '.', and must start and end with an alphanumeric character";

    /**
     * 校验 label key,并返回校验错误信息
     *
     * @param labelKey label  key
     * @return 校验错误信息
     */
    public static List<String> validateLabelKey(String labelKey) {
        return isQualifiedName(labelKey);
    }

    /**
     * IsQualifiedName tests whether the value passed is what Kubernetes calls a
     * "qualified name".  This is a format used in various places throughout the
     * system.  If the value is not valid, a list of error strings is returned.
     * Otherwise an empty list (or nil) is returned.
     */
    private static List<String> isQualifiedName(String value) {
        List<String> errs = new ArrayList<>();
        String[] parts = value.split("/");
        String name;
        switch (parts.length) {
            case 1:
                name = parts[0];
                break;
            case 2:
                String prefix = parts[0];
                name = parts[1];
                if (prefix.length() == 0) {
                    errs.add("prefix part must be " + emptyError());
                } else {
                    List<String> msgs = isDNS1123Subdomain(prefix);
                    if (msgs.size() != 0) {
                        errs.addAll(prefixEach(msgs, "prefix part "));
                    }
                }
                break;
            default:
                errs.add("a qualified name " + regexError(qualifiedNameErrMsg, qualifiedNameFmt, "MyName", "my.name",
                    "123-abc") +
                    " with an optional DNS subdomain prefix and '/' (e.g. 'example.com/MyName')");
                return errs;
        }
        if (name.length() == 0) {
            errs.add("name part " + emptyError());
        } else if (name.length() > qualifiedNameMaxLength) {
            errs.add("name part " + maxLenError(qualifiedNameMaxLength));
        }
        if (!qualifiedNameRegexp.matcher(name).matches()) {
            errs.add("name part " + regexError(qualifiedNameErrMsg, qualifiedNameFmt, "MyName", "my.name", "123-abc"));
        }
        return errs;
    }


    private static List<String> isDNS1123Subdomain(String value) {
        List<String> errs = new ArrayList<>();
        if (value.length() > DNS1123SubdomainMaxLength) {
            errs.add(maxLenError(DNS1123SubdomainMaxLength));
        }
        if (!dns1123SubdomainRegexp.matcher(value).matches()) {
            errs.add(regexError(dns1123SubdomainErrorMsg, dns1123SubdomainFmt, "example.com"));
        }
        return errs;
    }

    private static String maxLenError(int length) {
        return String.format("must be no more than %d characters", length);
    }

    private static String regexError(String msg, String fmt, String... examples) {
        if (examples.length == 0) {
            return msg + " (regex used for validation is '" + fmt + "')";
        }
        msg += " (e.g. ";
        StringBuilder msgBuilder = new StringBuilder(msg);
        for (int i = 0; i < examples.length; i++) {
            if (i > 0) {
                msgBuilder.append(" or ");
            }
            msgBuilder.append("'").append(examples[i]).append("', ");
        }
        msg = msgBuilder.toString();
        msg += "regex used for validation is '" + fmt + "')";
        return msg;
    }

    private static String emptyError() {
        return "must be non-empty";
    }

    private static List<String> prefixEach(List<String> msgs, String prefix) {
        for (int i = 0; i < msgs.size(); i++) {
            msgs.set(i, prefix + msgs.get(i));
        }
        return msgs;
    }

    /**
     * 校验 label value,并返回校验错误信息
     *
     * @param value label value
     * @return 校验错误信息
     */
    public static List<String> validateLabelValue(String value) {
        List<String> errs = new ArrayList<>();

        if (value.length() > LabelValueMaxLength) {
            errs.add(maxLenError(LabelValueMaxLength));
        }

        if (!labelValueRegexp.matcher(value).matches()) {
            errs.add(regexError(labelValueErrMsg, labelValueFmt, "MyValue", "my_value", "12345"));
        }

        return errs;
    }
}
