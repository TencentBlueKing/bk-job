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

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.model.db.CacheHostDO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.HostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
@Slf4j
@Service
public class HostServiceImpl implements HostService {

    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationService applicationService;
    private final HostCache hostCache;
    private final BizCmdbClient bizCmdbClient;

    @Autowired
    public HostServiceImpl(ApplicationHostDAO applicationHostDAO,
                           ApplicationService applicationService,
                           HostCache hostCache,
                           BizCmdbClient bizCmdbClient) {
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
        this.hostCache = hostCache;
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public List<ApplicationHostDTO> getHostsByAppId(Long appId) {
        ApplicationDTO applicationDTO = applicationService.getAppByAppId(appId);
        ResourceScope scope = applicationDTO.getScope();
        if (scope.getType() == ResourceScopeTypeEnum.BIZ) {
            return applicationHostDAO.listHostInfoByBizId(Long.parseLong(scope.getId()));
        } else {
            return applicationHostDAO.listHostInfoByBizIds(applicationDTO.getSubBizIds(), null, null);
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
        int affectedNum = applicationHostDAO.batchInsertHost(insertList);
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
        int affectedNum = applicationHostDAO.batchUpdateHostsBeforeLastTime(hostInfoList);
        log.info("try to update {} hosts, {} updated", hostInfoList.size(), affectedNum);
        watch.stop();
        watch.start("listHostInfoByHostIds");
        List<Long> hostIds = hostInfoList.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toList());
        hostInfoList = applicationHostDAO.listHostInfoByHostIds(hostIds);
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
                        updateCount += applicationHostDAO.batchUpdateHostStatusByHostIds(agentAliveStatus,
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
        List<ApplicationHostDTO> hosts = applicationHostDAO.listHostInfoByHostIds(hostIds);
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
            if (applicationHostDAO.existAppHostInfoByHostId(hostInfoDTO.getHostId())) {
                // 只更新事件中的主机属性与agent状态
                int affectedNum = applicationHostDAO.updateHostAttrsBeforeLastTime(hostInfoDTO);
                if (affectedNum == 0) {
                    ApplicationHostDTO hostInDB = applicationHostDAO.getHostById(hostInfoDTO.getHostId());
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
                int affectedNum = applicationHostDAO.insertHostWithoutTopo(hostInfoDTO);
                log.info("insert host: id={}, affectedNum={}", hostInfoDTO.getHostId(), affectedNum);
                return Pair.of(needToCreate, affectedNum);
            }
        } catch (Throwable t) {
            log.error("createOrUpdateHostBeforeLastTime fail", t);
            return Pair.of(needToCreate, 0);
        } finally {
            // 从拓扑表向主机表同步拓扑数据
            int affectedNum = applicationHostDAO.syncHostTopo(hostInfoDTO.getHostId());
            log.info("hostTopo synced: hostId={}, affectedNum={}", hostInfoDTO.getHostId(), affectedNum);
            // 更新缓存
            updateDbHostToCache(hostInfoDTO.getHostId());
        }
    }

    public int updateHostAttrsByHostId(ApplicationHostDTO hostInfoDTO) {
        return applicationHostDAO.updateHostAttrsByHostId(hostInfoDTO);
    }

    public int updateHostAttrsBeforeLastTime(ApplicationHostDTO hostInfoDTO) {
        return applicationHostDAO.updateHostAttrsBeforeLastTime(hostInfoDTO);
    }

    @Override
    public int deleteHostBeforeOrEqualLastTime(ApplicationHostDTO hostInfoDTO) {
        int affectedRowNum = applicationHostDAO.deleteHostBeforeOrEqualLastTime(
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
        ApplicationHostDTO hostInfoDTO = applicationHostDAO.getHostById(hostId);
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
        return applicationHostDAO.listAllBasicHost();
    }

    @Override
    public int deleteByBasicHost(List<BasicHostDTO> basicHostList) {
        if (CollectionUtils.isEmpty(basicHostList)) {
            return 0;
        }
        List<Long> hostIdList = basicHostList.stream().map(BasicHostDTO::getHostId).collect(Collectors.toList());
        // 先查出主机信息用于更新缓存
        List<ApplicationHostDTO> hostList = applicationHostDAO.listHostInfoByHostIds(hostIdList);
        // 从DB删除
        int deletedNum = applicationHostDAO.deleteByBasicHost(basicHostList);
        // 删除缓存
        hostCache.batchDeleteHost(hostList);
        hostList = applicationHostDAO.listHostInfoByHostIds(hostIdList);
        // 未成功从DB删除的主机重新加入缓存
        hostCache.batchAddOrUpdateHosts(hostList);
        return deletedNum;
    }

    @Override
    public long countHostsByOsType(String osType) {
        return applicationHostDAO.countHostsByOsType(osType);
    }

    @Override
    public Map<String, Integer> groupHostByOsType() {
        return applicationHostDAO.groupHostByOsType();
    }

    @Override
    public List<List<InstanceTopologyDTO>> queryBizNodePaths(String username,
                                                             Long bizId,
                                                             List<InstanceTopologyDTO> nodeList) {
        // 查业务拓扑树
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(bizId);
        // 搜索路径
        return TopologyHelper.findTopoPaths(appTopologyTree, nodeList);
    }

    private List<Long> buildIncludeBizIdList(ApplicationDTO application) {
        List<Long> bizIdList = new ArrayList<>();
        if (application.isBiz()) {
            bizIdList.add(application.getBizIdIfBizApp());
        } else if (application.isBizSet()) {
            if (application.getSubBizIds() != null) {
                bizIdList.addAll(application.getSubBizIds());
            }
        }
        return bizIdList;
    }

    public List<ApplicationHostDTO> listHosts(Collection<HostDTO> hosts) {
        Pair<List<HostDTO>, List<ApplicationHostDTO>> hostResult = listHostsFromCacheOrCmdb(hosts);
        if (CollectionUtils.isEmpty(hostResult.getRight())) {
            return Collections.emptyList();
        }
        return hostResult.getRight();
    }

    public List<ApplicationHostDTO> listHostsByCloudIpv6(Long cloudAreaId, String ipv6) {
        List<ApplicationHostDTO> hosts = applicationHostDAO.listHostInfoByCloudIpv6(cloudAreaId, ipv6);
        // 多个Ipv6中的具体一个与查询条件Ipv6相等才有效
        hosts = filterHostsByCloudIpv6(hosts, cloudAreaId, ipv6);
        if (CollectionUtils.isEmpty(hosts)) {
            hosts = bizCmdbClient.listHostsByCloudIpv6s(Collections.singletonList(cloudAreaId + IpUtils.COLON + ipv6));
            hosts = filterHostsByCloudIpv6(hosts, cloudAreaId, ipv6);
        }
        return hosts;
    }

    /**
     * 筛选出与指定云区域ID、Ipv6地址匹配的主机
     * 指定的Ipv6地址必须与主机多个Ipv6地址的其中一个精确匹配
     *
     * @param hosts       筛选前的主机列表
     * @param cloudAreaId 云区域ID
     * @param ipv6        Ipv6地址
     * @return 筛选后的主机列表
     */
    private List<ApplicationHostDTO> filterHostsByCloudIpv6(List<ApplicationHostDTO> hosts,
                                                            Long cloudAreaId,
                                                            String ipv6) {
        return hosts.stream().filter(host -> {
            String multiIpv6 = host.getIpv6();
            Set<String> ipv6s = Arrays.stream(multiIpv6.split("[,;]"))
                .map(String::trim)
                .collect(Collectors.toSet());
            return host.getCloudAreaId().equals(cloudAreaId) && ipv6s.contains(ipv6);
        }).collect(Collectors.toList());
    }

    @Override
    public ServiceListAppHostResultDTO listAppHostsPreferCache(Long appId,
                                                               List<HostDTO> hosts,
                                                               boolean refreshAgentId) {
        StopWatch watch = new StopWatch("listAppHostsPreferCache");
        try {
            ServiceListAppHostResultDTO result = new ServiceListAppHostResultDTO();

            watch.start("getAppByAppId");
            ApplicationDTO application = applicationService.getAppByAppId(appId);
            watch.stop();

            watch.start("listHostsFromCacheOrCmdb");
            Pair<List<HostDTO>, List<ApplicationHostDTO>> hostResult = listHostsFromCacheOrCmdb(hosts);
            watch.stop();

            List<HostDTO> notExistHosts = hostResult.getLeft();
            List<ApplicationHostDTO> existHosts = hostResult.getRight();

            watch.start("refreshHostAgentIdIfNeed");
            refreshHostAgentIdIfNeed(refreshAgentId, existHosts);
            watch.stop();

            List<HostDTO> validHosts = new ArrayList<>();
            List<HostDTO> notInAppHosts = new ArrayList<>();

            if (application.isAllBizSet()) {
                // 如果是全业务，所以主机都是合法的
                result.setNotExistHosts(notExistHosts);
                result.setValidHosts(existHosts.stream().map(ApplicationHostDTO::toHostDTO)
                    .collect(Collectors.toList()));
                result.setNotInAppHosts(Collections.emptyList());
                return result;
            }

            // 普通业务集和普通业务需要判断主机是否归属于业务
            List<Long> includeBizIds = buildIncludeBizIdList(application);
            if (CollectionUtils.isEmpty(includeBizIds)) {
                log.warn("App do not contains any biz, appId:{}", appId);
                result.setValidHosts(Collections.emptyList());
                result.setNotExistHosts(Collections.emptyList());
                result.setNotInAppHosts(hosts);
                return result;
            }

            existHosts.forEach(existHost -> {
                if (includeBizIds.contains(existHost.getBizId())) {
                    validHosts.add(existHost.toHostDTO());
                } else {
                    notInAppHosts.add(existHost.toHostDTO());
                }
            });

            // 对于判定为其他业务下的主机，可能是缓存数据不准确导致，需要根据CMDB实时数据进行二次判定
            if (CollectionUtils.isNotEmpty(notInAppHosts)) {
                watch.start("reConfirmNotInAppHostsByCmdb");
                reConfirmNotInAppHostsByCmdb(notInAppHosts, notExistHosts, validHosts, appId, includeBizIds);
                watch.stop();
            }

            if (CollectionUtils.isNotEmpty(notExistHosts) || CollectionUtils.isNotEmpty(notInAppHosts)) {
                log.info("Contains invalid hosts, appId: {}, notExistHosts: {}, hostsInOtherApp: {}",
                    appId, notExistHosts, notInAppHosts);
            }

            result.setNotExistHosts(notExistHosts);
            result.setValidHosts(validHosts);
            result.setNotInAppHosts(notInAppHosts);

            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 3000) {
                log.warn("ListAppHostsPreferCache slow, watch: {}", watch.prettyPrint());
            }
        }

    }

    /**
     * 对于判定为其他业务下的主机，可能是缓存数据不准确导致，根据CMDB实时数据进行二次判定
     *
     * @param notInAppHosts 前期判定为在其他业务下的主机，在该方法中数据可能被修改
     * @param notExistHosts 前期判定为不存在的主机，在该方法中数据可能被修改
     * @param validHosts    前期判定为在业务下的主机，在该方法中数据可能被修改
     * @param appId         Job内业务ID
     * @param includeBizIds Job内业务ID可能对应的多个CMDB业务ID列表
     */
    private void reConfirmNotInAppHostsByCmdb(List<HostDTO> notInAppHosts,
                                              List<HostDTO> notExistHosts,
                                              List<HostDTO> validHosts,
                                              Long appId,
                                              List<Long> includeBizIds) {
        Pair<List<HostDTO>, List<ApplicationHostDTO>> cmdbHostsPair = listHostsFromCmdb(notInAppHosts);
        if (CollectionUtils.isNotEmpty(cmdbHostsPair.getLeft())) {
            notExistHosts.addAll(cmdbHostsPair.getLeft());
        }
        List<ApplicationHostDTO> cmdbExistHosts = cmdbHostsPair.getRight();
        if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
            notInAppHosts.clear();
            List<ApplicationHostDTO> cmdbValidHosts = new ArrayList<>();
            cmdbExistHosts.forEach(existHost -> {
                if (includeBizIds.contains(existHost.getBizId())) {
                    validHosts.add(existHost.toHostDTO());
                    cmdbValidHosts.add(existHost);
                } else {
                    notInAppHosts.add(existHost.toHostDTO());
                }
            });
            if (!cmdbValidHosts.isEmpty()) {
                log.info(
                    "{} hosts belong to appId {} after check in cmdb, cmdbValidHosts={}",
                    cmdbValidHosts.size(),
                    appId,
                    cmdbValidHosts
                );
            }
        }
    }

    private void refreshHostAgentIdIfNeed(boolean needRefreshAgentId, List<ApplicationHostDTO> hosts) {
        // 如果Job缓存的主机中没有agentId，那么需要从cmdb实时获取（解决一些特殊场景，比如节点管理Agent插件安装，bk_agent_id更新事件还没被处理的场景
        if (!needRefreshAgentId || CollectionUtils.isEmpty(hosts)) {
            return;
        }
        long start = System.currentTimeMillis();
        List<Long> missingAgentIdHostIds = hosts.stream()
            .filter(host -> StringUtils.isEmpty(host.getAgentId()))
            .map(ApplicationHostDTO::getHostId)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(missingAgentIdHostIds)) {
            return;
        }
        List<ApplicationHostDTO> cmdbHosts = listHostsFromCmdbByHostIds(missingAgentIdHostIds);
        if (CollectionUtils.isEmpty(cmdbHosts)) {
            log.warn("Refresh host agent id, hosts are not exist in cmdb! hosts: {}", missingAgentIdHostIds);
            return;
        }

        Map<Long, String> hostIdAndAgentIdMap = cmdbHosts.stream()
            .filter(host -> StringUtils.isNotEmpty(host.getAgentId()))
            .collect(Collectors.toMap(ApplicationHostDTO::getHostId,
                ApplicationHostDTO::getAgentId, (oldValue, newValue) -> newValue));
        hosts.forEach(host -> {
            if (StringUtils.isEmpty(host.getAgentId())) {
                host.setAgentId(hostIdAndAgentIdMap.get(host.getHostId()));
            }
        });
        log.info("Refresh host agent id, hostIds: {}, hostIdAndAgentIdMap: {}, cost: {}",
            missingAgentIdHostIds, hostIdAndAgentIdMap, System.currentTimeMillis() - start);
    }

    private Pair<List<HostDTO>, List<ApplicationHostDTO>> listHostsFromCmdb(Collection<HostDTO> hosts) {
        List<HostDTO> notExistHosts = new ArrayList<>();
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        Pair<List<Long>, List<String>> pair = separateByHostIdOrCloudIp(hosts);
        List<Long> hostIds = pair.getLeft();
        List<String> cloudIps = pair.getRight();
        if (CollectionUtils.isNotEmpty(hostIds)) {
            Pair<List<Long>, List<ApplicationHostDTO>> result =
                new ListHostByHostIdsStrategy().listHostsFromCmdb(hostIds);
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistHostId -> notExistHosts.add(HostDTO.fromHostId(notExistHostId)));
            }
        }
        if (CollectionUtils.isNotEmpty(cloudIps)) {
            Pair<List<String>, List<ApplicationHostDTO>> result =
                new ListHostByIpsStrategy().listHostsFromCmdb(cloudIps);
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistCloudIp -> notExistHosts.add(HostDTO.fromCloudIp(notExistCloudIp)));
            }
        }
        return Pair.of(notExistHosts, appHosts);
    }

    private Pair<List<Long>, List<String>> separateByHostIdOrCloudIp(Collection<HostDTO> hosts) {
        List<Long> hostIds = new ArrayList<>();
        List<String> cloudIps = new ArrayList<>();
        for (HostDTO host : hosts) {
            if (host.getHostId() != null) {
                hostIds.add(host.getHostId());
            } else {
                cloudIps.add(host.toCloudIp());
            }
        }
        return Pair.of(hostIds, cloudIps);
    }

    private Pair<List<HostDTO>, List<ApplicationHostDTO>> listHostsFromCacheOrCmdb(Collection<HostDTO> hosts) {
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        List<HostDTO> notExistHosts = new ArrayList<>();
        Pair<List<Long>, List<String>> pair = separateByHostIdOrCloudIp(hosts);
        List<Long> hostIds = pair.getLeft();
        List<String> cloudIps = pair.getRight();
        if (CollectionUtils.isNotEmpty(hostIds)) {
            Pair<List<Long>, List<ApplicationHostDTO>> result = listHostsByStrategy(hostIds,
                new ListHostByHostIdsStrategy());
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistHostId -> notExistHosts.add(HostDTO.fromHostId(notExistHostId)));
            }
        }
        if (CollectionUtils.isNotEmpty(cloudIps)) {
            Pair<List<String>, List<ApplicationHostDTO>> result = listHostsByStrategy(cloudIps,
                new ListHostByIpsStrategy());
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistCloudIp -> notExistHosts.add(HostDTO.fromCloudIp(notExistCloudIp)));
            }
        }
        return Pair.of(notExistHosts, appHosts);
    }

    private <K> Pair<List<K>, List<ApplicationHostDTO>> listHostsByStrategy(List<K> keys,
                                                                            ListHostStrategy<K> listHostStrategy) {
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        List<K> notExistKeys = null;

        if (CollectionUtils.isNotEmpty(keys)) {
            Pair<List<K>, List<ApplicationHostDTO>> result = listHostStrategy.listHostsFromCache(keys);
            notExistKeys = result.getLeft();
            if (CollectionUtils.isNotEmpty(result.getRight())) {
                appHosts.addAll(result.getRight());
            }
        }

        if (CollectionUtils.isNotEmpty(notExistKeys)) {
            Pair<List<K>, List<ApplicationHostDTO>> result = listHostStrategy.listHostsFromDb(notExistKeys);
            notExistKeys = result.getLeft();
            if (CollectionUtils.isNotEmpty(result.getRight())) {
                appHosts.addAll(result.getRight());
            }
        }

        if (CollectionUtils.isNotEmpty(notExistKeys)) {
            Pair<List<K>, List<ApplicationHostDTO>> result = listHostStrategy.listHostsFromCmdb(notExistKeys);
            notExistKeys = result.getLeft();
            if (CollectionUtils.isNotEmpty(result.getRight())) {
                appHosts.addAll(result.getRight());
            }
        }

        return Pair.of(notExistKeys, appHosts);
    }

    /**
     * 主机查询策略
     *
     * @param <K> 查询使用的主机KEY
     */
    private interface ListHostStrategy<K> {
        Pair<List<K>, List<ApplicationHostDTO>> listHostsFromCache(List<K> keys);

        Pair<List<K>, List<ApplicationHostDTO>> listHostsFromDb(List<K> keys);

        Pair<List<K>, List<ApplicationHostDTO>> listHostsFromCmdb(List<K> keys);
    }

    /**
     * 根据ip查询主机
     */
    private class ListHostByIpsStrategy implements ListHostStrategy<String> {
        @Override
        public Pair<List<String>, List<ApplicationHostDTO>> listHostsFromCache(List<String> cloudIps) {
            long start = System.currentTimeMillis();
            List<ApplicationHostDTO> appHosts = new ArrayList<>();
            List<String> notExistCloudIps = new ArrayList<>();
            List<CacheHostDO> cacheHosts = hostCache.batchGetHostsByIps(cloudIps);
            for (int i = 0; i < cloudIps.size(); i++) {
                String cloudIp = cloudIps.get(i);
                CacheHostDO cacheHost = cacheHosts.get(i);
                if (cacheHost != null) {
                    appHosts.add(cacheHost.toApplicationHostDTO());
                } else {
                    notExistCloudIps.add(cloudIp);
                }
            }

            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("ListHostsFromRedis slow, ipSize: {}, cost: {}", cloudIps.size(), cost);
            }
            return Pair.of(notExistCloudIps, appHosts);
        }

        @Override
        public Pair<List<String>, List<ApplicationHostDTO>> listHostsFromDb(List<String> cloudIps) {
            long start = System.currentTimeMillis();
            List<ApplicationHostDTO> appHosts = new ArrayList<>();
            List<String> notExistCloudIps = new ArrayList<>(cloudIps);
            List<ApplicationHostDTO> hostsInDb = applicationHostDAO.listHostsByCloudIps(cloudIps);
            if (CollectionUtils.isNotEmpty(hostsInDb)) {
                for (ApplicationHostDTO appHost : hostsInDb) {
                    if (appHost.getBizId() == null || appHost.getBizId() <= 0) {
                        log.info("Host: {}|{} missing bizId, skip!", appHost.getHostId(), appHost.getCloudIp());
                        // DB中缓存的主机可能没有业务信息(依赖的主机事件还没有处理),那么暂时跳过该主机
                        continue;
                    }
                    notExistCloudIps.remove(appHost.getCloudIp());
                    appHosts.add(appHost);
                }
            }
            if (CollectionUtils.isNotEmpty(appHosts)) {
                hostCache.batchAddOrUpdateHosts(appHosts);
            }

            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("ListHostsFromMySQL slow, ipSize: {}, cost: {}", cloudIps.size(), cost);
            }
            return Pair.of(notExistCloudIps, appHosts);
        }

        @Override
        public Pair<List<String>, List<ApplicationHostDTO>> listHostsFromCmdb(List<String> cloudIps) {
            long start = System.currentTimeMillis();

            List<String> notExistCloudIps = new ArrayList<>(cloudIps);

            List<ApplicationHostDTO> cmdbExistHosts = bizCmdbClient.listHostsByCloudIps(cloudIps);
            if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
                List<String> cmdbExistHostIds = cmdbExistHosts.stream()
                    .map(ApplicationHostDTO::getCloudIp)
                    .collect(Collectors.toList());
                notExistCloudIps.removeAll(cmdbExistHostIds);
                log.info("sync new hosts from cmdb, hosts:{}", cmdbExistHosts);

                hostCache.batchAddOrUpdateHosts(cmdbExistHosts);
            }

            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("ListHostsFromCmdb slow, ipSize: {}, cost: {}", cloudIps.size(), cost);
            }
            return Pair.of(notExistCloudIps, cmdbExistHosts);
        }
    }

    private class ListHostByHostIdsStrategy implements ListHostStrategy<Long> {
        @Override
        public Pair<List<Long>, List<ApplicationHostDTO>> listHostsFromCache(List<Long> hostIds) {
            long start = System.currentTimeMillis();
            List<ApplicationHostDTO> appHosts = new ArrayList<>();
            List<Long> notExistHostIds = new ArrayList<>();
            List<CacheHostDO> cacheHosts = hostCache.batchGetHostsByHostIds(hostIds);
            for (int i = 0; i < hostIds.size(); i++) {
                long hostId = hostIds.get(i);
                CacheHostDO cacheHost = cacheHosts.get(i);
                if (cacheHost != null) {
                    appHosts.add(cacheHost.toApplicationHostDTO());
                } else {
                    notExistHostIds.add(hostId);
                }
            }
            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("ListHostsFromRedis slow, hostSize: {}, cost: {}", hostIds.size(), cost);
            }
            return Pair.of(notExistHostIds, appHosts);
        }

        @Override
        public Pair<List<Long>, List<ApplicationHostDTO>> listHostsFromDb(List<Long> hostIds) {
            long start = System.currentTimeMillis();
            List<ApplicationHostDTO> appHosts = new ArrayList<>();
            List<Long> notExistHostIds = new ArrayList<>(hostIds);
            List<ApplicationHostDTO> hostsInDb = applicationHostDAO.listHostInfoByHostIds(hostIds);
            if (CollectionUtils.isNotEmpty(hostsInDb)) {
                for (ApplicationHostDTO appHost : hostsInDb) {
                    if (appHost.getBizId() == null || appHost.getBizId() <= 0) {
                        log.info("Host: {}|{} missing bizId, skip!", appHost.getHostId(), appHost.getCloudIp());
                        // DB中缓存的主机可能没有业务信息(依赖的主机事件还没有处理),那么暂时跳过该主机
                        continue;
                    }
                    notExistHostIds.remove(appHost.getHostId());
                    appHosts.add(appHost);
                }
            }

            if (CollectionUtils.isNotEmpty(appHosts)) {
                hostCache.batchAddOrUpdateHosts(appHosts);
            }
            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("ListHostsFromMySQL slow, hostSize: {}, cost: {}", hostIds.size(), cost);
            }
            return Pair.of(notExistHostIds, appHosts);
        }

        @Override
        public Pair<List<Long>, List<ApplicationHostDTO>> listHostsFromCmdb(List<Long> hostIds) {
            long start = System.currentTimeMillis();
            List<Long> notExistHostIds = new ArrayList<>(hostIds);
            List<ApplicationHostDTO> cmdbExistHosts = listHostsFromCmdbByHostIds(hostIds);
            if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
                List<Long> cmdbExistHostIds = cmdbExistHosts.stream()
                    .map(ApplicationHostDTO::getHostId)
                    .collect(Collectors.toList());
                notExistHostIds.removeAll(cmdbExistHostIds);
                log.info("sync new hosts from cmdb, hosts:{}", cmdbExistHosts);

                hostCache.batchAddOrUpdateHosts(cmdbExistHosts);
            }

            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("ListHostsFromCmdb slow, hostSize: {}, cost: {}", hostIds.size(), cost);
            }
            return Pair.of(notExistHostIds, cmdbExistHosts);
        }
    }

    @Override
    public Map<Long, ApplicationHostDTO> listHostsByHostIds(Collection<Long> hostIds) {
        Pair<List<Long>, List<ApplicationHostDTO>> result = listHostsByStrategy(new ArrayList<>(hostIds),
            new ListHostByHostIdsStrategy());
        return result.getRight().stream().collect(
            Collectors.toMap(ApplicationHostDTO::getHostId, host -> host, (oldValue, newValue) -> newValue));
    }

    /**
     * 从cmdb实时查询主机
     *
     * @param hostIds 主机ID列表
     * @return 主机 Map<hostId, host>
     */
    @Override
    public List<ApplicationHostDTO> listHostsFromCmdbByHostIds(List<Long> hostIds) {
        return bizCmdbClient.listHostsByHostIds(hostIds);
    }

    @Override
    public ApplicationHostDTO getHostByIp(String cloudIp) {
        Pair<List<String>, List<ApplicationHostDTO>> result = listHostsByStrategy(Collections.singletonList(cloudIp),
            new ListHostByIpsStrategy());
        return CollectionUtils.isNotEmpty(result.getRight()) ? result.getRight().get(0) : null;
    }

    @Override
    public Map<String, ApplicationHostDTO> listHostsByIps(Collection<String> cloudIps) {
        Pair<List<String>, List<ApplicationHostDTO>> result = listHostsByStrategy(new ArrayList<>(cloudIps),
            new ListHostByIpsStrategy());
        return result.getRight().stream().collect(
            Collectors.toMap(ApplicationHostDTO::getCloudIp, host -> host, (oldValue, newValue) -> newValue));
    }
}
