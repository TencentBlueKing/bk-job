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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import com.tencent.bk.job.manage.service.host.strategy.NoTenantListHostStrategy;
import com.tencent.bk.job.manage.service.host.strategy.NoTenantListHostStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户无关的主机服务接口实现，仅用于系统内部调用
 */
@Slf4j
@Service
public class NoTenantHostServiceImpl extends BaseHostService implements NoTenantHostService {

    private final NoTenantHostDAO noTenantHostDAO;
    private final ApplicationService applicationService;
    private final HostCache hostCache;
    private final NoTenantListHostStrategyService noTenantListHostStrategyService;

    @Autowired
    public NoTenantHostServiceImpl(NoTenantHostDAO noTenantHostDAO,
                                   ApplicationService applicationService,
                                   HostCache hostCache,
                                   NoTenantListHostStrategyService noTenantListHostStrategyService) {
        super(null);
        this.noTenantHostDAO = noTenantHostDAO;
        this.applicationService = applicationService;
        this.hostCache = hostCache;
        this.noTenantListHostStrategyService = noTenantListHostStrategyService;
    }

    @Override
    public List<ApplicationHostDTO> getHostsByAppId(Long appId) {
        ApplicationDTO applicationDTO = applicationService.getAppByAppId(appId);
        ResourceScope scope = applicationDTO.getScope();
        if (scope.getType() == ResourceScopeTypeEnum.BIZ) {
            return noTenantHostDAO.listHostInfoByBizId(Long.parseLong(scope.getId()));
        } else {
            return noTenantHostDAO.listHostInfoByBizIds(applicationDTO.getSubBizIds(), null, null);
        }
    }

    @Override
    public int batchInsertHosts(List<ApplicationHostDTO> insertList) {
        if (CollectionUtils.isEmpty(insertList)) {
            return 0;
        }
        StopWatch watch = new StopWatch();
        watch.start("batchInsertHost");
        // 批量插入主机
        int affectedNum = noTenantHostDAO.batchInsertHost(insertList);
        log.info("{} hosts inserted", affectedNum);
        insertList.forEach(hostCache::addOrUpdateHost);
        watch.stop();
        if (log.isDebugEnabled()) {
            log.debug("Performance:insertHosts:{}", watch.prettyPrint());
        }
        return affectedNum;
    }

    @Override
    public int batchUpdateHostsBeforeLastTime(List<ApplicationHostDTO> hostInfoList) {
        if (CollectionUtils.isEmpty(hostInfoList)) {
            return 0;
        }
        StopWatch watch = new StopWatch();
        watch.start("batchUpdateHostsBeforeLastTime to DB");
        // 批量更新主机
        int affectedNum = noTenantHostDAO.batchUpdateHostsBeforeLastTime(hostInfoList);
        log.info("try to update {} hosts, {} updated", hostInfoList.size(), affectedNum);
        watch.stop();
        watch.start("listHostInfoByHostIds");
        List<Long> hostIds = hostInfoList.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toList());
        hostInfoList = noTenantHostDAO.listHostInfoByHostIds(hostIds);
        watch.stop();
        watch.start("updateHostToCache");
        hostInfoList.forEach(hostCache::addOrUpdateHost);
        watch.stop();
        if (watch.getTotalTimeMillis() < 10_000L) {
            if (log.isDebugEnabled()) {
                log.debug("Performance:batchUpdateHostsBeforeLastTime:{}", watch.prettyPrint());
            }
        } else if (watch.getTotalTimeMillis() < 60_000L) {
            log.info("Performance:batchUpdateHostsBeforeLastTime:{}", watch.prettyPrint());
        } else {
            log.warn("Performance:batchUpdateHostsBeforeLastTime:{}", watch.prettyPrint());
        }
        return affectedNum;
    }

    @Override
    public int updateHostsStatus(List<HostSimpleDTO> simpleHostList) {
        StopWatch watch = new StopWatch();
        int updateCount = 0;
        try {
            if (!simpleHostList.isEmpty()) {
                watch.start("updateHostsStatus");
                // MySql5.7为例默认单条SQL最大为4M
                int batchSize = 5000;
                int size = simpleHostList.size();
                int start = 0;
                int end;
                do {
                    end = start + batchSize;
                    end = Math.min(end, size);
                    List<HostSimpleDTO> subList = simpleHostList.subList(start, end);
                    Map<Integer, List<Long>> agentAliveStatusGroupMap = subList.stream()
                        .collect(Collectors.groupingBy(HostSimpleDTO::getAgentAliveStatus,
                            Collectors.mapping(HostSimpleDTO::getHostId, Collectors.toList())));
                    for (Integer agentAliveStatus : agentAliveStatusGroupMap.keySet()) {
                        updateCount += noTenantHostDAO.batchUpdateHostStatusByHostIds(agentAliveStatus,
                            agentAliveStatusGroupMap.get(agentAliveStatus));
                    }
                    start += batchSize;
                } while (end < size);
                watch.stop();
                watch.start("updateHostsCache");
                deleteOrUpdateHostCache(simpleHostList);
                watch.stop();
            }
        } catch (Throwable throwable) {
            log.error(String.format("updateHostStatus fail：hostSize=%s", simpleHostList.size()), throwable);
        }
        if (watch.getTotalTimeMillis() > 180000) {
            log.info("updateHostsStatus too slow, run statistics:{}", watch.prettyPrint());
        }
        log.debug("Performance:updateHostsStatus:{}", watch);
        return updateCount;
    }

    private void deleteOrUpdateHostCache(List<HostSimpleDTO> simpleHostList) {
        if (CollectionUtils.isEmpty(simpleHostList)) {
            return;
        }
        int maxDeleteNum = 10000;
        if (simpleHostList.size() <= maxDeleteNum) {
            // 需要更新的主机数量较少，直接删缓存
            deleteHostCache(simpleHostList);
        } else {
            // 需要更新的主机数量较多，从DB加载最新数据到缓存，防止缓存穿透
            loadDBHostToCache(simpleHostList);
        }
    }

    private void deleteHostCache(List<HostSimpleDTO> simpleHostList) {
        Collection<ApplicationHostDTO> hosts = simpleHostList.stream()
            .map(HostSimpleDTO::convertToHostDTO)
            .collect(Collectors.toList());
        Collection<Long> hostIds = hosts.stream()
            .map(ApplicationHostDTO::getHostId)
            .collect(Collectors.toList());
        hostCache.batchDeleteHost(hosts);
        if (log.isDebugEnabled()) {
            log.debug(
                "{} hosts deleted from cache, hostIds:{}",
                hostIds.size(),
                hostIds
            );
        }
    }

    private void loadDBHostToCache(List<HostSimpleDTO> simpleHostList) {
        Collection<Long> hostIds = simpleHostList.stream()
            .map(HostSimpleDTO::getHostId)
            .collect(Collectors.toList());
        List<ApplicationHostDTO> hosts = noTenantHostDAO.listHostInfoByHostIds(hostIds);
        Collection<Long> existHostIds = hosts.stream()
            .map(ApplicationHostDTO::getHostId)
            .collect(Collectors.toList());
        hostCache.batchAddOrUpdateHosts(hosts);
        if (log.isDebugEnabled()) {
            log.debug(
                "{} hosts from db loaded to cache, hostIds:{}",
                existHostIds.size(),
                existHostIds
            );
        }
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public Pair<Boolean, Integer> createOrUpdateHostBeforeLastTime(ApplicationHostDTO hostInfoDTO) {
        boolean needToCreate = false;
        try {
            if (noTenantHostDAO.existAppHostInfoByHostId(hostInfoDTO.getHostId())) {
                // 只更新事件中的主机属性与agent状态
                int affectedNum = noTenantHostDAO.updateHostAttrsBeforeLastTime(hostInfoDTO);
                if (affectedNum == 0) {
                    ApplicationHostDTO hostInDB = noTenantHostDAO.getHostById(hostInfoDTO.getHostId());
                    if (hostInDB != null) {
                        log.info(
                            "Not update host, hostId={}, dbHostLastTime={}, currentHostLastTime={}",
                            hostInDB.getHostId(),
                            hostInDB.getLastTime(),
                            hostInfoDTO.getLastTime()
                        );
                    } else {
                        log.warn(
                            "Not update host, hostId={}, hostInDB not exists",
                            hostInfoDTO.getHostId()
                        );
                    }
                }
                return Pair.of(needToCreate, affectedNum);
            } else {
                needToCreate = true;
                hostInfoDTO.setBizId(JobConstants.PUBLIC_APP_ID);
                int affectedNum = noTenantHostDAO.insertHostWithoutTopo(hostInfoDTO);
                log.info("insert host: id={}, affectedNum={}", hostInfoDTO.getHostId(), affectedNum);
                return Pair.of(needToCreate, affectedNum);
            }
        } catch (Throwable t) {
            log.error("createOrUpdateHostBeforeLastTime fail", t);
            return Pair.of(needToCreate, 0);
        } finally {
            // 从拓扑表向主机表同步拓扑数据
            int affectedNum = noTenantHostDAO.syncHostTopo(hostInfoDTO.getHostId());
            log.info("hostTopo synced: hostId={}, affectedNum={}", hostInfoDTO.getHostId(), affectedNum);
            // 更新缓存
            updateDbHostToCache(hostInfoDTO.getHostId());
        }
    }

    public int updateHostAttrsByHostId(ApplicationHostDTO hostInfoDTO) {
        return noTenantHostDAO.updateHostAttrsByHostId(hostInfoDTO);
    }

    public int updateHostAttrsBeforeLastTime(ApplicationHostDTO hostInfoDTO) {
        return noTenantHostDAO.updateHostAttrsBeforeLastTime(hostInfoDTO);
    }

    @Override
    public int deleteHostBeforeOrEqualLastTime(ApplicationHostDTO hostInfoDTO) {
        int affectedRowNum = noTenantHostDAO.deleteHostBeforeOrEqualLastTime(
            null,
            hostInfoDTO.getHostId(),
            hostInfoDTO.getLastTime()
        );
        log.info(
            "{} host deleted, id={} ,ip={}",
            affectedRowNum,
            hostInfoDTO.getHostId(),
            hostInfoDTO.getIp()
        );
        if (affectedRowNum > 0) {
            hostCache.deleteHost(hostInfoDTO);
        }
        return affectedRowNum;
    }

    public void updateDbHostToCache(Long hostId) {
        ApplicationHostDTO hostInfoDTO = noTenantHostDAO.getHostById(hostId);
        if (hostInfoDTO.getBizId() != null && hostInfoDTO.getBizId() > 0) {
            // 只更新常规业务的主机到缓存
            if (applicationService.existBiz(hostInfoDTO.getBizId())) {
                hostCache.addOrUpdateHost(hostInfoDTO);
                log.info("host cache updated: hostId:{}", hostInfoDTO.getHostId());
            }
        }
    }

    @Override
    public List<BasicHostDTO> listAllBasicHost() {
        return noTenantHostDAO.listAllBasicHost();
    }

    @Override
    public int deleteByBasicHost(List<BasicHostDTO> basicHostList) {
        if (CollectionUtils.isEmpty(basicHostList)) {
            return 0;
        }
        List<Long> hostIdList = basicHostList.stream().map(BasicHostDTO::getHostId).collect(Collectors.toList());
        // 先查出主机信息用于更新缓存
        List<ApplicationHostDTO> hostList = noTenantHostDAO.listHostInfoByHostIds(hostIdList);
        // 从DB删除
        int deletedNum = noTenantHostDAO.deleteByBasicHost(basicHostList);
        // 删除缓存
        hostCache.batchDeleteHost(hostList);
        hostList = noTenantHostDAO.listHostInfoByHostIds(hostIdList);
        // 未成功从DB删除的主机重新加入缓存
        hostCache.batchAddOrUpdateHosts(hostList);
        return deletedNum;
    }

    @Override
    public List<ApplicationHostDTO> listHostsFromCacheOrDB(Collection<HostDTO> hosts) {
        List<ApplicationHostDTO> existHosts = new ArrayList<>();
        List<HostDTO> notExistHosts = new ArrayList<>();
        Pair<List<Long>, List<String>> pair = separateByHostIdOrCloudIp(hosts);
        List<Long> hostIds = pair.getLeft();
        List<String> cloudIps = pair.getRight();
        if (CollectionUtils.isNotEmpty(hostIds)) {
            NoTenantListHostStrategy<Long> strategy = noTenantListHostStrategyService.buildByIdsFromCacheOrDbStrategy();
            Pair<List<Long>, List<ApplicationHostDTO>> result = strategy.listHosts(hostIds);
            existHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistHostId -> notExistHosts.add(HostDTO.fromHostId(notExistHostId)));
            }
        }
        if (CollectionUtils.isNotEmpty(cloudIps)) {
            NoTenantListHostStrategy<String> strategy =
                noTenantListHostStrategyService.buildByIpsFromCacheOrDbStrategy();
            Pair<List<String>, List<ApplicationHostDTO>> result = strategy.listHosts(cloudIps);
            existHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistCloudIp -> notExistHosts.add(HostDTO.fromCloudIp(notExistCloudIp)));
            }
        }
        if (!notExistHosts.isEmpty()) {
            log.warn(
                "{} hosts not exist in cache or db:{}",
                notExistHosts.size(),
                notExistHosts.stream().map(HostDTO::toStringBasic).collect(Collectors.toList())
            );
        }
        return existHosts;
    }

}
