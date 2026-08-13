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

package com.tencent.bk.job.analysis.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 发起接口的返回体。
 * <p>
 * <b>不返回任何操作结果</b>：发起阶段只做 dryRun 预检，作业没有真的执行，
 * 返回任何 taskInstanceId 之类的字段都是在骗调用方。
 */
@Data
public class V4ApprovalTaskCreatedDTO {

    @JsonProperty("approval_task_id")
    private String approvalTaskId;

    /**
     * 任务所属租户，便于调用方识别任务归属
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * 任务状态，新建时固定为 PENDING
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("approval_channel")
    private String approvalChannel;

    /**
     * 过期时刻，Unix 时间戳，单位毫秒
     */
    @JsonProperty("expire_at")
    private Long expireAt;
}
