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
import com.tencent.bk.job.common.cc.model.result.HostProp;
import com.tencent.bk.job.common.cc.model.result.HostWithModules;
import com.tencent.bk.job.common.cc.model.result.ModuleProp;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.host.HostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final HostTopoDAO hostTopoDAO;
    private final HostService hostService;
    private final BizCmdbClient bizCmdbClient;

    @Autowired
    public HostSyncService(AppHostsUpdateHelper appHostsUpdateHelper,
                           ApplicationHostDAO applicationHostDAO,
                           HostTopoDAO hostTopoDAO,
                           HostService hostService,
                           BizCmdbClient bizCmdbClient) {
        this.appHostsUpdateHelper = appHostsUpdateHelper;
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostService = hostService;
        this.bizCmdbClient = bizCmdbClient;
    }

    private List<HostWithModules> getHostRelationsFromCmdb(ApplicationDTO bizApp) {
        List<CcInstanceDTO> ccInstanceDTOList = new ArrayList<>();
        ccInstanceDTOList.add(new CcInstanceDTO(CcNodeTypeEnum.BIZ.getType(), bizApp.getBizIdIfBizApp()));
        return bizCmdbClient.getHostRelationsByTopology(
            bizApp.getBizIdIfBizApp(),
            ccInstanceDTOList
        );
    }

    private List<ApplicationHostDTO> computeInsertHostList(
        Long bizId,
        Set<Long> localHostIds,
        List<HostProp> hostPropList,
        long cmdbHostsFetchTimeMills
    ) {
        List<Long> insertHostIdList = new ArrayList<>();
        List<ApplicationHostDTO> insertHostList = hostPropList.stream()
            .filter(hostProp -> !localHostIds.contains(hostProp.getHostId()))
            .map(hostProp -> {
                ApplicationHostDTO hostDTO = hostProp.toApplicationHostDTO();
                Long lastTime = hostDTO.getLastTime();
                if (lastTime == null || lastTime < 0) {
                    hostDTO.setLastTime(cmdbHostsFetchTimeMills);
                    log.warn(
                        "cmdbHostLastTime({}) is invalid, use cmdbHostsFetchTimeMills({}) for insert, host={}",
                        lastTime,
                        cmdbHostsFetchTimeMills,
                        hostDTO
                    );
                }
                hostDTO.setBizId(bizId);
                insertHostIdList.add(hostProp.getHostId());
                return hostDTO;
            }).collect(Collectors.toList());
        log.info(
            "bizId={}, insertHostIdList.size={}, insertHostIdList={}",
            bizId,
            insertHostIdList.size(),
            insertHostIdList
        );
        return insertHostList;
    }

    private List<ApplicationHostDTO> computeUpdateHostList(
        Long bizId,
        Set<Long> localHostIds,
        List<BasicHostDTO> localHosts,
        List<HostProp> hostPropList,
        long cmdbHostsFetchTimeMills
    ) {
        Map<Long, BasicHostDTO> localHostsIdMap = new HashMap<>();
        for (BasicHostDTO localHost : localHosts) {
            localHostsIdMap.put(localHost.getHostId(), localHost);
        }
        List<Long> updateHostIdList = new ArrayList<>();
        List<ApplicationHostDTO> updateHostList = hostPropList.stream().filter(hostProp ->
            needToUpdate(hostProp, localHostIds, localHostsIdMap, cmdbHostsFetchTimeMills)
        ).map(hostProp -> {
            ApplicationHostDTO hostDTO = hostProp.toApplicationHostDTO();
            Long lastTime = hostDTO.getLastTime();
            if (lastTime == null || lastTime < 0) {
                hostDTO.setLastTime(cmdbHostsFetchTimeMills);
                log.warn(
                    "cmdbHostLastTime({}) is invalid, use cmdbHostsFetchTimeMills({}) for update, host={}",
                    lastTime,
                    cmdbHostsFetchTimeMills,
                    hostDTO
                );
            }
            hostDTO.setBizId(bizId);
            updateHostIdList.add(hostProp.getHostId());
            return hostDTO;
        }).collect(Collectors.toList());
        log.info(
            "bizId={}, updateHostIdList.size={}, updateHostIdList={}",
            bizId,
            updateHostIdList.size(),
            updateHostIdList
        );
        return updateHostList;
    }

    /**
     * 判断主机信息是否需要更新
     *
     * @param hostProp                从CMDB获取的主机信息
     * @param localHostIds            本地主机ID列表
     * @param localHostsIdMap         本地主机ID与主机信息映射表
     * @param cmdbHostsFetchTimeMills CMDB数据获取时间
     * @return 本地库中主机信息是否需要更新
     */
    private boolean needToUpdate(HostProp hostProp,
                                 Set<Long> localHostIds,
                                 Map<Long, BasicHostDTO> localHostsIdMap,
                                 long cmdbHostsFetchTimeMills) {
        if (!localHostIds.contains(hostProp.getHostId())) {
            return false;
        }
        Long lastTime = null;
        if (StringUtils.isNotBlank(hostProp.getLastTime())) {
            lastTime = TimeUtil.parseIsoZonedTimeToMillis(hostProp.getLastTime());
        }
        if (lastTime == null || lastTime < 0) {
            lastTime = cmdbHostsFetchTimeMills;
            log.warn(
                "cmdbHostLastTime({}) is invalid, use cmdbHostsFetchTimeMills({}) to check whether update, host={}",
                hostProp.getLastTime(),
                cmdbHostsFetchTimeMills,
                hostProp
            );
        }
        BasicHostDTO localHost = localHostsIdMap.get(hostProp.getHostId());
        if (localHost.getLastTime() >= lastTime) {
            log.debug(
                "local host(hostId={}, lastTime={}) is not older than target host last time({}), ignore update",
                localHost.getHostId(),
                localHost.getLastTime(),
                cmdbHostsFetchTimeMills
            );
            return false;
        } else {
            log.info(
                "local host(hostId={}, lastTime={}) is older than target host last time({}), need to update",
                localHost.getHostId(),
                localHost.getLastTime(),
                cmdbHostsFetchTimeMills
            );
            return true;
        }
    }

    /**
     * 使用从CMDB获取的单个业务下的主机、主机关系去更新DB中的对应数据
     *
     * @param bizId                   CMDB业务ID
     * @param hostWithModulesList     主机与模块信息
     * @param cmdbHostsFetchTimeMills CMDB主机信息获取时间
     * @return CMDB主机ID集合
     */
    private Set<BasicHostDTO> refreshBizHostAndRelations(Long bizId,
                                                         List<HostWithModules> hostWithModulesList,
                                                         long cmdbHostsFetchTimeMills) {
        StopWatch watch = new StopWatch();
        // CMDB数据拆分
        Set<Long> cmdbHostIds = new HashSet<>();
        Set<BasicHostDTO> cmdbBasicHosts = new HashSet<>();
        List<HostProp> hostPropList = new ArrayList<>();
        for (HostWithModules hostWithModules : hostWithModulesList) {
            HostProp hostProp = hostWithModules.getHost();
            if (hostProp != null) {
                hostPropList.add(hostProp);
                cmdbHostIds.add(hostProp.getHostId());
                Long lastTimeMills = null;
                if (StringUtils.isNotBlank(hostProp.getLastTime())) {
                    lastTimeMills = TimeUtil.parseIsoZonedTimeToMillis(hostProp.getLastTime());
                }
                BasicHostDTO cmdbBasicHost = new BasicHostDTO(
                    hostProp.getHostId(),
                    lastTimeMills
                );
                cmdbBasicHosts.add(cmdbBasicHost);
            }
        }
        // 本地数据：主机
        List<BasicHostDTO> localHostList = applicationHostDAO.listBasicHostInfo(cmdbHostIds);
        Set<Long> localHostIds = localHostList.stream().map(BasicHostDTO::getHostId).collect(Collectors.toSet());

        logCmdbHostIds(watch, bizId, cmdbHostIds);
        logLocalHostIds(watch, bizId, localHostIds);

        // 本地数据：主机关系
        List<HostTopoDTO> localHostTopoList = hostTopoDAO.listHostTopoByHostIds(cmdbHostIds);

        // 对比CMDB数据与本地数据找出要新增的主机关系
        List<HostTopoDTO> insertHostTopoList = computeInsertHostTopoList(
            bizId,
            localHostTopoList,
            hostWithModulesList,
            cmdbHostsFetchTimeMills
        );

        // 对比CMDB数据与本地数据找出要更新的主机关系
        List<HostTopoDTO> updateHostTopoList = computeUpdateHostTopoList(
            bizId,
            localHostTopoList,
            hostWithModulesList,
            cmdbHostsFetchTimeMills
        );

        // 对比CMDB数据与本地数据找出要删除的主机关系
        List<HostTopoDTO> deleteHostTopoList = computeDeleteHostTopoList(
            bizId,
            localHostTopoList,
            hostWithModulesList,
            cmdbHostsFetchTimeMills
        );

        // 刷新主机关系数据
        Triple<Integer, Integer, Integer> refreshHostTopoResult = refreshHostTopos(
            insertHostTopoList,
            updateHostTopoList,
            deleteHostTopoList,
            watch
        );
        int insertedHostTopoNum = refreshHostTopoResult.getLeft();
        int updatedHostTopoNum = refreshHostTopoResult.getMiddle();
        int deletedHostTopoNum = refreshHostTopoResult.getRight();

        // 对比CMDB数据与本地数据找出要新增的主机
        List<ApplicationHostDTO> insertHostList = computeInsertHostList(
            bizId,
            localHostIds,
            hostPropList,
            cmdbHostsFetchTimeMills
        );

        // 对比CMDB数据与本地数据找出要更新的主机
        List<ApplicationHostDTO> updateHostList = computeUpdateHostList(
            bizId,
            localHostIds,
            localHostList,
            hostPropList,
            cmdbHostsFetchTimeMills
        );

        // 刷新主机数据
        Pair<Integer, Integer> refreshHostResult = refreshHosts(insertHostList, updateHostList, watch);
        int insertedHostNum = refreshHostResult.getLeft();
        int updatedHostNum = refreshHostResult.getRight();

        logRefreshResult(
            bizId,
            insertedHostTopoNum,
            updatedHostTopoNum,
            deletedHostTopoNum,
            insertedHostNum,
            updatedHostNum,
            watch
        );
        return cmdbBasicHosts;
    }

    /**
     * 根据本地主机拓扑数据与从CMDB获取的主机拓扑数据计算需要新增的拓扑数据
     *
     * @param bizId               业务ID
     * @param localHostTopoList   本地主机拓扑数据
     * @param hostWithModulesList 从CMDB获取的主机拓扑数据
     * @return 需要新增的主机拓扑数据
     */
    private List<HostTopoDTO> computeInsertHostTopoList(Long bizId,
                                                        List<HostTopoDTO> localHostTopoList,
                                                        List<HostWithModules> hostWithModulesList,
                                                        long cmdbHostsFetchTimeMills) {
        List<HostTopoDTO> insertHostTopoList = new ArrayList<>();
        Set<String> localTopoKeys = new HashSet<>();
        for (HostTopoDTO hostTopoDTO : localHostTopoList) {
            localTopoKeys.add(buildTopoKey(hostTopoDTO));
        }
        for (HostWithModules hostWithModules : hostWithModulesList) {
            HostProp host = hostWithModules.getHost();
            List<ModuleProp> modules = hostWithModules.getModules();
            if (CollectionUtils.isEmpty(modules)) {
                continue;
            }
            for (ModuleProp module : modules) {
                if (module == null) {
                    continue;
                }
                String topoKey = buildTopoKey(bizId, host, module);
                if (localTopoKeys.contains(topoKey)) {
                    continue;
                }
                HostTopoDTO hostTopo = buildHostTopo(bizId, host, module);
                Long lastTime = hostTopo.getLastTime();
                if (lastTime == null || lastTime < 0) {
                    hostTopo.setLastTime(cmdbHostsFetchTimeMills);
                    log.warn(
                        "cmdbHostTopoLastTime({}) is invalid, use cmdbHostsFetchTimeMills({}) for insert, hostTopo={}",
                        lastTime,
                        cmdbHostsFetchTimeMills,
                        hostTopo
                    );
                }
                insertHostTopoList.add(hostTopo);
            }
        }
        log.info(
            "bizId={}, insertHostTopoList.size={}, insertHostTopoList={}",
            bizId,
            insertHostTopoList.size(),
            insertHostTopoList
        );
        return insertHostTopoList;
    }

    /**
     * 根据本地主机拓扑数据与从CMDB获取的主机拓扑数据计算需要更新的拓扑数据
     *
     * @param bizId                   业务ID
     * @param localHostTopoList       本地主机拓扑数据
     * @param hostWithModulesList     从CMDB获取的主机拓扑数据
     * @param cmdbHostsFetchTimeMills CMDB主机信息获取时间
     * @return 需要更新的主机拓扑数据
     */
    private List<HostTopoDTO> computeUpdateHostTopoList(Long bizId,
                                                        List<HostTopoDTO> localHostTopoList,
                                                        List<HostWithModules> hostWithModulesList,
                                                        long cmdbHostsFetchTimeMills) {
        List<HostTopoDTO> updateHostTopoList = new ArrayList<>();
        Map<String, HostTopoDTO> localTopoMap = new HashMap<>();
        for (HostTopoDTO hostTopoDTO : localHostTopoList) {
            localTopoMap.put(buildTopoKey(hostTopoDTO), hostTopoDTO);
        }
        for (HostWithModules hostWithModules : hostWithModulesList) {
            HostProp host = hostWithModules.getHost();
            List<ModuleProp> modules = hostWithModules.getModules();
            if (CollectionUtils.isEmpty(modules)) {
                continue;
            }
            for (ModuleProp module : modules) {
                if (module == null) {
                    continue;
                }
                String topoKey = buildTopoKey(bizId, host, module);
                if (!localTopoMap.containsKey(topoKey)) {
                    continue;
                }
                HostTopoDTO localHostTopo = localTopoMap.get(topoKey);
                HostTopoDTO cmdbHostTopo = buildHostTopo(bizId, host, module);
                Long cmdbHostTopoLastTime = cmdbHostTopo.getLastTime();
                if (cmdbHostTopoLastTime == null || cmdbHostTopoLastTime < 0) {
                    cmdbHostTopoLastTime = cmdbHostsFetchTimeMills;
                    cmdbHostTopo.setLastTime(cmdbHostTopoLastTime);
                    log.warn(
                        "cmdbHostTopoLastTime({}) is invalid, use cmdbHostsFetchTimeMills({}) for update, " +
                            "cmdbHostTopo={}",
                        cmdbHostTopoLastTime,
                        cmdbHostsFetchTimeMills,
                        cmdbHostTopo
                    );
                }
                if (localHostTopo.getLastTime() < cmdbHostTopoLastTime) {
                    updateHostTopoList.add(cmdbHostTopo);
                    log.info(
                        "local hostTopo({}) is older than cmdb hostTopo time({}), need to update",
                        localHostTopo,
                        cmdbHostTopoLastTime
                    );
                } else {
                    log.debug(
                        "local hostTopo({}) is not older than cmdb hostTopo time({}), do not update",
                        localHostTopo,
                        cmdbHostTopoLastTime
                    );
                }
            }
        }
        logUpdateHostTopoList(bizId, updateHostTopoList);
        return updateHostTopoList;
    }

    private void logUpdateHostTopoList(Long bizId, List<HostTopoDTO> updateHostTopoList) {
        if (log.isDebugEnabled()) {
            log.debug(
                "bizId={}, updateHostTopoList.size={}, updateHostTopoList={}",
                bizId,
                updateHostTopoList.size(),
                updateHostTopoList
            );
            return;
        }
        if (updateHostTopoList.size() < 200) {
            log.info(
                "bizId={}, updateHostTopoList.size={}, updateHostTopoList={}",
                bizId,
                updateHostTopoList.size(),
                updateHostTopoList
            );
            return;
        }
        log.info(
            "bizId={}, updateHostTopoList.size={}",
            bizId,
            updateHostTopoList.size()
        );
    }

    /**
     * 根据本地主机拓扑数据与从CMDB获取的主机拓扑数据计算需要删除的拓扑数据
     *
     * @param bizId                   业务ID
     * @param localHostTopoList       本地主机拓扑数据
     * @param hostWithModulesList     从CMDB获取的主机拓扑数据
     * @param cmdbHostsFetchTimeMills CMDB主机信息获取时间
     * @return 需要更新的主机拓扑数据
     */
    private List<HostTopoDTO> computeDeleteHostTopoList(Long bizId,
                                                        List<HostTopoDTO> localHostTopoList,
                                                        List<HostWithModules> hostWithModulesList,
                                                        long cmdbHostsFetchTimeMills) {
        List<HostTopoDTO> deleteHostTopoList = new ArrayList<>();
        Set<String> cmdbHostTopoKeys = new HashSet<>();

        for (HostWithModules hostWithModules : hostWithModulesList) {
            HostProp host = hostWithModules.getHost();
            List<ModuleProp> modules = hostWithModules.getModules();
            if (CollectionUtils.isEmpty(modules)) {
                continue;
            }
            for (ModuleProp module : modules) {
                if (module == null) {
                    continue;
                }
                String topoKey = buildTopoKey(bizId, host, module);
                cmdbHostTopoKeys.add(topoKey);
            }
        }
        for (HostTopoDTO localHostTopo : localHostTopoList) {
            String topoKey = buildTopoKey(localHostTopo);
            if (cmdbHostTopoKeys.contains(topoKey)) {
                continue;
            }
            if (localHostTopo.getLastTime() < cmdbHostsFetchTimeMills) {
                deleteHostTopoList.add(localHostTopo);
                log.info(
                    "local hostTopo({}) is older than cmdb hostTopo fetch time({}), need to delete",
                    localHostTopo,
                    cmdbHostsFetchTimeMills
                );
            } else {
                log.info(
                    "local hostTopo({}) is not older than cmdb hostTopo fetch time({}), do not delete",
                    localHostTopo,
                    cmdbHostsFetchTimeMills
                );
            }
        }
        log.info(
            "bizId={}, deleteHostTopoList.size={}, deleteHostTopoList={}",
            bizId,
            deleteHostTopoList.size(),
            deleteHostTopoList
        );
        return deleteHostTopoList;
    }

    private Triple<Integer, Integer, Integer> refreshHostTopos(List<HostTopoDTO> insertHostTopoList,
                                                               List<HostTopoDTO> updateHostTopoList,
                                                               List<HostTopoDTO> deleteHostTopoList,
                                                               StopWatch watch) {
        int deletedNum = applyDeleteHostTopoList(deleteHostTopoList, watch);
        int insertedNum = applyInsertHostTopoList(insertHostTopoList, watch);
        int updatedNum = applyUpdateHostTopoList(updateHostTopoList, watch);
        return Triple.of(insertedNum, updatedNum, deletedNum);
    }

    private int applyDeleteHostTopoList(List<HostTopoDTO> deleteHostTopoList,
                                        StopWatch watch) {
        int deletedNum = 0;
        try {
            watch.start("batchDeleteWithLastTime");
            deletedNum = hostTopoDAO.batchDeleteWithLastTime(deleteHostTopoList);
            watch.stop();
        } catch (Exception e) {
            log.error("Fail to batchDeleteWithLastTime, try to delete one by one", e);
            watch.start("deleteHostTopoBeforeOrEqualLastTime one by one");
            for (HostTopoDTO hostTopoDTO : deleteHostTopoList) {
                deletedNum += tryToDeleteHostTopoBeforeOrEqualLastTime(hostTopoDTO);
            }
            watch.stop();
        }
        return deletedNum;
    }

    private int tryToDeleteHostTopoBeforeOrEqualLastTime(HostTopoDTO hostTopoDTO) {
        try {
            return hostTopoDAO.deleteHostTopoBeforeOrEqualLastTime(
                hostTopoDTO.getHostId(),
                hostTopoDTO.getBizId(),
                hostTopoDTO.getSetId(),
                hostTopoDTO.getModuleId(),
                hostTopoDTO.getLastTime()
            );
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to deleteHostTopoBeforeOrEqualLastTime, hostTopo={}",
                hostTopoDTO
            );
            log.error(msg.getMessage(), e);
            return 0;
        }
    }

    private int applyInsertHostTopoList(List<HostTopoDTO> insertHostTopoList,
                                        StopWatch watch) {
        int insertedNum = 0;
        try {
            watch.start("batchInsertHostTopo");
            insertedNum = hostTopoDAO.batchInsertHostTopo(insertHostTopoList);
            watch.stop();
        } catch (Exception e) {
            log.error("Fail to batchInsertHostTopo, try to insert one by one", e);
            watch.start("insertHostTopo one by one");
            for (HostTopoDTO hostTopoDTO : insertHostTopoList) {
                insertedNum += tryToInsertHostTopo(hostTopoDTO);
            }
            watch.stop();
        }
        return insertedNum;
    }

    private int tryToInsertHostTopo(HostTopoDTO hostTopoDTO) {
        try {
            return hostTopoDAO.insertHostTopo(hostTopoDTO);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to insertHostTopo, hostTopo={}",
                hostTopoDTO
            );
            log.error(msg.getMessage(), e);
            return 0;
        }
    }

    private int applyUpdateHostTopoList(List<HostTopoDTO> updateHostTopoList,
                                        StopWatch watch) {
        int updatedNum = 0;
        try {
            watch.start("batchUpdateBeforeLastTime");
            updatedNum = hostTopoDAO.batchUpdateBeforeLastTime(updateHostTopoList);
            watch.stop();
        } catch (Exception e) {
            log.error("Fail to batchUpdateBeforeLastTime, try to update one by one", e);
            watch.start("updateBeforeLastTime one by one");
            for (HostTopoDTO hostTopoDTO : updateHostTopoList) {
                updatedNum += tryToUpdateBeforeLastTime(hostTopoDTO);
            }
            watch.stop();
        }
        return updatedNum;
    }

    private int tryToUpdateBeforeLastTime(HostTopoDTO hostTopoDTO) {
        try {
            return hostTopoDAO.updateBeforeLastTime(hostTopoDTO);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to updateBeforeLastTime, hostTopo={}",
                hostTopoDTO
            );
            log.error(msg.getMessage(), e);
            return 0;
        }
    }

    private String buildTopoKey(HostTopoDTO hostTopoDTO) {
        String delimiter = "_";
        return hostTopoDTO.getHostId() +
            delimiter +
            hostTopoDTO.getBizId() +
            delimiter +
            hostTopoDTO.getSetId() +
            delimiter +
            hostTopoDTO.getModuleId();
    }

    private String buildTopoKey(Long bizId, HostProp host, ModuleProp moduleProp) {
        String delimiter = "_";
        return host.getHostId() +
            delimiter +
            bizId +
            delimiter +
            moduleProp.getSetId() +
            delimiter +
            moduleProp.getModuleId();
    }

    private HostTopoDTO buildHostTopo(Long bizId, HostProp host, ModuleProp moduleProp) {
        Long lastTimeMills = null;
        if (StringUtils.isNotBlank(moduleProp.getLastTime())) {
            lastTimeMills = TimeUtil.parseIsoZonedTimeToMillis(moduleProp.getLastTime());
        }
        return new HostTopoDTO(
            host.getHostId(),
            bizId,
            moduleProp.getSetId(),
            moduleProp.getModuleId(),
            lastTimeMills
        );
    }

    private Pair<Integer, Integer> refreshHosts(List<ApplicationHostDTO> insertHostList,
                                                List<ApplicationHostDTO> updateHostList,
                                                StopWatch watch) {
        int insertNum = applyInsertHostList(insertHostList, watch);
        int updateNum = applyUpdateHostList(updateHostList, watch);
        return Pair.of(insertNum, updateNum);
    }

    private int applyInsertHostList(List<ApplicationHostDTO> insertHostList,
                                    StopWatch watch) {
        int insertNum = 0;
        try {
            watch.start("batchInsertHosts");
            // 新增主机
            insertNum = hostService.batchInsertHosts(insertHostList);
            watch.stop();
        } catch (Exception e) {
            log.error("Fail to batchInsertHosts, try to insert one by one", e);
            watch.start("insertHosts one by one");
            for (ApplicationHostDTO hostDTO : insertHostList) {
                insertNum += tryToInsertOneHost(hostDTO);
            }
            watch.stop();
        }
        return insertNum;
    }

    private int tryToInsertOneHost(ApplicationHostDTO hostDTO) {
        try {
            Pair<Boolean, Integer> pair = hostService.createOrUpdateHostBeforeLastTime(hostDTO);
            return pair.getRight();
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format("Fail to insert host={}", hostDTO);
            log.error(msg.getMessage(), e);
            return 0;
        }
    }

    private int applyUpdateHostList(List<ApplicationHostDTO> updateHostList,
                                    StopWatch watch) {
        int updateNum = 0;
        try {
            watch.start("batchUpdateHostsBeforeLastTime");
            // 更新主机
            updateNum = hostService.batchUpdateHostsBeforeLastTime(updateHostList);
            watch.stop();
        } catch (Exception e) {
            log.error("Fail to batchUpdateHostsBeforeLastTime, try to update one by one", e);
            watch.start("updateHosts one by one");
            for (ApplicationHostDTO hostDTO : updateHostList) {
                updateNum += tryToUpdateOneHost(hostDTO);
            }
            watch.stop();
        }
        return updateNum;
    }

    private int tryToUpdateOneHost(ApplicationHostDTO hostDTO) {
        try {
            return hostService.updateHostAttrsBeforeLastTime(hostDTO);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to updateHostAttrsBeforeLastTime, host={}",
                hostDTO
            );
            log.error(msg.getMessage(), e);
            return 0;
        }
    }

    private void logRefreshResult(Long bizId,
                                  int insertedHostTopoNum,
                                  int updatedHostTopoNum,
                                  int deletedHostTopoNum,
                                  int insertedHostNum,
                                  int updatedHostNum,
                                  StopWatch watch) {
        if (watch.getTotalTimeMillis() > 300_000) {
            log.warn("Performance:refreshBizHostAndRelations: bizId={}, {}", bizId, watch.prettyPrint());
        } else if (watch.getTotalTimeMillis() > 50_000) {
            log.info("Performance:refreshBizHostAndRelations: bizId={}, {}", bizId, watch.prettyPrint());
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Performance:refreshBizHostAndRelations: bizId={}, {}", bizId, watch.prettyPrint());
            }
        }
        log.info(
            "RefreshBizHostAndRelationsStatistics:bizId={}, insertedHostTopoNum={}, " +
                "updatedHostTopoNum={}, deletedHostTopoNum={}, insertedHostNum={}, updatedHostNum={}",
            bizId,
            insertedHostTopoNum,
            updatedHostTopoNum,
            deletedHostTopoNum,
            insertedHostNum,
            updatedHostNum
        );
    }

    private void logCmdbHostIds(StopWatch watch, Long bizId, Collection<Long> cmdbHostIds) {
        watch.start("logCmdbHostIds");
        log.info(
            "bizId={}, cmdbHostIds.size={}, cmdbHostIds={}",
            bizId,
            cmdbHostIds.size(),
            StringUtil.concatCollection(cmdbHostIds)
        );
        watch.stop();
    }

    private void logLocalHostIds(StopWatch watch, Long bizId, Collection<Long> localHostIds) {
        watch.start("logLocalHostIds");
        log.info(
            "bizId={}, localHostIds.size={}, localHostIds={}",
            bizId,
            localHostIds.size(),
            StringUtil.concatCollection(localHostIds)
        );
        watch.stop();
    }

    private Triple<Set<BasicHostDTO>, Long, Long> syncBizHostsIndeed(ApplicationDTO bizApp) {
        Long bizId = Long.valueOf(bizApp.getScope().getId());
        long cmdbInterfaceTimeConsuming = 0L;
        long writeToDBTimeConsuming = 0L;
        StopWatch bizHostsWatch = new StopWatch();
        bizHostsWatch.start("getHostRelationsFromCmdb");
        long startTime = System.currentTimeMillis();
        log.info("begin to syncBizHosts:bizId={}", bizId);
        long cmdbHostsFetchTimeMills = System.currentTimeMillis();
        List<HostWithModules> hostRelationsFromCmdb = getHostRelationsFromCmdb(bizApp);
        cmdbInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        bizHostsWatch.stop();
        bizHostsWatch.start("refreshBizHostAndRelations to local DB");
        startTime = System.currentTimeMillis();
        Set<BasicHostDTO> cmdbBasicHosts = refreshBizHostAndRelations(
            bizId,
            hostRelationsFromCmdb,
            cmdbHostsFetchTimeMills
        );
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        bizHostsWatch.stop();
        if (bizHostsWatch.getTotalTimeMillis() < 600_000L) {
            log.info("Performance:syncBizHostAndRelations:bizId={},{}", bizId, bizHostsWatch);
        } else {
            log.warn("SLOW:Performance:syncBizHostAndRelations:bizId={},{}", bizId, bizHostsWatch);
        }
        return Triple.of(cmdbBasicHosts, cmdbInterfaceTimeConsuming, writeToDBTimeConsuming);
    }

    public Triple<Set<BasicHostDTO>, Long, Long> syncBizHostsAtOnce(ApplicationDTO bizApp) {
        Long bizId = Long.valueOf(bizApp.getScope().getId());
        try {
            appHostsUpdateHelper.waitAndStartBizHostsUpdating(bizId);
            return syncBizHostsIndeed(bizApp);
        } catch (Throwable t) {
            log.error("Fail to syncBizHosts of bizId " + bizId, t);
            return null;
        } finally {
            appHostsUpdateHelper.endToUpdateBizHosts();
        }
    }

    public void clearHostNotInCmdb(Set<BasicHostDTO> cmdbBasicHosts, long cmdbHostsFetchTimeMills) {
        // 删除主机拓扑
        List<HostTopoDTO> deleteHostTopoList = computeDeleteHostTopoList(cmdbBasicHosts, cmdbHostsFetchTimeMills);
        int deletedHostTopoNum = 0;
        if (!CollectionUtils.isEmpty(deleteHostTopoList)) {
            for (HostTopoDTO hostTopoDTO : deleteHostTopoList) {
                deletedHostTopoNum += hostTopoDAO.deleteHostTopoBeforeOrEqualLastTime(
                    hostTopoDTO.getHostId(),
                    hostTopoDTO.getBizId(),
                    hostTopoDTO.getSetId(),
                    hostTopoDTO.getModuleId(),
                    cmdbHostsFetchTimeMills
                );
            }
            log.info(
                "deleteHostTopoList.size={}, deletedHostTopoNum={}, deleteHostTopoList={}",
                deleteHostTopoList.size(),
                deletedHostTopoNum,
                deleteHostTopoList
            );
        }

        // 删除主机
        List<BasicHostDTO> deleteHostList = computeDeleteHostList(cmdbBasicHosts, cmdbHostsFetchTimeMills);
        if (!CollectionUtils.isEmpty(deleteHostList)) {
            int deletedHostNum = hostService.deleteByBasicHost(deleteHostList);
            log.info(
                "deleteHostList.size={}, deletedHostNum={}, deleteHostIdList={}",
                deleteHostList.size(),
                deletedHostNum,
                deleteHostList.stream().map(BasicHostDTO::getHostId).collect(Collectors.toList())
            );
        }
    }

    private List<HostTopoDTO> computeDeleteHostTopoList(
        Set<BasicHostDTO> cmdbBasicHosts,
        long cmdbHostsFetchTimeMills
    ) {
        Set<Long> cmdbHostIds = cmdbBasicHosts.stream().map(BasicHostDTO::getHostId).collect(Collectors.toSet());
        List<HostTopoDTO> hostTopoList = hostTopoDAO.listHostTopoByExcludeHostIds(cmdbHostIds);
        return hostTopoList.stream().filter(hostTopoDTO -> {
            if (hostTopoDTO.getLastTime() > cmdbHostsFetchTimeMills) {
                log.info(
                    "local hostTopo({}) is not older than cmdb hosts fetch time({}), do not delete",
                    hostTopoDTO,
                    cmdbHostsFetchTimeMills
                );
                return false;
            }
            log.info(
                "local hostTopo({}) is equal or older than cmdb hosts fetch time({}), need to delete",
                hostTopoDTO,
                cmdbHostsFetchTimeMills
            );
            return true;
        }).collect(Collectors.toList());
    }

    private List<BasicHostDTO> computeDeleteHostList(
        Set<BasicHostDTO> cmdbBasicHosts,
        long cmdbHostsFetchTimeMills
    ) {
        List<BasicHostDTO> localBasicHosts = hostService.listAllBasicHost();
        Map<Long, BasicHostDTO> cmdbBasicHostMap = new HashMap<>();
        for (BasicHostDTO cmdbBasicHost : cmdbBasicHosts) {
            cmdbBasicHostMap.put(cmdbBasicHost.getHostId(), cmdbBasicHost);
        }
        List<BasicHostDTO> deleteHostList = new ArrayList<>();
        for (BasicHostDTO localBasicHost : localBasicHosts) {
            if (cmdbBasicHostMap.containsKey(localBasicHost.getHostId())) {
                continue;
            }
            // 本地主机时间戳比从CMDB获取的主机要新，不删除
            if (localBasicHost.getLastTime() > cmdbHostsFetchTimeMills) {
                log.info(
                    "local host(hostId={}, lastTime={}) is not older than cmdb hosts fetch time({}), do not delete",
                    localBasicHost.getHostId(),
                    localBasicHost.getLastTime(),
                    cmdbHostsFetchTimeMills
                );
                continue;
            }
            log.info(
                "local host(hostId={}, lastTime={}) is equal or older than cmdb hosts fetch time({}), need to " +
                    "delete",
                localBasicHost.getHostId(),
                localBasicHost.getLastTime(),
                cmdbHostsFetchTimeMills
            );
            deleteHostList.add(localBasicHost);
        }
        return deleteHostList;
    }

}
