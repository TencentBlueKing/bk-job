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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.DecimalFormatJsonSerializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "执行对象任务执行信息")
@Data
public class ExecuteObjectTaskVO {
    @Schema(description = "执行次数")
    private Integer executeCount;

    @Schema(description = "滚动批次")
    private Integer batch;

    @Schema(description = "执行对象类型")
    private ExecuteObjectVO executeObject;

    @Schema(description = "执行对象任务执行状态")
    private Integer status;

    @Schema(description = "执行对象任务执行状态描述")
    private String statusDesc;

    @Schema(description = "开始时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;

    @Schema(description = "结束时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;

    @Schema(description = "耗时")
    @JsonSerialize(using = DecimalFormatJsonSerializer.class)
    private Long totalTime;

    @Schema(description = "脚本返回码")
    private Integer exitCode;

    @Schema(description = "脚本错误码")
    private Integer errorCode;

    @Schema(description = "脚本执行输出")
    private String tag;

}
