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

package com.tencent.bk.job.common.cc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 业务DTO
 */
@Getter
@Setter
@ToString
public class BusinessInfoDTO {
    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long bizId;
    /**
     * 业务名称
     */
    @JsonProperty("bk_biz_name")
    private String bizName;
    /**
     * 业务运维
     */
    @JsonProperty("bk_biz_maintainer")
    private String maintainers;
    /**
     * 开发商账号
     */
    @JsonProperty("bk_supplier_account")
    private String supplierAccount;
    /**
     * 表示业务类型
     */
    @JsonProperty("default")
    private Integer defaultApp;
    /**
     * 业务所在时区
     */
    @JsonProperty("time_zone")
    private String timezone;
    /**
     * 业务语言
     */
    @JsonProperty("language")
    private String language;
}
