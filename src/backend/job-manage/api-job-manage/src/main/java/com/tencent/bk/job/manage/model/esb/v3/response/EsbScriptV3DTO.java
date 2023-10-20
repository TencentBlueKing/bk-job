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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EsbScriptV3DTO extends EsbAppScopeDTO {

    @JsonPropertyDescription("Script id")
    private String id;

    @JsonPropertyDescription("Script name")
    private String name;

    @JsonProperty("script_language")
    @JsonPropertyDescription("Script language")
    private int type;

    @JsonPropertyDescription("Creator")
    private String creator;

    @JsonPropertyDescription("Create time")
    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("last_modify_user")
    @JsonPropertyDescription("Last modify user")
    private String lastModifyUser;

    @JsonProperty("last_modify_time")
    @JsonPropertyDescription("Last modify time")
    private Long lastModifyTime;

    @JsonProperty("online_script_version_id")
    @JsonPropertyDescription("Online script version id")
    private Long onlineScriptVersionId;

    @JsonProperty("description")
    @JsonPropertyDescription("Script description")
    private String description;
}
