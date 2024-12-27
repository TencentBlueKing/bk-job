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

package com.tencent.bk.job.common.mysql.dynamic.id;

import lombok.Getter;

@Getter
public enum IdGenType {
    AUTO_INCREMENT(IdGenType.Constants.AUTO_INCREMENT),
    SEGMENT(IdGenType.Constants.LEAF_SEGMENT);

    public static class Constants {
        public final static String AUTO_INCREMENT = "auto_increment";
        public final static String LEAF_SEGMENT = "leaf_segment";
    }

    private final String type;

    IdGenType(String type) {
        this.type = type;
    }

    public static IdGenType valOf(String type) {
        if (type == null) {
            return null;
        }
        for (IdGenType value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No IdGenType constant: " + type);
    }

    public static boolean checkValid(String type) {
        if (type == null) {
            return false;
        }
        for (IdGenType value : values()) {
            if (value.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
