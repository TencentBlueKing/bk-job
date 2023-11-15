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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.validation.CheckEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询账号列表请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EsbGetAccountListV3Req extends EsbAppScopeReq {
    /**
     * 账号用途（1：系统账号，2：DB账号），不传则不区分
     */
    @CheckEnum(enumClass = AccountCategoryEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.AccountCategory_illegal.message}")
    private Integer category;

    /**
     * 账号名称
     */
    private String account;

    /**
     * 账号别名
     */
    private String alias;

    /**
     * 起始位置
     */
    private Integer start;

    /**
     * 起始位置
     */
    private Integer length;
}
