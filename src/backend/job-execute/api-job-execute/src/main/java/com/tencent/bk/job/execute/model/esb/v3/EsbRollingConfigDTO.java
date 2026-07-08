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

package com.tencent.bk.job.execute.model.esb.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.RollingExecutionModeEnum;
import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.constant.RollingTypeEnum;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.validation.EsbRollingConfigGroupSequenceProvider;
import lombok.Data;
import org.hibernate.validator.group.GroupSequenceProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 滚动执行配置
 */
@Data
@GroupSequenceProvider(EsbRollingConfigGroupSequenceProvider.class)
public class EsbRollingConfigDTO {

    /**
     * 滚动对象，1-传输目标；2-源文件，默认为传输目标
     *
     * @see com.tencent.bk.job.common.constant.RollingTypeEnum
     */
    @CheckEnum(enumClass = RollingTypeEnum.class, message = "{validation.constraints.RollingType_illegal.message}")
    private Integer type = RollingTypeEnum.TARGET_EXECUTE_OBJECT.getValue();

    /**
     * 滚动机制，1-执行失败则暂停；2-忽略失败，自动滚动下一批；3-人工确认，默认为执行失败则暂停
     *
     * @see com.tencent.bk.job.common.constant.RollingModeEnum
     */
    @CheckEnum(enumClass = RollingModeEnum.class, message = "{validation.constraints.RollingMode_illegal.message}")
    private Integer mode = RollingModeEnum.PAUSE_IF_FAIL.getValue();

    /**
     * 滚动批次执行模式，1-串行(默认)；2-并行(错峰)
     *
     * @see com.tencent.bk.job.common.constant.RollingExecutionModeEnum
     */
    @CheckEnum(enumClass = RollingExecutionModeEnum.class,
        message = "{validation.constraints.RollingExecutionMode_illegal.message}")
    @JsonProperty("execution_mode")
    private Integer executionMode = RollingExecutionModeEnum.SERIAL.getValue();

    /**
     * 批次间固定间隔（累积式相邻批），单位毫秒，仅并行模式使用且必填
     */
    @NotNull(
        groups = ValidationGroups.RollingExecutionMode.Parallel.class,
        message = "{validation.constraints.RollingBatchStartWaitFixedMs_NotNull.message}"
    )
    @Min(
        value = 0,
        groups = ValidationGroups.RollingExecutionMode.Parallel.class,
        message = "{validation.constraints.RollingBatchStartWaitMs_Min.message}"
    )
    @JsonProperty("batch_start_wait_fixed_ms")
    private Long batchStartWaitFixedMs;

    /**
     * 批次间随机延迟下限，单位毫秒，仅并行模式使用且必填
     */
    @NotNull(
        groups = ValidationGroups.RollingExecutionMode.Parallel.class,
        message = "{validation.constraints.RollingBatchStartWaitRandomMinMs_NotNull.message}"
    )
    @Min(
        value = 0,
        groups = ValidationGroups.RollingExecutionMode.Parallel.class,
        message = "{validation.constraints.RollingBatchStartWaitMs_Min.message}"
    )
    @JsonProperty("batch_start_wait_random_min_ms")
    private Long batchStartWaitRandomMinMs;

    /**
     * 批次间随机延迟上限，单位毫秒，仅并行模式使用且必填，需 ≥ batch_start_wait_random_min_ms
     */
    @NotNull(
        groups = ValidationGroups.RollingExecutionMode.Parallel.class,
        message = "{validation.constraints.RollingBatchStartWaitRandomMaxMs_NotNull.message}"
    )
    @Min(
        value = 0,
        groups = ValidationGroups.RollingExecutionMode.Parallel.class,
        message = "{validation.constraints.RollingBatchStartWaitMs_Min.message}"
    )
    @JsonProperty("batch_start_wait_random_max_ms")
    private Long batchStartWaitRandomMaxMs;

    /**
     * 滚动对象为【传输目标】的配置项
     * 目标执行对象滚动分批策略表达式，当前不支持与源文件滚动同时配置
     */
    @NotBlank(
        groups = ValidationGroups.RollingType.TargetExecuteObject.class,
        message = "{validation.constraints.RollingExpression_NotBlank.message}"
    )
    private String expression;

    /**
     * 滚动对象为【源文件】的配置项
     * 源文件滚动配置，当前不支持与目标执行对象同时配置
     */
    @NotNull(
        groups = ValidationGroups.RollingType.FileSource.class,
        message = "{validation.constraints.RollingFileSource_NotNull.message}"
    )
    @JsonProperty("file_source")
    @Valid
    private EsbFileSourceRollingConfigDTO fileSource;
}
