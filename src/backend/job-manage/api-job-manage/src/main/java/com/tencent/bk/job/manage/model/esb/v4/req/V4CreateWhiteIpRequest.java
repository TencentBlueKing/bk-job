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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.manage.model.esb.v4.req.validation.CheckV4WhiteIpScope;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建主机白名单。不继承单业务范围请求，一条记录可以覆盖全部业务或多个业务。
 */
@Getter
@Setter
@CheckV4WhiteIpScope
public class V4CreateWhiteIpRequest extends EsbReq {

    @JsonProperty("all_scope")
    private boolean allScope = false;

    @JsonProperty("scope_list")
    @Valid
    private List<V4WhiteIpScopeReq> scopeList;

    @JsonProperty("host_list")
    @NotEmpty(message = "{validation.constraints.NotBlankField.message}")
    @Valid
    private List<V4WhiteIpHostReq> hostList;

    @JsonProperty("remark")
    @NotBlank(message = "{validation.constraints.NotBlankField.message}")
    private String remark;

    @JsonProperty("action_scope_list")
    @NotEmpty(message = "{validation.constraints.NotBlankField.message}")
    private List<String> actionScopeList;
}
