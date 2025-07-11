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
import com.tencent.bk.job.analysis.dao.AIChatHistoryDAO;
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
import java.util.List;

/**
 * 清理AI对话记录的任务
 */
@Slf4j
@Component
public class AIChatHistoryCleanTask {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_AI_CHAT_HISTORY_CLEAN_TASK_RUNNING_MACHINE =
        "aiChatHistoryCleanTask-running-machine";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final RedisTemplate<String, String> redisTemplate;
    private final AIProperties aiProperties;
    private final AIChatHistoryDAO aiChatHistoryDAO;

    @Autowired
    public AIChatHistoryCleanTask(RedisTemplate<String, String> redisTemplate,
                                  AIProperties aiProperties,
                                  AIChatHistoryDAO aiChatHistoryDAO) {
        this.redisTemplate = redisTemplate;
        this.aiProperties = aiProperties;
        this.aiChatHistoryDAO = aiChatHistoryDAO;
    }

    public void execute() {
        log.info("AIChatHistoryCleanTask start");
        StopWatch watch = new StopWatch();
        Boolean successExecuted = false;
        try {
            // 分布式唯一性保证
            successExecuted = new DistributedUniqueTask<>(
                redisTemplate,
                AIChatHistoryCleanTask.class.getSimpleName(),
                REDIS_KEY_AI_CHAT_HISTORY_CLEAN_TASK_RUNNING_MACHINE,
                machineIp,
                () -> {
                    doExecute(watch);
                    return true;
                }
            ).execute();
        } catch (Exception e) {
            log.error("AIChatHistoryCleanTask failed", e);
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (successExecuted != null && successExecuted) {
                log.info("AIChatHistoryCleanTask finished, timeConsuming={}", watch.prettyPrint());
            }
        }
    }

    public void doExecute(StopWatch watch) {
        watch.start("cleanChatHistoryByMaxKeepDays");
        Integer maxKeepDays = aiProperties.getChatHistory().getMaxKeepDays();
        cleanChatHistoryByMaxKeepDays(maxKeepDays);
        watch.stop();
        watch.start("cleanChatHistoryByMaxHistoryPerUser");
        Integer maxHistoryPerUser = aiProperties.getChatHistory().getMaxHistoryPerUser();
        cleanChatHistoryByMaxHistoryPerUser(maxHistoryPerUser);
        watch.stop();
    }

    /**
     * 根据最大保留天数清理聊天记录
     *
     * @param maxKeepDays 最大保留天数
     */
    private void cleanChatHistoryByMaxKeepDays(Integer maxKeepDays) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime lastKeepDate = currentDateTime.minusDays(maxKeepDays);
        LocalDateTime lastKeepDateStartTime = TimeUtil.getDayStartTime(lastKeepDate);
        long maxStartTimeMills = lastKeepDateStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        log.info(
            "Begin to delete ai chat history before {}, maxStartTimeMills={}",
            TimeUtil.getTimeStr(lastKeepDateStartTime, TIME_FORMAT),
            maxStartTimeMills
        );
        int batchSize = 1000;
        int totalDeletedNum = 0;
        int deletedNum;
        do {
            deletedNum = aiChatHistoryDAO.deleteChatHistory(maxStartTimeMills, batchSize);
            totalDeletedNum += deletedNum;
            if (deletedNum > 0) {
                ThreadUtils.sleep(1000);
            }
        } while (deletedNum > 0);
        log.info(
            "Finish to cleanChatHistoryByMaxKeepDays({}), totalDeletedNum={}, maxStartTimeMills={}",
            maxKeepDays,
            totalDeletedNum,
            maxStartTimeMills
        );
    }

    /**
     * 根据每个用户最大聊天记录数清理聊天记录
     *
     * @param maxHistoryPerUser 每个用户最大聊天记录数
     */
    private void cleanChatHistoryByMaxHistoryPerUser(Integer maxHistoryPerUser) {
        // 1.查出所有用户
        List<String> userList = aiChatHistoryDAO.listAllUserOfChatHistory();
        // 2.清理每个用户的聊天历史记录
        for (String username : userList) {
            Long maxId = aiChatHistoryDAO.getFirstIdAfterOffset(username, maxHistoryPerUser);
            if (maxId == null) {
                continue;
            }
            int deletedNum = cleanChatHistoryForUser(username, maxId);
            log.info("Finish to cleanChatHistoryForUser({}), maxId={}, deletedNum={}", username, maxId, deletedNum);
        }
        log.info("cleanChatHistoryByMaxHistoryPerUser finished");
    }

    /**
     * 清理某个用户的聊天记录
     *
     * @param username 用户名
     * @param maxId    最大记录id（包含）
     * @return 删除的记录条数
     */
    private int cleanChatHistoryForUser(String username, Long maxId) {
        int batchSize = 1000;
        int totalDeletedNum = 0;
        int deletedNum;
        do {
            deletedNum = aiChatHistoryDAO.deleteChatHistory(username, maxId, batchSize);
            totalDeletedNum += deletedNum;
            if (deletedNum > 0) {
                ThreadUtils.sleep(100);
            }
        } while (deletedNum > 0);
        return totalDeletedNum;
    }
}
