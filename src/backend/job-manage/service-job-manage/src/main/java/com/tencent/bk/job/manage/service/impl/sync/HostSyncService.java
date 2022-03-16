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
import com.tencent.bk.job.common.cc.sdk.CcClient;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.HostService;
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

    private AppHostsUpdateHelper appHostsUpdateHelper;
    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostService hostService;

    @Autowired
    public HostSyncService(ApplicationDAO applicationDAO,
                           ApplicationHostDAO applicationHostDAO,
                           HostService hostService) {
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.hostService = hostService;
    }

    private List<ApplicationHostInfoDTO> getHostsByAppInfo(CcClient ccClient, ApplicationDTO applicationDTO) {
        List<CcInstanceDTO> ccInstanceDTOList = new ArrayList<>();
        ccInstanceDTOList.add(new CcInstanceDTO(CcNodeTypeEnum.APP.getType(), applicationDTO.getId()));
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList = ccClient.getHosts(applicationDTO.getId(),
            ccInstanceDTOList);
        // 获取Agent状态
        hostService.fillAgentStatus(applicationHostInfoDTOList);
        return applicationHostInfoDTOList;
    }

    private List<ApplicationHostInfoDTO> computeInsertList(
        Long appId,
        Set<Long> localAppHostIds,
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostInfoDTO> insertList =
            applicationHostInfoDTOList.stream().filter(applicationHostInfoDTO ->
                !localAppHostIds.contains(applicationHostInfoDTO.getHostId())).collect(Collectors.toList());
        watch.start("log insertList");
        log.info(String.format("appId=%s,insertHostIds=%s", appId, String.join(",",
            insertList.stream().map(ApplicationHostInfoDTO::getHostId).map(Object::toString)
                .collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return insertList;
    }

    private List<ApplicationHostInfoDTO> computeUpdateList(
        Long appId,
        Set<Long> localAppHostIds,
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostInfoDTO> updateList =
            applicationHostInfoDTOList.stream().filter(applicationHostInfoDTO ->
                localAppHostIds.contains(applicationHostInfoDTO.getHostId())).collect(Collectors.toList());
        watch.start("log updateList");
        log.info(String.format("appId=%s,updateHostIds=%s", appId, String.join(",",
            updateList.stream().map(ApplicationHostInfoDTO::getHostId)
                .map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return updateList;
    }

    private List<ApplicationHostInfoDTO> computeDeleteList(
        Long appId,
        Set<Long> ccAppHostIds,
        List<ApplicationHostInfoDTO> localAppHosts
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostInfoDTO> deleteList =
            localAppHosts.stream().filter(applicationHostInfoDTO ->
                !ccAppHostIds.contains(applicationHostInfoDTO.getHostId())).collect(Collectors.toList());
        watch.start("log deleteList");
        log.info(String.format("appId=%s,deleteHostIds=%s", appId, String.join(",",
            deleteList.stream().map(ApplicationHostInfoDTO::getHostId).map(Object::toString)
                .collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return deleteList;
    }

    private int refreshAppHosts(Long appId,
                                List<ApplicationHostInfoDTO> applicationHostInfoDTOList) {
        StopWatch watch = new StopWatch();
        //找出要删除的/更新的/新增的分别处理
        //对比库中数据与接口数据
        watch.start("listHostInfoByAppId");
        List<ApplicationHostInfoDTO> localAppHosts = applicationHostDAO.listHostInfoByAppId(appId);
        watch.stop();
        watch.start("mapTo ccAppHostIds");
        Set<Long> ccAppHostIds =
            applicationHostInfoDTOList.stream().map(ApplicationHostInfoDTO::getHostId).collect(Collectors.toSet());
        watch.stop();
        watch.start("mapTo localAppHostIds");
        Set<Long> localAppHostIds =
            localAppHosts.stream().map(ApplicationHostInfoDTO::getHostId).collect(Collectors.toSet());
        watch.stop();
        watch.start("log ccAppHostIds");
        log.info(String.format("appId=%s,ccAppHostIds=%s", appId, String.join(",",
            ccAppHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        watch.start("log localAppHostIds");
        log.info(String.format("appId=%s,localAppHostIds=%s", appId, String.join(",",
            localAppHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        watch.start("compute insertList");
        List<ApplicationHostInfoDTO> insertList = computeInsertList(appId, localAppHostIds, applicationHostInfoDTOList);
        watch.stop();
        watch.start("compute updateList");
        List<ApplicationHostInfoDTO> updateList = computeUpdateList(appId, localAppHostIds, applicationHostInfoDTOList);
        watch.stop();
        watch.start("compute deleteList");
        List<ApplicationHostInfoDTO> deleteList = computeDeleteList(appId, ccAppHostIds, localAppHosts);
        watch.stop();
        watch.start("deleteHostsFromApp");
        // 记录一次业务主机同步过程中所有更新失败的主机ID
        // 需要删除的主机
        List<Long> deleteFailHostIds = hostService.deleteHostsFromApp(appId, deleteList);
        watch.stop();
        watch.start("insertHostsToApp");
        // 需要新增的主机
        List<Long> insertFailHostIds = hostService.insertHostsToApp(appId, insertList);
        watch.stop();
        watch.start("updateHostsInApp");
        // 需要更新的主机
        List<Long> updateFailHostIds = hostService.updateHostsInApp(appId, updateList);
        watch.stop();
        if (watch.getTotalTimeMillis() > 10000) {
            log.info("Performance:refreshAppHosts:appId={},{}", appId, watch.prettyPrint());
        } else {
            log.debug("Performance:refreshAppHosts:appId={},{}", appId, watch.prettyPrint());
        }
        log.info(
            Thread.currentThread().getName() +
                ":Finished:Statistics:appId={}:insertFailHostIds={}," +
                "updateFailHostIds={},deleteFailHostIds={}",
            appId,
            insertFailHostIds,
            updateFailHostIds,
            deleteFailHostIds
        );
        return 1;
    }

    private Pair<Long, Long> syncAppHostsIndeed(ApplicationDTO applicationDTO) {
        Long appId = applicationDTO.getId();
        Long cmdbInterfaceTimeConsuming = 0L;
        Long writeToDBTimeConsuming = 0L;
        CcClient ccClient = CcClientFactory.getCcClient();
        StopWatch appHostsWatch = new StopWatch();
        appHostsWatch.start("getHostsByAppInfo from CMDB");
        Long startTime = System.currentTimeMillis();
        log.info("begin to syncAppHosts:appId={}", appId);
        List<ApplicationHostInfoDTO> hosts = getHostsByAppInfo(ccClient, applicationDTO);
        cmdbInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        appHostsWatch.stop();
        appHostsWatch.start("updateHosts to local DB");
        startTime = System.currentTimeMillis();
        refreshAppHosts(appId, hosts);
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        appHostsWatch.stop();
        log.info("Performance:syncAppHosts:appId={},{}", appId, appHostsWatch.toString());
        return Pair.of(cmdbInterfaceTimeConsuming, writeToDBTimeConsuming);
    }

    public Pair<Long, Long> syncAppHostsAtOnce(ApplicationDTO applicationDTO) {
        Long appId = applicationDTO.getId();
        try {
            appHostsUpdateHelper.waitAndStartAppHostsUpdating(appId);
            return syncAppHostsIndeed(applicationDTO);
        } catch (Throwable t) {
            log.error("Fail to syncAppHosts of appId " + appId, t);
            return null;
        } finally {
            appHostsUpdateHelper.endToUpdateAppHosts(appId);
        }
    }

}
