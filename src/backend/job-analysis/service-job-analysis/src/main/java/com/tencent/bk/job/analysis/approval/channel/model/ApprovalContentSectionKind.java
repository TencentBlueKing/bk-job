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

package com.tencent.bk.job.analysis.approval.channel.model;

/**
 * 审批内容渲染产出的章节类型。身份在渲染时声明，简要裁剪按类型整段丢弃，不从 Markdown 反解析。
 * <p>
 * {@link #discardOrder} 越大越先被整章丢弃；0 表示不可整章丢弃（一级标题、操作概要）。
 */
public enum ApprovalContentSectionKind {

    TITLE(0),
    SUMMARY(0),
    MULTI_LINE(1),
    GLOBAL_VARS(2),
    SCRIPT(3),
    RAW_PARAMS(4);

    private final int discardOrder;

    ApprovalContentSectionKind(int discardOrder) {
        this.discardOrder = discardOrder;
    }

    public int getDiscardOrder() {
        return discardOrder;
    }

    public boolean isDroppable() {
        return discardOrder > 0;
    }
}
