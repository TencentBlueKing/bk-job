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

package com.tencent.bk.job.manage.model.web.request.dynamicfilter;

import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 动态条件过滤器命中预览请求：根据用户当前编辑中的过滤器，分页返回 cmdb 命中的容器。
 * <p>
 * 仅做查询/试算，不做任何持久化；后端 namespace 字段非必填。
 */
@Data
@Schema(description = "动态条件过滤器命中预览请求")
public class PreviewDynamicContainerReq {

    @Schema(description = "动态条件过滤器（同保存表单）")
    @NotNull(message = "{validation.constraints.PreviewDynamicContainer_filterEmpty.message}")
    @Valid
    private WebContainerConditionFilter filter;

    @Schema(description = "数据起始位置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 0, message = "{validation.constraints.PreviewDynamicContainer_startInvalid.message}")
    private Integer start = 0;

    @Schema(description = "拉取数量；最大不超过100", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "{validation.constraints.PreviewDynamicContainer_pageSizeInvalid.message}")
    @Max(value = 100, message = "{validation.constraints.PreviewDynamicContainer_pageSizeInvalid.message}")
    private Integer pageSize = 10;
}
