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

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台任务：清理DB中的不属于任何业务的无效主机
 */
@Slf4j
@Component
public class ClearDeletedHostsTask {

    private static final String CLEAR_DELETED_HOSTS_TASK_RUNNING_MACHINE = "clear:deleted:hosts";

    private final RedisTemplate<String, String> redisTemplate;
    private final IBizCmdbClient bizCmdbClient;
    private final ApplicationHostDAO applicationHostDAO;

    @Autowired
    public ClearDeletedHostsTask(RedisTemplate<String, String> redisTemplate,
                                 IBizCmdbClient bizCmdbClient,
                                 ApplicationHostDAO applicationHostDAO) {
        this.redisTemplate = redisTemplate;
        this.bizCmdbClient = bizCmdbClient;
        this.applicationHostDAO = applicationHostDAO;
    }

    public boolean execute() {
        String machineIp = IpUtils.getFirstMachineIP();
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            CLEAR_DELETED_HOSTS_TASK_RUNNING_MACHINE,
            machineIp,
            5000
        );
        if (!lockGotten) {
            String runningMachine = redisTemplate.opsForValue().get(CLEAR_DELETED_HOSTS_TASK_RUNNING_MACHINE);
            if (StringUtils.isNotBlank(runningMachine)) {
                //已有清理线程在跑，不再清理
                log.warn("sync user thread already running on {}", runningMachine);
            } else {
                log.info("clear deleted hosts lock not gotten, return");
            }
            return false;
        }
        // 开一个心跳子线程，维护当前机器正在清理无效主机的状态
        RedisKeyHeartBeatThread clearDeletedHostsRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            CLEAR_DELETED_HOSTS_TASK_RUNNING_MACHINE,
            machineIp,
            5000L,
            4000L
        );
        clearDeletedHostsRedisKeyHeartBeatThread.setName("clearDeletedHostsRedisKeyHeartBeatThread");
        clearDeletedHostsRedisKeyHeartBeatThread.start();
        try {
            clearDeletedHosts();
            return true;
        } catch (Throwable t) {
            log.warn("Fail to clearDeletedHosts", t);
            return false;
        } finally {
            clearDeletedHostsRedisKeyHeartBeatThread.stopAtOnce();
        }
    }

    private void clearDeletedHosts() {
        // 1.查出不属于任何业务且最近半小时内未更新的主机ID进行check
        List<Long> hostIdList = applicationHostDAO.listHostIdNotUpdated(
            JobConstants.PUBLIC_APP_ID,
            LocalDateTime.now().minusMinutes(30)
        );
        if (CollectionUtils.isEmpty(hostIdList)) {
            log.info("no deleted hosts found, finish");
            return;
        } else {
            log.info("{} hosts(not belong to any biz) found", hostIdList.size());
        }
        // 2.分批查询、核验、删除
        int batchSize = 500;
        int start = 0;
        int end = start + batchSize;
        int hostIdNum = hostIdList.size();
        end = Math.min(end, hostIdNum);
        int deletedHostTotalNum = 0;
        do {
            List<Long> subList = hostIdList.subList(start, end);
            List<ApplicationHostDTO> hosts = bizCmdbClient.listHostsByHostIds(subList);
            Set<Long> validHostIds = hosts.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toSet());
            List<Long> hostIdsToBeDelete = subList.stream()
                .filter(hostId -> !validHostIds.contains(hostId))
                .collect(Collectors.toList());
            int deletedHostNum = applicationHostDAO.batchDeleteHostById(hostIdsToBeDelete);
            deletedHostTotalNum += deletedHostNum;
            log.info("{} host deleted:{}", deletedHostNum, hostIdsToBeDelete);
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, hostIdNum);
        } while (start < hostIdNum);
        log.info("clearDeletedHosts finished, {} hosts deleted", deletedHostTotalNum);
    }

}
