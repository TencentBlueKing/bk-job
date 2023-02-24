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

package com.tencent.bk.job.execute.engine.evict;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.execute.model.inner.AppCodeTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.AppIdTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.ComposedTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.TaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.TaskInstanceIdEvictPolicyDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 通过POJO实例创建Policy的工厂类
 */
@Slf4j
public class PolicyFactory {

    /**
     * 根据策略数据实体，创建任务驱逐策略对象
     *
     * @param policy 策略数据实体
     * @return 策略对象
     */
    public static ITaskEvictPolicy createPolicyByDTO(TaskEvictPolicyDTO policy) {
        if (policy instanceof TaskInstanceIdEvictPolicyDTO) {
            return new TaskInstanceIdEvictPolicy(((TaskInstanceIdEvictPolicyDTO) policy).getTaskInstanceIdsToEvict());
        } else if (policy instanceof AppCodeTaskEvictPolicyDTO) {
            return new AppCodeTaskEvictPolicy(((AppCodeTaskEvictPolicyDTO) policy).getAppCodesToEvict());
        } else if (policy instanceof AppIdTaskEvictPolicyDTO) {
            return new AppIdTaskEvictPolicy(((AppIdTaskEvictPolicyDTO) policy).getAppIdsToEvict());
        } else if (policy instanceof ComposedTaskEvictPolicyDTO) {
            ComposedTaskEvictPolicyDTO composedPolicyDTO = (ComposedTaskEvictPolicyDTO) policy;
            List<TaskEvictPolicyDTO> policyDTOList = composedPolicyDTO.getPolicyList();
            return new ComposedTaskEvictPolicy(composedPolicyDTO.getOperator(), policyDTOList);
        } else {
            log.error("TaskEvictPolicyDTO not support yet:{}", policy);
            throw new InternalException(ErrorCode.NOT_SUPPORT_FEATURE);
        }
    }

}
