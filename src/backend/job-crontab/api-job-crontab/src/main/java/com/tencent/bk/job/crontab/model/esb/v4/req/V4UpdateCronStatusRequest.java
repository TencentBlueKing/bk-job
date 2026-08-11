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

package com.tencent.bk.job.crontab.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * v4 启停定时任务请求。
 * <p>
 * 与 v3 的 EsbUpdateCronStatusV3Request 的协议差异：不再提供 bk_biz_id 兼容字段，
 * 业务范围只用 bk_scope_type + bk_scope_id。
 * <p>
 * 校验不放在请求体的 validate 方法里，而是由 V4UpdateCronStatusRequestConverter 统一执行，
 * 这样审批预检链路与直接执行链路使用同一份校验实现。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class V4UpdateCronStatusRequest extends EsbAppScopeReq {

    /**
     * 定时任务 ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 目标状态。1-启动，2-暂停
     */
    @JsonProperty("status")
    private Integer status;
}
