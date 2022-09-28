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

package com.tencent.bk.job.manage.config.listener;

import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPIPDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对IP白名单数据中不存在hostId的数据使用CMDB中的完整数据补全hostId
 */
@Slf4j
@Component
public class WhiteIpMakeUpInitListener implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${job.whiteIp.makeUp.enabled:true}")
    private final boolean whiteIpMakeUpEnabled = true;

    private final String REDIS_KEY_WHITEIP_MAKEUP_TASK_RUNNING_MACHINE = "whiteIp-makeUp-task-running-machine";

    private final RedisTemplate<String, String> redisTemplate;
    private final WhiteIPIPDAO whiteIPIPDAO;
    private final BizCmdbClient bizCmdbClient;

    @Autowired
    public WhiteIpMakeUpInitListener(
        RedisTemplate<String, String> redisTemplate,
        WhiteIPIPDAO whiteIPIPDAO,
        BizCmdbClient bizCmdbClient) {
        this.redisTemplate = redisTemplate;
        this.whiteIPIPDAO = whiteIPIPDAO;
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if (whiteIpMakeUpEnabled) {
            String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_WHITEIP_MAKEUP_TASK_RUNNING_MACHINE);
            if (StringUtils.isNotBlank(runningMachine)) {
                log.info("skip, whiteIp makeUp task is running at another machine:{}", runningMachine);
                return;
            }
            startWhiteIpMakeUpThread();
        } else {
            log.info("whiteIp makeUp task is canceled by config");
        }
    }

    private void startWhiteIpMakeUpThread() {
        new Thread(() -> {
            // 开一个心跳子线程，维护当前机器正在跑白名单IP补全任务的状态
            RedisKeyHeartBeatThread heartBeatThread = new RedisKeyHeartBeatThread(
                redisTemplate,
                REDIS_KEY_WHITEIP_MAKEUP_TASK_RUNNING_MACHINE,
                IpUtils.getFirstMachineIP(),
                5000L,
                4000L
            );
            heartBeatThread.setName("[" + heartBeatThread.getId() +
                "]-WhiteIpMakeUpTaskHeartBeatThread");
            heartBeatThread.start();
            try {
                makeUpWhiteIps();
            } catch (Throwable t) {
                log.warn("Fail to makeUpWhiteIps", t);
            } finally {
                heartBeatThread.stopAtOnce();
                log.info("whiteIp makeUp task finished");
            }
        }).start();
    }

    private void makeUpWhiteIps() {
        int start = 0;
        int batchSize = 100;
        List<WhiteIPIPDTO> whiteIpList;
        int totalCount = 0;
        do {
            whiteIpList = whiteIPIPDAO.listWhiteIPIPWithNullHostId(start, batchSize);
            if (CollectionUtils.isEmpty(whiteIpList)) {
                continue;
            }
            int count = 0;
            int foundHostIdCount = addHostIdForWhiteIps(whiteIpList);
            log.debug("{} hostIds found for {} whiteIps", foundHostIdCount, whiteIpList.size());
            for (WhiteIPIPDTO whiteIPIPDTO : whiteIpList) {
                if (whiteIPIPDTO.getHostId() != null) {
                    count += whiteIPIPDAO.updateHostIdById(whiteIPIPDTO.getId(), whiteIPIPDTO.getHostId());
                }
            }
            start += batchSize;
            totalCount += count;
            log.info("{}/{} whiteIps have been made up", count, whiteIpList.size());
        } while (!CollectionUtils.isEmpty(whiteIpList));
        log.info("{} whiteIps have been made up in total", totalCount);
    }

    private int addHostIdForWhiteIps(List<WhiteIPIPDTO> whiteIpList) {
        List<String> cloudIpList = new ArrayList<>();
        List<String> cloudIpv6List = new ArrayList<>();
        for (WhiteIPIPDTO whiteIPIPDTO : whiteIpList) {
            String ip = whiteIPIPDTO.getIp();
            String ipv6 = whiteIPIPDTO.getIpv6();
            if (StringUtils.isNotBlank(ip)) {
                cloudIpList.add(whiteIPIPDTO.getCloudIp());
            } else if (StringUtils.isNotBlank(ipv6)) {
                cloudIpv6List.add(whiteIPIPDTO.getCloudIpv6());
            }
        }
        List<ApplicationHostDTO> hostList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cloudIpList)) {
            hostList.addAll(bizCmdbClient.listHostsByCloudIps(cloudIpList));
        }
        if (CollectionUtils.isNotEmpty(cloudIpv6List)) {
            hostList.addAll(bizCmdbClient.listHostsByCloudIpv6s(cloudIpv6List));
        }
        Map<String, ApplicationHostDTO> hostMap = new HashMap<>();
        for (ApplicationHostDTO hostDTO : hostList) {
            if (StringUtils.isNotBlank(hostDTO.getIp())) {
                hostMap.put(hostDTO.getCloudAreaId() + ":" + hostDTO.getIp(), hostDTO);
            }
            if (StringUtils.isNotBlank(hostDTO.getIpv6())) {
                hostMap.put(hostDTO.getCloudAreaId() + ":" + hostDTO.getIpv6(), hostDTO);
            }
        }
        int foundHostIdCount = 0;
        for (WhiteIPIPDTO whiteIPIPDTO : whiteIpList) {
            String ip = whiteIPIPDTO.getIp();
            String ipv6 = whiteIPIPDTO.getIpv6();
            if (StringUtils.isNotBlank(ip) && hostMap.containsKey(whiteIPIPDTO.getCloudIp())) {
                whiteIPIPDTO.setHostId(hostMap.get(whiteIPIPDTO.getCloudIp()).getHostId());
                foundHostIdCount += 1;
                log.info(
                    "Makeup whiteIp {}(cloudIp={}) with hostId={}",
                    whiteIPIPDTO.getId(),
                    whiteIPIPDTO.getCloudIp(),
                    whiteIPIPDTO.getHostId()
                );
            } else if (StringUtils.isNotBlank(ipv6) && hostMap.containsKey(whiteIPIPDTO.getCloudIpv6())) {
                whiteIPIPDTO.setHostId(hostMap.get(whiteIPIPDTO.getCloudIpv6()).getHostId());
                foundHostIdCount += 1;
                log.info(
                    "Makeup whiteIp {}(cloudIpv6={}) with hostId={}",
                    whiteIPIPDTO.getId(),
                    whiteIPIPDTO.getCloudIpv6(),
                    whiteIPIPDTO.getHostId()
                );
            }
        }
        return foundHostIdCount;
    }
}
