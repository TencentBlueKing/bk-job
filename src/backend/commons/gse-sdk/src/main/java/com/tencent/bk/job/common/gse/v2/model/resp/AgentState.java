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

package com.tencent.bk.job.common.gse.v2.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GSE - Agent状态
 */
@Data
public class AgentState {
    /**
     * 目标Agent ，数据格式分为两种。1. cloudId:ip（兼容老版本Agent没有agentId的情况) 2. agentId
     */
    @JsonProperty("bk_agent_id")
    private String agentId;

    /**
     * 云区域ID
     */
    @JsonProperty("bk_cloud_id")
    private Integer cloudId;

    /**
     * Agent版本
     */
    private String version;

    /**
     * Agent运行模式
     */
    @JsonProperty("run_mode")
    private Integer runMode;

    /**
     * Agent状态
     */
    @JsonProperty("status_code")
    private Integer statusCode;
}
