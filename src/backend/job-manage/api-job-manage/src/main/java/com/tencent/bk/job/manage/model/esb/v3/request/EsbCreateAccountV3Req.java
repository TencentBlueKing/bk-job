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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.util.json.SkipLogFields;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
@ApiModel("账号创建请求")
@ToString(exclude = {"password"})
public class EsbCreateAccountV3Req extends EsbAppScopeReq {

    /**
     * 帐号名称
     */
    @NotEmpty(message = "{validation.constraints.AccountName_empty.message}")
    private String account;

    /**
     * 账号类型：1-Linux，2-Windows，9-Mysql，10-Oracle，11-DB2
     */
    @CheckEnum(enumClass = AccountTypeEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.AccountType_illegal.message}")
    private Integer type;

    /**
     * 账号用途：1-系统账号，2-数据库账号
     */
    @CheckEnum(enumClass = AccountCategoryEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.AccountCategory_illegal.message}")
    private Integer category;

    /**
     * 系统账号的密码(Windows)
     */
    @SkipLogFields
    @Length(max = 255, message = "{validation.constraints.AccountPassword_tooLong.message}")
    private String password;

    /**
     * 别名
     */
    @Length(max = 255, message = "{validation.constraints.AccountAlias_tooLong.message}")
    private String alias;

    /**
     * 描述
     */
    @JsonProperty("description")
    @Length(max = 1024, message = "{validation.constraints.AccountDescription_tooLong.message}")
    private String remark;
}
