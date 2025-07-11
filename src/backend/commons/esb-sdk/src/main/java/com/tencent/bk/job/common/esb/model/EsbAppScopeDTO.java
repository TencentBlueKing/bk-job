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

package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 资源范围-ESB DTO
 */
@Setter
@Getter
@ToString
public class EsbAppScopeDTO {
    /**
     * 兼容字段,表示cmdb 业务/业务集ID
     */
    @CompatibleImplementation(name = "bizId", type = CompatibleType.API,
        explain = "兼容字段,表示业务ID或者业务集ID", deprecatedVersion = "3.6.x")
    @JsonProperty("bk_biz_id")
    @JsonPropertyDescription("bk_biz_id")
    private Long bizId;

    /**
     * 资源范围类型
     */
    @JsonProperty("bk_scope_type")
    @JsonPropertyDescription("Resource scope type")
    private String scopeType;

    /**
     * 资源范围ID
     */
    @JsonProperty("bk_scope_id")
    @JsonPropertyDescription("Resource scope id")
    private String scopeId;
}
