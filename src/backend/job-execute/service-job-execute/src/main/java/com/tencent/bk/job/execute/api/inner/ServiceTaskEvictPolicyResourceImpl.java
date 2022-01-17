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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.constant.RedisConstants;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyManager;
import com.tencent.bk.job.execute.model.inner.ComposedTaskEvictPolicyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ServiceTaskEvictPolicyResourceImpl implements ServiceTaskEvictPolicyResource {

    private final TaskEvictPolicyManager taskEvictPolicyManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public ServiceTaskEvictPolicyResourceImpl(TaskEvictPolicyManager taskEvictPolicyManager,
                                              RedisTemplate<String, String> redisTemplate) {
        this.taskEvictPolicyManager = taskEvictPolicyManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public InternalResponse<ComposedTaskEvictPolicyDTO> getCurrentPolicy() {
        return InternalResponse.buildSuccessResp(taskEvictPolicyManager.getPolicy());
    }

    @Override
    public InternalResponse<Boolean> setPolicy(ComposedTaskEvictPolicyDTO policyDTO) {
        try {
            // 将策略写入Redis
            redisTemplate.opsForValue().set(RedisConstants.KEY_EXECUTE_TASK_EVICT_POLICY, JsonUtils.toJson(policyDTO));
            // 再立即更新策略缓存
            taskEvictPolicyManager.updatePolicy();
            return InternalResponse.buildSuccessResp(true);
        } catch (Exception e) {
            log.error("Fail to write/update policy", e);
            return InternalResponse.buildSuccessResp(false);
        }
    }

    @Override
    public InternalResponse<Boolean> clearPolicy() {
        try {
            // 清除Redis中的策略
            redisTemplate.delete(RedisConstants.KEY_EXECUTE_TASK_EVICT_POLICY);
            // 再立即更新策略缓存
            taskEvictPolicyManager.updatePolicy();
            return InternalResponse.buildSuccessResp(true);
        } catch (Exception e) {
            log.error("Fail to clear policy", e);
            return InternalResponse.buildSuccessResp(false);
        }
    }
}
