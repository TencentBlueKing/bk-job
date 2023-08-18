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

package com.tencent.bk.job.manage.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 创建公共脚本返回信息
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbCreatePublicScriptV3DTO {

    /**
     * 脚本版本ID
     */
    @JsonProperty("script_version_id")
    private long scriptVersionId;

    /**
     * 脚本ID
     */
    private String id;

    /**
     * 脚本名称
     */
    private String name;

    /**
     * 脚本语言:1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell
     */
    @JsonProperty("script_language")
    private int type;

    /**
     * 是否公共脚本
     */
    @JsonProperty("public_script")
    private boolean publicScript;

    /**
     * 脚本版本内容
     */
    private String content;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间Unix时间戳（ms）
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * 最近一次修改人
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;

    /**
     * 最近一次修改时间Unix时间戳（ms）
     */
    @JsonProperty("last_modify_time")
    private Long lastModifyTime;

    /**
     * 脚本版本
     */
    private String version;

    /**
     * 版本描述
     */
    @JsonProperty("version_desc")
    private String versionDesc;

    /**
     * 脚本版本状态（0：未上线，1：已上线，2：已下线，3：已禁用）
     */
    private int status;

    /**
     * 脚本描述
     */
    private String description;

}
