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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 创建脚本请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("创建业务脚本请求报文")
public class EsbCreateScriptV3Req extends EsbAppScopeReq {

    /**
     * 脚本名称
     */
    @NotEmpty(message = "{validation.constraints.ScriptName_empty.message}", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptName_outOfLength.message}", groups = Create.class)
    @Pattern(regexp = "^[^\\\\|/:*<>\"?]+$", message = "{validation.constraints.ScriptName_illegal.message}",
        groups = Create.class)
    private String name;
    /**
     * 脚本描述
     */
    private String description;

    /**
     * 脚本类型
     * @see com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum
     */
    @JsonProperty("script_language")
    @NotNull(message = "{validation.constraints.ScriptType_empty.message}",groups = Create.class)
    @CheckEnum(enumClass = ScriptTypeEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.ScriptType_illegal.message}", groups = Create.class)
    private Integer type;

    /**
     * 脚本内容，需Base64编码
     */
    @NotEmpty(message = "{validation.constraints.ScriptContent_empty.message}", groups = Create.class)
    private String content;

    /**
     * 脚本版本
     */
    @NotEmpty(message = "{validation.constraints.ScriptVersion_empty.message}", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptVersion_outOfLength.message}", groups = Create.class)
    @Pattern(regexp = "^[A-Za-z0-9_\\-#@.]+$", message = "{validation.constraints.ScriptVersion_illegal.message}",
        groups = Create.class)
    private String version;

    /**
     * 版本描述
     */
    @JsonProperty("version_desc")
    private String versionDesc;
}
