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

package com.tencent.bk.job.backup.constant;

import lombok.Getter;

/**
 * DB 数据节点类型
 */
@Getter
public enum DbDataNodeTypeEnum {
    /**
     * 单MySql节点
     */
    STANDALONE(0),
    /**
     * MySql分库分表节点
     */
    SHARDING(1),

    /**
     * 单MongoDB节点
     */
    SINGLE_MONGODB(2);

    private final int value;

    DbDataNodeTypeEnum(int value) {
        this.value = value;
    }

    public static DbDataNodeTypeEnum valOf(int type) {
        for (DbDataNodeTypeEnum dataNodeType : values()) {
            if (dataNodeType.getValue() == type) {
                return dataNodeType;
            }
        }
        throw new IllegalArgumentException("No DbDataNodeTypeEnum constant: " + type);
    }
}
