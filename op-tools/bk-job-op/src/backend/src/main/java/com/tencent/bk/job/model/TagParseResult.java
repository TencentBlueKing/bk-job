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

package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个Tag的解析结果
 */
@Data
@AllArgsConstructor
public class TagParseResult {

    /**
     * 解析状态
     */
    private TagParseStatusEnum status;

    /**
     * 调用方传入的原始Tag，非法时原样返回给调用方便于定位
     */
    private String rawTag;

    /**
     * 解析出的结构化Tag，仅status为VALID时非空
     */
    private RepoTagDTO tag;

    /**
     * 非VALID时的原因说明，用于操作日志
     */
    private String reason;

    public static TagParseResult valid(String rawTag, RepoTagDTO tag) {
        return new TagParseResult(TagParseStatusEnum.VALID, rawTag, tag, null);
    }

    public static TagParseResult ignored(String rawTag, String reason) {
        return new TagParseResult(TagParseStatusEnum.IGNORED, rawTag, null, reason);
    }

    public static TagParseResult invalid(String rawTag, String reason) {
        return new TagParseResult(TagParseStatusEnum.INVALID, rawTag, null, reason);
    }
}
