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

package com.tencent.bk.job.common.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 分页查询条件
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PageCondition implements Cloneable {
    /**
     * 分页起始
     */
    private int start = 0;
    /**
     * 分页大小
     */
    private int length = 20;

    /**
     * 分页-是否计算总数；默认计算。分页计算总数可能会影响 API 性能, 必要场景才可使用
     */
    private boolean countPageTotal = true;

    public static PageCondition build(int start, int length) {
        PageCondition pageCondition = new PageCondition();
        pageCondition.setStart(start);
        pageCondition.setLength(length);
        pageCondition.setCountPageTotal(true);
        return pageCondition;
    }

    public static PageCondition build(int start, int length, boolean countPageTotal) {
        PageCondition pageCondition = new PageCondition();
        pageCondition.setStart(start);
        pageCondition.setLength(length);
        pageCondition.setCountPageTotal(countPageTotal);
        return pageCondition;
    }

    public PageCondition clone() {
        PageCondition pageCondition = new PageCondition();
        pageCondition.setStart(start);
        pageCondition.setLength(length);
        pageCondition.setCountPageTotal(countPageTotal);
        return pageCondition;
    }

}
