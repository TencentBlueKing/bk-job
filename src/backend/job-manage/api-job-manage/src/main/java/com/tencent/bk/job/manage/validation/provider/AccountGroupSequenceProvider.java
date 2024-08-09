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

package com.tencent.bk.job.manage.validation.provider;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 是db类型账号，需要校验db端口和关联的系统账号
 */
public class AccountGroupSequenceProvider implements DefaultGroupSequenceProvider<AccountCreateUpdateReq> {

    @Override
    public List<Class<?>> getValidationGroups(AccountCreateUpdateReq bean){
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(AccountCreateUpdateReq.class);

        if (bean != null) {
            if (bean.getCategory() != null && bean.getCategory().equals(AccountCategoryEnum.DB.getValue())){
                defaultGroupSequence.add(AccountCreateUpdateReq.dbAccountGroup.class);
            }
        }
        return defaultGroupSequence;
    }
}
