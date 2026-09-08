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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * OpenAPI V4 作业模板文件分发步骤详情。
 */
@Getter
@Setter
public class V4JobTemplateFileStepReq {

    @JsonProperty("file_source_list")
    @NotEmpty(message = "{validation.constraints.InvalidTemplateFileSourceList_empty.message}")
    @Valid
    private List<V4JobTemplateFileSourceReq> fileSourceList;

    @JsonProperty("file_destination")
    @NotNull(message = "{validation.constraints.InvalidTemplateFileDestination_empty.message}")
    @Valid
    private V4JobTemplateFileDestinationReq fileDestination;

    @JsonProperty("timeout")
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateStepTimeout.message}")
    private Long timeout;

    /**
     * 传输模式。1 严谨、2 强制、3 保险(源IP目录)、4 保险(日期目录)，缺省为 2。
     */
    @JsonProperty("transfer_mode")
    @CheckEnum(enumClass = FileTransferModeEnum.class,
        message = "{validation.constraints.InvalidTemplateTransferMode.message}")
    private Integer transferMode;

    /**
     * 上传限速，单位 MB/s。不限速时不传。
     */
    @JsonProperty("source_speed_limit")
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateSpeedLimit.message}")
    private Long sourceSpeedLimit;

    /**
     * 下载限速，单位 MB/s。不限速时不传。
     */
    @JsonProperty("destination_speed_limit")
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateSpeedLimit.message}")
    private Long destinationSpeedLimit;

    /**
     * 失败是否忽略。1 是、0 否，缺省为 0。
     */
    @JsonProperty("is_ignore_error")
    private Integer isIgnoreError;
}
