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
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpenApiV4WhiteIpRecordDTO {

    @JsonProperty("id")
    private Long id;

    /**
     * 创建结果取第一台主机的管控区域；列表结果取与页面列表相同的汇总值。
     */
    @JsonProperty("bk_cloud_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long cloudAreaId;

    @JsonProperty("host_list")
    private List<OpenApiV4WhiteIpHostDTO> hostList;

    @JsonProperty("action_scope_list")
    private List<String> actionScopeList;

    @JsonProperty("all_scope")
    private Boolean allScope;

    @JsonProperty("scope_list")
    private List<OpenApiV4WhiteIpScopeDTO> scopeList;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("last_modifier")
    private String lastModifier;

    @JsonProperty("last_modify_time")
    private Long lastModifyTime;
}
