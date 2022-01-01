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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.RedisConstants;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 任务驱逐策略管理器，每隔一段时间从Redis加载驱逐策略
 */
@Slf4j
@Component
public class TaskEvictPolicyManager {

    // 策略更新时间间隔：30s
    private final int POLICY_UPDATE_INTERVAL_MILLS = 30000;
    private String policyJsonStr = null;
    private volatile ComposedTaskEvictPolicy policy = null;

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public TaskEvictPolicyManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ComposedTaskEvictPolicy getPolicy() {
        return policy;
    }

    /**
     * 从Redis加载并更新驱逐策略
     */
    public void updatePolicy() {
        String loadedPolicyJsonStr = redisTemplate.opsForValue()
            .get(RedisConstants.KEY_EXECUTE_TASK_EVICT_POLICY);
        if (StringUtil.isChanged(policyJsonStr, loadedPolicyJsonStr)) {
            policyJsonStr = loadedPolicyJsonStr;
            try {
                policy = JsonUtils.fromJson(
                    loadedPolicyJsonStr, new TypeReference<ComposedTaskEvictPolicy>() {
                    }
                );
                log.info("loaded new policy:{}", loadedPolicyJsonStr);
            } catch (Exception e) {
                FormattingTuple message = MessageFormatter.format(
                    "Fail to parse taskEvictPolicy from {}",
                    loadedPolicyJsonStr
                );
                log.warn(message.getMessage(), e);
                throw e;
            }
        } else {
            log.debug("taskEvictPolicy not change");
        }
    }

    @PostConstruct
    private void init() {
        Thread taskEvictPolicyLoader = new Thread(() -> {
            // 每隔一定时间更新驱逐策略
            try {
                updatePolicy();
            } catch (Exception e) {
                log.warn("Fail to update taskEvictPolicy", e);
            } finally {
                ThreadUtils.sleep(POLICY_UPDATE_INTERVAL_MILLS);
            }
        });
        taskEvictPolicyLoader.setName("taskEvictPolicyLoader");
        // 设为守护线程，不阻塞进程退出
        taskEvictPolicyLoader.setDaemon(true);
        taskEvictPolicyLoader.start();
    }
}
