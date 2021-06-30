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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.manage.model.web.vo.TagVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 脚本新增、更新请求
 */

@Data
@ApiModel("脚本新增、更新请求报文")
public class ScriptCreateUpdateReq {
    /**
     * 脚本版本ID，对应某个版本的脚本的ID
     */
    @ApiModelProperty(value = "脚本版本ID,更新脚本版本时需要传入", required = false)
    private Long scriptVersionId;
    /**
     * 脚本ID，一个脚本包含多个版本的脚本
     */
    @ApiModelProperty(value = "脚本ID,新增/更新脚本版本时需要传入", required = false)
    private String id;
    /**
     * 脚本名称
     */
    @ApiModelProperty(value = "脚本名称", required = true, example = "scriptName")
    private String name;
    /**
     * 脚本类型
     */
    @ApiModelProperty(value = "脚本类型,新增脚本/脚本版本时需要传入", required = false)
    private Integer type;
    /**
     * 业务ID
     */
    @ApiModelProperty(value = "业务ID", required = true, example = "2")
    private Long appId;
    /**
     * 脚本内容
     */
    @ApiModelProperty(value = "脚本内容,新增脚本/脚本版本时需要传入，BASE64编码", required = false)
    private String content;

    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者，新增脚本/脚本版本时需要传入", required = false)
    private String creator;

    /**
     * 脚本的版本号
     */
    @ApiModelProperty(value = "版本号，新增脚本/脚本版本时需要传入", required = false)
    private String version;
    /**
     * 脚本标签
     */
    @ApiModelProperty(value = "脚本标签，新增/更新脚本需要传入", required = false)
    private List<TagVO> tags;

    /**
     * 脚本描述
     */
    @ApiModelProperty(value = "脚本描述", required = false)
    private String description;

    /**
     * 脚本版本描述
     */
    @ApiModelProperty(value = "脚本版本描述", required = false)
    private String versionDesc;


}


