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

package com.tencent.bk.job.crontab.validation.provider;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.Update;
import com.tencent.bk.job.crontab.model.esb.v3.request.EsbSaveCronV3Request;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * EsbSaveCronV3Request 参数联合校验
 */
public class EsbSaveCronV3RequestSequenceProvider implements DefaultGroupSequenceProvider<EsbSaveCronV3Request> {

    @Override
    public List<Class<?>> getValidationGroups(EsbSaveCronV3Request bean) {
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(EsbSaveCronV3Request.class);
        if (bean != null) { // 这块判空请务必要做
            Long id = bean.getId();
            if (id == null || id == -1) {
                if (StringUtils.isBlank(bean.getCronExpression())
                    && (bean.getExecuteTime() == null || bean.getExecuteTime() <= 0)) {
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{
                        "expression/execute_time", "expression/execute_time cannot both be null or invalid"});
                }
                defaultGroupSequence.add(Create.class);
            } else {
                boolean hasChange = false;
                Long planId = bean.getPlanId();
                if (planId != null && planId > 0) {
                    hasChange = true;
                }
                String name = bean.getName();
                if (StringUtils.isNotBlank(name)) {
                    hasChange = true;
                }
                String cronExpression = bean.getCronExpression();
                if (StringUtils.isNotBlank(cronExpression)) {
                    hasChange = true;
                }
                Long executeTime = bean.getExecuteTime();
                if (executeTime != null && executeTime > 0) {
                    hasChange = true;
                }
                if (!hasChange) {
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{
                        "job_plan_id/name/expression/execute_time",
                        "At least one of job_plan_id/name/expression/execute_time must be given to update cron "
                            + id});
                }
                defaultGroupSequence.add(Update.class);
            }
        }
        return defaultGroupSequence;
    }
}
