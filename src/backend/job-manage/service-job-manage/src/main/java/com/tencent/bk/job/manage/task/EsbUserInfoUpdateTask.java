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

package com.tencent.bk.job.manage.task;

import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.EsbUserInfoDTO;
import com.tencent.bk.job.manage.service.PaaSService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2020/1/12
 * @Version 1.0
 */
@Slf4j
@Component
public class EsbUserInfoUpdateTask {

    private static final String REDIS_KEY_SYNC_USER_JOB_LOCK = "sync-user-job-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final Logger logger = LoggerFactory.getLogger(EsbUserInfoUpdateTask.class);

    static {
        List<String> keyList = Arrays.asList(REDIS_KEY_SYNC_USER_JOB_LOCK);
        keyList.forEach(key -> {
            try {
                //进程重启首先尝试释放上次加上的锁避免死锁
                LockUtils.releaseDistributedLock(key, machineIp);
            } catch (Throwable t) {
                logger.info("Redis key:" + key + " does not need to be released, ignore");
            }
        });
    }

    private final String REDIS_KEY_SYNC_USER_JOB_RUNNING_MACHINE = "sync-user-job-running-machine";
    private final RedisTemplate<String, String> redisTemplate;
    private DSLContext dslContext;
    private PaaSService paaSService;
    private EsbUserInfoDAO esbUserInfoDAO;

    @Autowired
    public EsbUserInfoUpdateTask(DSLContext dslContext, PaaSService paaSService, EsbUserInfoDAO esbUserInfoDAO,
                                 RedisTemplate<String, String> redisTemplate) {
        this.dslContext = dslContext;
        this.paaSService = paaSService;
        this.esbUserInfoDAO = esbUserInfoDAO;
        this.redisTemplate = redisTemplate;
    }

    public boolean execute() {
        log.info("syncUser arranged");
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            REDIS_KEY_SYNC_USER_JOB_LOCK, machineIp, 5000);
        if (!lockGotten) {
            log.info("syncUser lock not gotten, return");
            return false;
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_SYNC_USER_JOB_RUNNING_MACHINE);
        if (StringUtils.isNotBlank(runningMachine)) {
            //已有同步线程在跑，不再同步
            log.warn("sync user thread already running on {}", runningMachine);
            return false;
        }
        // 开一个心跳子线程，维护当前机器正在同步用户的状态
        RedisKeyHeartBeatThread userSyncRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            REDIS_KEY_SYNC_USER_JOB_RUNNING_MACHINE,
            machineIp,
            5000L,
            4000L
        );
        userSyncRedisKeyHeartBeatThread.setName("userSyncRedisKeyHeartBeatThread");
        userSyncRedisKeyHeartBeatThread.start();
        logger.info("updateEsbUserInfo:beigin");
        StopWatch watch = new StopWatch("syncUser");
        watch.start("total");
        try {
            // 1.接口数据拉取
            List<BkUserDTO> userList = paaSService.getAllUserList("", "100");
            if (null == userList) {
                userList = new ArrayList<>();
            }
            // 2.组装
            var remoteUserSet = userList.stream().map(it -> new EsbUserInfoDTO(it.getId(), it.getUsername(),
                it.getDisplayName(), it.getLogo(), System.currentTimeMillis())).collect(Collectors.toSet());
            if (remoteUserSet.isEmpty()) {
                logger.warn("updateEsbUserInfo: fail to fetch remote userInfo, return");
                return false;
            }

            // 3.计算差异数据
            val localUserSet = new HashSet<EsbUserInfoDTO>(esbUserInfoDAO.listEsbUserInfo());
            val clonedRemoteUserSet = new HashSet<EsbUserInfoDTO>(remoteUserSet);
            remoteUserSet.removeAll(localUserSet);
            val insertSet = remoteUserSet;
            logger.info("insertUserInfoSet=" + insertSet.stream()
                .map(EsbUserInfoDTO::toString).collect(Collectors.joining(",")));
            localUserSet.removeAll(clonedRemoteUserSet);
            val deleteSet = localUserSet;
            logger.info("deleteUserInfoSet=" + deleteSet.stream()
                .map(EsbUserInfoDTO::toString).collect(Collectors.joining(",")));

            // 4.入库
            dslContext.transaction(configuration -> {
                val context = DSL.using(configuration);
                deleteSet.forEach(esbUserInfoDTO -> esbUserInfoDAO.deleteEsbUserInfoById(context,
                    esbUserInfoDTO.getId()));
                insertSet.forEach(esbUserInfoDTO -> esbUserInfoDAO.insertEsbUserInfo(context, esbUserInfoDTO));
            });
        } catch (Throwable t) {
            log.error("FATAL: syncUser thread fail", t);
        } finally {
            userSyncRedisKeyHeartBeatThread.setRunFlag(false);
            watch.stop();
            log.info("syncUser time consuming:" + watch.toString());
        }
        return true;
    }
}
