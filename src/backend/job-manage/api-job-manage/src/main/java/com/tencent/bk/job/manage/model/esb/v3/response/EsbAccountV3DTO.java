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

package com.tencent.bk.job.manage.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EsbAccountV3DTO extends EsbAppScopeDTO {
    @JsonPropertyDescription("Account ID")
    private Long id;

    @JsonPropertyDescription("Account name")
    private String account;

    @JsonPropertyDescription("Account alias")
    private String alias;

    // 账号用途（1：系统账号，2：DB账号）
    @JsonPropertyDescription("Account usage. 1- OS Account, 2 - DB Account")
    private int category;

    // 账号类型（1：Linux，2：Windows，9：MySQL，10：Oracle，11：DB2）
    @JsonPropertyDescription("Account type. 1 - Linux, 2 - Windows, 9 - MySQL, 10 - Oracle, 11 - DB2")
    private int type;

    @JsonProperty("db_system_account_id")
    @JsonPropertyDescription("System account id for db account")
    private Long dbSystemAccountId;

    @JsonPropertyDescription("Account OS")
    private String os;

    @JsonPropertyDescription("Creator")
    private String creator;

    @JsonProperty("create_time")
    @JsonPropertyDescription("Create time")
    private Long createTime;

    @JsonProperty("last_modify_user")
    @JsonPropertyDescription("Last modify user")
    private String lastModifyUser;

    @JsonPropertyDescription("Last modify time")
    @JsonProperty("last_modify_time")
    private Long lastModifyTime;

    /**
     * 账号描述
     */
    @JsonPropertyDescription("Account description")
    @JsonProperty("description")
    private String description;
}
