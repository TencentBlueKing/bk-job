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

package com.tencent.bk.job.analysis.approval.crypto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 脱敏后的参数快照，供审批内容展示。
 */
@Getter
public class ApprovalDisplayParams {

    /**
     * 脱敏后的请求对象，序列化后作为审批内容里的「原始参数」区
     */
    private final Object params;

    /**
     * 从参数里摘出来、需要单独成块原样展示的明文段（目前只有脚本内容）
     */
    private final List<PlainTextBlock> plainTextBlocks;

    public ApprovalDisplayParams(Object params, List<PlainTextBlock> plainTextBlocks) {
        this.params = params;
        this.plainTextBlocks = plainTextBlocks == null ? Collections.emptyList() : plainTextBlocks;
    }

    public static ApprovalDisplayParams of(Object params) {
        return new ApprovalDisplayParams(params, new ArrayList<>());
    }

    /**
     * 一段原样展示的明文
     */
    @Getter
    public static class PlainTextBlock {

        /**
         * 该段明文在参数里的字段名，用于在审批内容里标明它是哪个字段
         */
        private final String field;

        private final String value;

        public PlainTextBlock(String field, String value) {
            this.field = field;
            this.value = value;
        }
    }
}
