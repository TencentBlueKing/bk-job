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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.Update;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AddOrUpdateDangerousRuleReq;

/**
 * id属性值如果不为空并且不为-1，执行更新组的校验逻辑，否则执行新增组的校验逻辑
 */
public class DangerousRuleGroupSequenceProvider implements DefaultGroupSequenceProvider<AddOrUpdateDangerousRuleReq> {

    @Override
    public List<Class<?>> getValidationGroups(AddOrUpdateDangerousRuleReq bean){
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(AddOrUpdateDangerousRuleReq.class); 

        if (bean != null) { // 这块判空请务必要做
            try {
                Long id = bean.getId();
                if (id == null || id == -1) {
                    defaultGroupSequence.add(Create.class);
                } else{
                    defaultGroupSequence.add(Update.class);
                }
            } catch (Exception e) {
            }
        }
        return defaultGroupSequence;
    }
}