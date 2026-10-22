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
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 分页查询主机白名单。分页字段复用 {@link EsbJobReq}，调用方不得调用会静默改写页大小的 adjustPageParam。
 */
@Getter
@Setter
public class V4ListWhiteIpRequest extends EsbJobReq {

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("scope_list")
    @Valid
    private List<V4WhiteIpScopeReq> scopeList;

    @JsonProperty("action_scope_list")
    private List<String> actionScopeList;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("last_modifier")
    private String lastModifier;

    @JsonProperty("order_field")
    private String orderField;

    /**
     * 0 降序，1 升序。仅在 order_field 有值时生效。
     */
    @JsonProperty("order")
    private Integer order;
}
