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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import com.tencent.bk.job.common.validation.Update;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 更新公共脚本版本请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("更新公共脚本版本请求报文")
public class EsbUpdatePublicScriptVersionV3Req extends EsbJobReq {

    /**
     * 脚本ID
     */
    @NotEmpty(message = "{validation.constraints.ScriptId_empty.message}", groups = Update.class)
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 脚本版本ID
     */
    @NotNull(message = "{validation.constraints.ScriptVersion_empty.message}", groups = Update.class)
    @JsonProperty("script_version_id")
    private Long scriptVersionId;

    /**
     * 脚本内容，需Base64编码
     */
    @NotEmpty(message = "{validation.constraints.ScriptContent_empty.message}", groups = Update.class)
    private String content;

    /**
     * 版本描述
     */
    @JsonProperty("version_desc")
    private String versionDesc;

}
