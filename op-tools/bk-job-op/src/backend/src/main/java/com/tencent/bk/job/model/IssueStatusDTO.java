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
import lombok.NoArgsConstructor;

/**
 * Issue状态存储实体，对应 issue_status 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueStatusDTO {

    /**
     * Issue标识，如 issue_4523
     */
    private String id;

    /**
     * 从id中解析出的Issue编号。
     *
     * <p>列表按Issue编号降序展示，而id是字符串，直接按字符串排序会让 issue_999 排在 issue_4523 前面。
     * 这里在写入时把编号解析成数字单独存一列，让排序交给SQL完成；id不符合 issue_数字 格式时为null。</p>
     */
    private Long issueNo;

    /**
     * Issue状态
     */
    private String status;

    /**
     * Issue状态描述
     */
    private String statusDescription;

    /**
     * 状态最后更新时间（毫秒时间戳）
     */
    private Long updateTime;
}
