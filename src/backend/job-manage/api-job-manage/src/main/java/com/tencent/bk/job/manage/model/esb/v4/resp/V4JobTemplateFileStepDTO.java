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

package com.tencent.bk.job.manage.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * OpenAPI V4 作业模板文件步骤信息。
 */
@Getter
@Setter
public class V4JobTemplateFileStepDTO {

    @JsonProperty("file_source_list")
    @JsonPropertyDescription("File source list")
    private List<V4JobTemplateFileSourceDTO> fileSourceList;

    @JsonProperty("file_destination")
    @JsonPropertyDescription("File destination")
    private V4JobTemplateFileDestinationDTO fileDestination;

    @JsonProperty("timeout")
    @JsonPropertyDescription("Timeout in seconds")
    private Long timeout;

    @JsonProperty("transfer_mode")
    @JsonPropertyDescription("Transfer mode")
    private Integer transferMode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("source_speed_limit")
    @JsonPropertyDescription("Upload speed limit")
    private Long sourceSpeedLimit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("destination_speed_limit")
    @JsonPropertyDescription("Download speed limit")
    private Long destinationSpeedLimit;

    @JsonProperty("is_ignore_error")
    @JsonPropertyDescription("Is ignore error")
    private Integer isIgnoreError;
}
