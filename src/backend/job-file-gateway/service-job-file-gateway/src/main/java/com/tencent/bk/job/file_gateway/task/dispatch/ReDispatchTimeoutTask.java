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

package com.tencent.bk.job.file_gateway.task.dispatch;

import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLockConfig;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.service.dispatch.ReDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

@Slf4j
@Service
public class ReDispatchTimeoutTask {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_REDISPATCH_TASK_RUNNING_MACHINE =
        "file-gateway:reDispatch-task-running-machine";
    private final FileTaskDAO fileTaskDAO;
    private final ReDispatchService reDispatchService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${job.file-gateway.reDispatch.timeoutTask.enabled:true}")
    private boolean reDispatchTimeoutTaskEnabled = true;

    @Value("${job.file-gateway.reDispatch.timeoutTask.timeoutSeconds:10}")
    private int reDispatchTaskTimeoutSeconds = 10;

    @Autowired
    public ReDispatchTimeoutTask(FileTaskDAO fileTaskDAO,
                                 ReDispatchService reDispatchService,
                                 RedisTemplate<String, String> redisTemplate) {
        this.fileTaskDAO = fileTaskDAO;
        this.reDispatchService = reDispatchService;
        this.redisTemplate = redisTemplate;
    }

    public void run() {
        if (!reDispatchTimeoutTaskEnabled) {
            log.info("reDispatch timeout task not enabled, you can config it in configuration file by set job" +
                ".file-gateway.reDispatch.timeoutTask.enabled=true");
            return;
        }
        // 分布式唯一性保证
        HeartBeatRedisLockConfig config = HeartBeatRedisLockConfig.getDefault();
        config.setHeartBeatThreadName("reDispatchTaskRedisKeyHeartBeatThread");
        HeartBeatRedisLock lock = new HeartBeatRedisLock(
            redisTemplate,
            REDIS_KEY_REDISPATCH_TASK_RUNNING_MACHINE,
            machineIp,
            config
        );
        LockResult lockResult = lock.lock();
        if (!lockResult.isLockGotten()) {
            log.info(
                "lock {} gotten by another machine: {}, return",
                REDIS_KEY_REDISPATCH_TASK_RUNNING_MACHINE,
                lockResult.getLockValue()
            );
            return;
        }
        try {
            reDispatchFileSourceTasks();
        } finally {
            lockResult.tryToRelease();
        }
    }

    private void reDispatchFileSourceTasks() {
        StopWatch watch = new StopWatch("reDispatchFileSourceTasks");
        watch.start("listTimeoutFileSourceTaskIds");
        // 找出未结束且长时间无响应的任务，无响应且未结束的任务就应当被重调度了

        long intervalStart = computeReDispatchIntervalStart();
        long intervalEnd = computeReDispatchIntervalEnd();
        List<String> timeoutFileSourceTaskIdList = fileTaskDAO.listTimeoutFileSourceTaskIds(
            intervalStart,
            intervalEnd,
            TaskStatusEnum.getRunningStatusSet(),
            0,
            -1
        );
        watch.stop();
        if (timeoutFileSourceTaskIdList.isEmpty()) {
            log.info("no fileSourceTask need to be reDispatch");
            return;
        }
        log.info(
            "find {} fileSourceTask between [{},{}] to reDispatch: {}",
            timeoutFileSourceTaskIdList.size(),
            TimeUtil.formatTime(intervalStart),
            TimeUtil.formatTime(intervalEnd),
            timeoutFileSourceTaskIdList
        );
        watch.start("reDispatch Tasks");
        // 进行超时重调度
        for (String fileSourceTaskId : timeoutFileSourceTaskIdList) {
            boolean result = reDispatchService.reDispatchByGateway(fileSourceTaskId, 0L, 5000L);
            log.info(
                "reDispatch fileSourceTask by timeout({}s):{}, result={}",
                reDispatchTaskTimeoutSeconds,
                fileSourceTaskId,
                result
            );
        }
        watch.stop();
        if (watch.getTotalTimeSeconds() > 10) {
            log.warn(
                "SLOW: reDispatched {} fileSourceTask, timeConsuming:{}",
                timeoutFileSourceTaskIdList.size(),
                watch.prettyPrint()
            );
        } else {
            log.info(
                "reDispatched {} fileSourceTask, timeConsuming: {}s",
                timeoutFileSourceTaskIdList.size(),
                watch.getTotalTimeSeconds()
            );
        }
    }

    /**
     * 计算重调度区间开始时间
     *
     * @return 重调度区间开始时间(ms)
     */
    private long computeReDispatchIntervalStart() {
        // 只对最近半小时内的任务进行重调度
        long reDispatchStartIntervalMills = 30 * 60 * 1000L;
        return System.currentTimeMillis() - reDispatchStartIntervalMills;
    }

    /**
     * 计算重调度区间结束时间
     *
     * @return 重调度区间结束时间(ms)
     */
    private long computeReDispatchIntervalEnd() {
        // 对已经超时未更新状态的任务进行重调度
        long fileSourceTaskStatusExpireTimeMills = reDispatchTaskTimeoutSeconds * 1000L;
        return System.currentTimeMillis() - fileSourceTaskStatusExpireTimeMills;
    }
}
