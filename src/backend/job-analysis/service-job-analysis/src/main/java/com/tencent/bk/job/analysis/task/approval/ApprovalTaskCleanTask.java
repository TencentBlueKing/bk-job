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

package com.tencent.bk.job.analysis.task.approval;

import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.dao.ApprovalTaskDAO;
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
 * 清理过期的审批任务记录。
 * <p>
 * <b>保留期独立配置，不与 AI 会话共用</b>：审批记录是"谁在什么时候批准了什么"的凭据，
 * 保留期由安全要求决定；而参数快照里存着加密后的密码与脚本，留得越久数据面越大。
 * 两个诉求都与 AI 对话记录无关，共用配置只会让任何一方的调整误伤另一方。
 * <p>
 * <b>删除刻意跳过 EXECUTING</b>（见 {@link ApprovalTaskDAO#deleteByCreateTimeBefore}）：
 * 停在 EXECUTING 的任务意味着"已下发但结果未知"，正是最需要人工核对的那批记录，
 * 清理掉就再也查不出下游到底执行了没有。
 */
@Slf4j
@Component
public class ApprovalTaskCleanTask {

    private static final String MACHINE_IP = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_RUNNING_MACHINE = "approvalTaskCleanTask-running-machine";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int BATCH_SIZE = 1000;
    private static final int DEFAULT_MAX_KEEP_DAYS = 30;

    private final RedisTemplate<String, String> redisTemplate;
    private final ApprovalProperties approvalProperties;
    private final ApprovalTaskDAO approvalTaskDAO;

    @Autowired
    public ApprovalTaskCleanTask(RedisTemplate<String, String> redisTemplate,
                                 ApprovalProperties approvalProperties,
                                 ApprovalTaskDAO approvalTaskDAO) {
        this.redisTemplate = redisTemplate;
        this.approvalProperties = approvalProperties;
        this.approvalTaskDAO = approvalTaskDAO;
    }

    public void execute() {
        log.info("ApprovalTaskCleanTask start");
        StopWatch watch = new StopWatch();
        Boolean successExecuted = false;
        try {
            successExecuted = new DistributedUniqueTask<>(
                redisTemplate,
                ApprovalTaskCleanTask.class.getSimpleName(),
                REDIS_KEY_RUNNING_MACHINE,
                MACHINE_IP,
                () -> {
                    doExecute(watch);
                    return true;
                }
            ).execute();
        } catch (Exception e) {
            log.error("ApprovalTaskCleanTask failed", e);
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (successExecuted != null && successExecuted) {
                log.info("ApprovalTaskCleanTask finished, timeConsuming={}", watch.prettyPrint());
            }
        }
    }

    private void doExecute(StopWatch watch) {
        watch.start("cleanApprovalTaskByMaxKeepDays");
        try {
            cleanByMaxKeepDays(resolveMaxKeepDays());
        } finally {
            watch.stop();
        }
    }

    void cleanByMaxKeepDays(int maxKeepDays) {
        LocalDateTime lastKeepDate = LocalDateTime.now().minusDays(maxKeepDays);
        LocalDateTime lastKeepDateStartTime = TimeUtil.getDayStartTime(lastKeepDate);
        long maxCreateTimeMills = lastKeepDateStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        log.info(
            "Begin to delete approval task before {}, maxCreateTimeMills={}",
            TimeUtil.getTimeStr(lastKeepDateStartTime, TIME_FORMAT),
            maxCreateTimeMills
        );
        int totalDeletedNum = 0;
        int deletedNum;
        do {
            deletedNum = approvalTaskDAO.deleteByCreateTimeBefore(maxCreateTimeMills, BATCH_SIZE);
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

    /**
     * 配错成 0 或负数会把当天的记录一起删掉，因此非法值一律回落到默认保留期
     */
    int resolveMaxKeepDays() {
        Integer maxKeepDays = approvalProperties.getMaxKeepDays();
        if (maxKeepDays == null || maxKeepDays <= 0) {
            log.warn("Illegal approval maxKeepDays {}, fallback to {}", maxKeepDays, DEFAULT_MAX_KEEP_DAYS);
            return DEFAULT_MAX_KEEP_DAYS;
        }
        return maxKeepDays;
    }
}
