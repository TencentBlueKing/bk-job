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

import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 脚本新增请求
 */
@Data
@ApiModel("脚本新增请求")
public class ScriptCreateReq {

    /**
     * 脚本名称
     */
    @ApiModelProperty(value = "脚本名称", required = true, example = "scriptName")
    @NotEmpty(message = "{validation.constraints.ScriptName_empty.message}")
    @Length(max = 60, message = "{validation.constraints.ScriptName_outOfLength.message}")
    @Pattern(regexp = "^[^\\\\|/:*<>\"?]+$", message = "{validation.constraints.ScriptName_illegal.message}")
    private String name;

    /**
     * 脚本类型
     */
    @ApiModelProperty(value = "脚本类型,创建脚本时需要传入")
    @NotNull(message = "{validation.constraints.ScriptType_empty.message}")
    @CheckEnum(enumClass = ScriptTypeEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.ScriptType_illegal.message}")
    private Integer type;

    /**
     * 脚本内容
     */
    @ApiModelProperty(value = "脚本内容,创建脚本时需要传入，BASE64编码")
    @NotEmpty(message = "{validation.constraints.ScriptContent_empty.message}")
    private String content;

    /**
     * 脚本的版本号
     */
    @ApiModelProperty(value = "版本号，新增脚本时需要传入")
    @NotEmpty(message = "{validation.constraints.ScriptVersion_empty.message}")
    @Length(max = 60, message = "{validation.constraints.ScriptVersion_outOfLength.message}")
    @Pattern(regexp = "^[A-Za-z0-9_\\-#@.]+$", message = "{validation.constraints.ScriptVersion_illegal.message}")
    private String version;

    /**
     * 脚本标签
     */
    @ApiModelProperty(value = "脚本标签，新增/更新脚本需要传入")
    private List<TagVO> tags;

    /**
     * 脚本描述
     */
    @ApiModelProperty(value = "脚本描述")
    private String description;

}


