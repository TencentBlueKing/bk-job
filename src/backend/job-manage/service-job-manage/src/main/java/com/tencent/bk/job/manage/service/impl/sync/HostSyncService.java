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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 主机同步逻辑
 */
@Slf4j
@Service
public class HostSyncService {

    private final AppHostsUpdateHelper appHostsUpdateHelper;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostService hostService;
    private final AgentStatusService agentStatusService;

    @Autowired
    public HostSyncService(AppHostsUpdateHelper appHostsUpdateHelper,
                           ApplicationHostDAO applicationHostDAO,
                           HostService hostService,
                           AgentStatusService agentStatusService) {
        this.appHostsUpdateHelper = appHostsUpdateHelper;
        this.applicationHostDAO = applicationHostDAO;
        this.hostService = hostService;
        this.agentStatusService = agentStatusService;
    }

    private List<ApplicationHostDTO> getHostsByAppInfo(IBizCmdbClient bizCmdbClient, ApplicationDTO applicationDTO) {
        List<CcInstanceDTO> ccInstanceDTOList = new ArrayList<>();
        ccInstanceDTOList.add(new CcInstanceDTO(CcNodeTypeEnum.BIZ.getType(), applicationDTO.getBizIdIfBizApp()));
        List<ApplicationHostDTO> hosts = bizCmdbClient.getHosts(applicationDTO.getBizIdIfBizApp(),
            ccInstanceDTOList);
        // 获取Agent状态
        agentStatusService.fillRealTimeAgentStatus(hosts);
        return hosts;
    }

    private List<ApplicationHostDTO> computeInsertList(
        Long bizId,
        Set<Long> localBizHostIds,
        List<ApplicationHostDTO> applicationHostDTOList
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostDTO> insertList =
            applicationHostDTOList.stream().filter(ApplicationHostDTO ->
                !localBizHostIds.contains(ApplicationHostDTO.getHostId())).collect(Collectors.toList());
        watch.start("log insertList");
        log.info(String.format("bizId=%s,insertHostIds=%s", bizId, String.join(",",
            insertList.stream().map(ApplicationHostDTO::getHostId).map(Object::toString)
                .collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return insertList;
    }

    private List<ApplicationHostDTO> computeUpdateList(
        Long bizId,
        Set<Long> localBizHostIds,
        List<ApplicationHostDTO> applicationHostDTOList
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostDTO> updateList =
            applicationHostDTOList.stream().filter(ApplicationHostDTO ->
                localBizHostIds.contains(ApplicationHostDTO.getHostId())).collect(Collectors.toList());
        watch.start("log updateList");
        log.info(String.format("bizId=%s,updateHostIds=%s", bizId, String.join(",",
            updateList.stream().map(ApplicationHostDTO::getHostId)
                .map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return updateList;
    }

    private List<ApplicationHostDTO> computeDeleteList(
        Long bizId,
        Set<Long> ccBizHostIds,
        List<ApplicationHostDTO> localBizHosts
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostDTO> deleteList =
            localBizHosts.stream().filter(ApplicationHostDTO ->
                !ccBizHostIds.contains(ApplicationHostDTO.getHostId())).collect(Collectors.toList());
        watch.start("log deleteList");
        log.info(String.format("bizId=%s,deleteHostIds=%s", bizId, String.join(",",
            deleteList.stream().map(ApplicationHostDTO::getHostId).map(Object::toString)
                .collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return deleteList;
    }

    private void refreshBizHosts(Long bizId,
                                 List<ApplicationHostDTO> applicationHostDTOList) {
        StopWatch watch = new StopWatch();
        //找出要删除的/更新的/新增的分别处理
        //对比库中数据与接口数据
        watch.start("listHostInfoByBizId");
        List<ApplicationHostDTO> localBizHosts = applicationHostDAO.listHostInfoByBizId(bizId);
        watch.stop();
        watch.start("mapTo ccBizHostIds");
        Set<Long> ccBizHostIds =
            applicationHostDTOList.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toSet());
        watch.stop();
        watch.start("mapTo localBizHostIds");
        Set<Long> localBizHostIds =
            localBizHosts.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toSet());
        watch.stop();
        watch.start("log ccBizHostIds");
        log.info(String.format("bizId=%s,ccBizHostIds=%s", bizId, String.join(",",
            ccBizHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        watch.start("log localBizHostIds");
        log.info(String.format("bizId=%s,localBizHostIds=%s", bizId, String.join(",",
            localBizHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        watch.start("compute insertList");
        List<ApplicationHostDTO> insertList = computeInsertList(bizId, localBizHostIds, applicationHostDTOList);
        watch.stop();
        watch.start("compute updateList");
        List<ApplicationHostDTO> updateList = computeUpdateList(bizId, localBizHostIds, applicationHostDTOList);
        watch.stop();
        watch.start("compute deleteList");
        List<ApplicationHostDTO> deleteList = computeDeleteList(bizId, ccBizHostIds, localBizHosts);
        watch.stop();
        watch.start("deleteHostsFromBiz");
        // 记录一次业务主机同步过程中所有更新失败的主机ID
        // 需要从业务下移除的主机
        List<Long> removeFailHostIds = hostService.removeHostsFromBiz(bizId, deleteList);
        watch.stop();
        watch.start("insertHostsToApp");
        // 需要新增的主机
        List<Long> insertFailHostIds = hostService.insertHostsToBiz(bizId, insertList);
        watch.stop();
        watch.start("updateHostsInApp");
        // 需要更新的主机
        List<Long> updateFailHostIds = hostService.updateHostsInBiz(bizId, updateList);
        watch.stop();
        if (watch.getTotalTimeMillis() > 10000) {
            log.info("Performance:refreshBizHosts:bizId={},{}", bizId, watch.prettyPrint());
        } else {
            log.debug("Performance:refreshBizHosts:bizId={},{}", bizId, watch.prettyPrint());
        }
        log.info(
            Thread.currentThread().getName() +
                ":Finished:Statistics:bizId={}:insertFailHostIds={}," +
                "updateFailHostIds={},removeFailHostIds={}",
            bizId,
            insertFailHostIds,
            updateFailHostIds,
            removeFailHostIds
        );
    }

    private Pair<Long, Long> syncBizHostsIndeed(ApplicationDTO applicationDTO) {
        Long bizId = Long.valueOf(applicationDTO.getScope().getId());
        long cmdbInterfaceTimeConsuming = 0L;
        long writeToDBTimeConsuming = 0L;
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
        StopWatch bizHostsWatch = new StopWatch();
        bizHostsWatch.start("getHostsByAppInfo from CMDB");
        long startTime = System.currentTimeMillis();
        log.info("begin to syncBizHosts:bizId={}", bizId);
        List<ApplicationHostDTO> hosts = getHostsByAppInfo(bizCmdbClient, applicationDTO);
        cmdbInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        bizHostsWatch.stop();
        bizHostsWatch.start("updateHosts to local DB");
        startTime = System.currentTimeMillis();
        refreshBizHosts(bizId, hosts);
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        bizHostsWatch.stop();
        log.info("Performance:syncBizHosts:bizId={},{}", bizId, bizHostsWatch);
        return Pair.of(cmdbInterfaceTimeConsuming, writeToDBTimeConsuming);
    }

    public Pair<Long, Long> syncBizHostsAtOnce(ApplicationDTO applicationDTO) {
        Long bizId = Long.valueOf(applicationDTO.getScope().getId());
        try {
            appHostsUpdateHelper.waitAndStartBizHostsUpdating(bizId);
            return syncBizHostsIndeed(applicationDTO);
        } catch (Throwable t) {
            log.error("Fail to syncBizHosts of bizId " + bizId, t);
            return null;
        } finally {
            appHostsUpdateHelper.endToUpdateBizHosts(bizId);
        }
    }

}
