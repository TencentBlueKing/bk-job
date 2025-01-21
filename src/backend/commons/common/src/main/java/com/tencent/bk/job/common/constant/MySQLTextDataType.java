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

package com.tencent.bk.job.common.constant;

import com.tencent.bk.job.common.util.file.FileSizeUtil;
import lombok.Getter;

/**
 * 这里暂时只记录TEXT形式的MySQL类型，用于获取各类型的最大长度。
 * 因为在MySQL中，TEXT类型的存储是以字节流的长度为限制的，而char/varchar则是以字符串的长度为限制的，所以校验长度合法的逻辑是不太一样的。
 *
 */
@Getter
public enum MySQLTextDataType {
    TINYTEXT("TINYTEXT", 255L),
    TEXT("TEXT", 65535L),
    MEDIUMTEXT("MEDIUMTEXT", 16777215L),
    LONGTEXT("LONGTEXT", 4294967295L);

    private final String value;
    private final Long maximumLength;   // MySQL能存的最大字节数

    MySQLTextDataType(String value, Long maximumLength) {
        this.value = value;
        this.maximumLength = maximumLength;
    }

    public static MySQLTextDataType valOf(String value) {
        for (MySQLTextDataType type : MySQLTextDataType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value + "(" + FileSizeUtil.getFileSizeStr(maximumLength) + ")";
    }
}
