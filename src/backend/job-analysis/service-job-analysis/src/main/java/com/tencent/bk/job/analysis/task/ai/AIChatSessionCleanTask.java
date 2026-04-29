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

package com.tencent.bk.job.analysis.task.ai;

import com.tencent.bk.job.analysis.config.AIProperties;
import com.tencent.bk.job.analysis.dao.AIChatSessionDAO;
import com.tencent.bk.job.common.redis.util.DistributedUniqueTask;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 清理AI场景会话记录的任务，复用 ai.chatHistory.maxKeepDays 配置
 */
@Slf4j
@Component
public class AIChatSessionCleanTask {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_RUNNING_MACHINE = "aiChatSessionCleanTask-running-machine";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final RedisTemplate<String, String> redisTemplate;
    private final AIProperties aiProperties;
    private final AIChatSessionDAO aiChatSessionDAO;

    @Autowired
    public AIChatSessionCleanTask(RedisTemplate<String, String> redisTemplate,
                                  AIProperties aiProperties,
                                  AIChatSessionDAO aiChatSessionDAO) {
        this.redisTemplate = redisTemplate;
        this.aiProperties = aiProperties;
        this.aiChatSessionDAO = aiChatSessionDAO;
    }

    public void execute() {
        log.info("AIChatSessionCleanTask start");
        StopWatch watch = new StopWatch();
        Boolean successExecuted = false;
        try {
            successExecuted = new DistributedUniqueTask<>(
                redisTemplate,
                AIChatSessionCleanTask.class.getSimpleName(),
                REDIS_KEY_RUNNING_MACHINE,
                machineIp,
                () -> {
                    doExecute(watch);
                    return true;
                }
            ).execute();
        } catch (Exception e) {
            log.error("AIChatSessionCleanTask failed", e);
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (successExecuted != null && successExecuted) {
                log.info("AIChatSessionCleanTask finished, timeConsuming={}", watch.prettyPrint());
            }
        }
    }

    private void doExecute(StopWatch watch) {
        watch.start("cleanChatSessionByMaxKeepDays");
        Integer maxKeepDays = aiProperties.getChatHistory().getMaxKeepDays();
        cleanByMaxKeepDays(maxKeepDays);
        watch.stop();
    }

    private void cleanByMaxKeepDays(Integer maxKeepDays) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime lastKeepDate = currentDateTime.minusDays(maxKeepDays);
        LocalDateTime lastKeepDateStartTime = TimeUtil.getDayStartTime(lastKeepDate);
        long maxCreateTimeMills = lastKeepDateStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        log.info(
            "Begin to delete ai chat session before {}, maxCreateTimeMills={}",
            TimeUtil.getTimeStr(lastKeepDateStartTime, TIME_FORMAT),
            maxCreateTimeMills
        );
        int batchSize = 1000;
        int totalDeletedNum = 0;
        int deletedNum;
        do {
            deletedNum = aiChatSessionDAO.deleteByCreateTimeBefore(maxCreateTimeMills, batchSize);
            totalDeletedNum += deletedNum;
            if (deletedNum > 0) {
                ThreadUtils.sleep(1000);
            }
        } while (deletedNum > 0);
        log.info(
            "Finish cleanByMaxKeepDays({}), totalDeletedNum={}, maxCreateTimeMills={}",
            maxKeepDays,
            totalDeletedNum,
            maxCreateTimeMills
        );
    }
}
