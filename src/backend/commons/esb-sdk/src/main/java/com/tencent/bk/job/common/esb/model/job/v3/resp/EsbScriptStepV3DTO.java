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

package com.tencent.bk.job.common.esb.model.job.v3.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import lombok.Data;

/**
 * @since 17/11/2020 20:37
 */
@Data
public class EsbScriptStepV3DTO {
    @JsonProperty("script_type")
    @JsonPropertyDescription("Script type")
    private Integer type;

    @JsonProperty("script_id")
    @JsonPropertyDescription("Script id")
    private String scriptId;

    @JsonProperty("script_version_id")
    @JsonPropertyDescription("Script version id")
    private Long scriptVersionId;

    @JsonProperty("script_content")
    @JsonPropertyDescription("Script content")
    private String content;

    @JsonProperty("script_language")
    @JsonPropertyDescription("Script language")
    private Integer language;

    @JsonProperty("script_param")
    @JsonPropertyDescription("Script params")
    private String scriptParam;

    @JsonProperty("windows_interpreter")
    @JsonPropertyDescription("Windows interpreter")
    private String windowsInterpreter;

    @JsonProperty("script_timeout")
    @JsonPropertyDescription("Timeout")
    private Long scriptTimeout;

    @JsonPropertyDescription("Account")
    private EsbAccountV3BasicDTO account;

    @JsonPropertyDescription("Hosts")
    private EsbServerV3DTO server;

    @JsonProperty("is_param_sensitive")
    @JsonPropertyDescription("Is script params sensitive")
    private Integer secureParam;

    @JsonProperty("is_ignore_error")
    @JsonPropertyDescription("Is ignore error")
    private Integer ignoreError;
}
