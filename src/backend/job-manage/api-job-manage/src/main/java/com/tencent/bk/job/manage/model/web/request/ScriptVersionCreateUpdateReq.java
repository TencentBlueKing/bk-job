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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.Update;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * 脚本版本新增、更新请求
 */
@Data
@ApiModel("脚本版本新增、更新请求报文")
public class ScriptVersionCreateUpdateReq {

    /**
     * 脚本内容
     */
    @ApiModelProperty(value = "脚本内容,新增脚本/脚本版本时需要传入，BASE64编码")
    @NotEmpty(message = "{validation.constraints.ScriptContent_empty.message}", groups = {Create.class, Update.class})
    private String content;

    /**
     * 脚本的版本号
     */
    @ApiModelProperty(value = "版本号，新增脚本版本时需要传入")
    @NotEmpty(message = "{validation.constraints.ScriptVersion_empty.message}", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptVersion_outOfLength.message}", groups = Create.class)
    @Pattern(regexp = "^[A-Za-z0-9_\\-#@.]+$", message = "{validation.constraints.ScriptVersion_illegal.message}",
        groups = Create.class)
    private String version;

    /**
     * 脚本版本描述
     */
    @ApiModelProperty(value = "脚本版本描述")
    private String versionDesc;

}


