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

package com.tencent.bk.job.manage.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPRecordDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhiteIPServiceImpl implements WhiteIPService {
    private static final String SEPERATOR_COMMA = ",";
    private final MessageI18nService i18nService;
    private final ActionScopeDAO actionScopeDAO;
    private final WhiteIPRecordDAO whiteIPRecordDAO;
    private final ApplicationDAO applicationDAO;
    private final AppScopeMappingService appScopeMappingService;
    private final ApplicationHostDAO applicationHostDAO;
    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public WhiteIPServiceImpl(
        ActionScopeDAO actionScopeDAO,
        WhiteIPRecordDAO whiteIPRecordDAO,
        ApplicationDAO applicationDAO,
        MessageI18nService i18nService,
        AppScopeMappingService appScopeMappingService,
        ApplicationHostDAO applicationHostDAO,
        IBizCmdbClient bizCmdbClient) {
        this.actionScopeDAO = actionScopeDAO;
        this.whiteIPRecordDAO = whiteIPRecordDAO;
        this.applicationDAO = applicationDAO;
        this.i18nService = i18nService;
        this.appScopeMappingService = appScopeMappingService;
        this.applicationHostDAO = applicationHostDAO;
        this.bizCmdbClient = bizCmdbClient;
    }

    /**
     * 获取设置白名单IP后可对当前业务生效的appId列表
     *
     * @param appId Job业务Id
     * @return 设置白名单IP后可对当前业务生效的appId列表
     */
    private List<Long> getEffectiveAppIdList(Long appId) {
        List<Long> effectiveAppIds = new ArrayList<>();
        effectiveAppIds.add(JobConstants.DEFAULT_ALL_BIZ_SET_ID);
        effectiveAppIds.add(JobConstants.PUBLIC_APP_ID);
        if (appId != null
            && !appId.equals(JobConstants.DEFAULT_ALL_BIZ_SET_ID)
            && !appId.equals(JobConstants.PUBLIC_APP_ID)) {
            effectiveAppIds.add(appId);
        }
        return effectiveAppIds;
    }

    @Override
    public PageData<WhiteIPRecordVO> listWhiteIPRecord(
        String username,
        String ipStr,
        String appIdStr,
        String appNameStr,
        String actionScopeIdStr,
        String creator,
        String lastModifyUser,
        Integer start,
        Integer pageSize,
        String orderField,
        Integer order
    ) {
        log.info(
            LogUtil.getInputLog(
                username, ipStr, appIdStr,
                actionScopeIdStr, lastModifyUser, start,
                pageSize, orderField, order
            )
        );
        //1.参数校验、预处理
        if (null == ipStr) {
            ipStr = "";
        }
        List<Long> appIdList = StringUtil.strToList(appIdStr, Long.class, SEPERATOR_COMMA);
        List<Long> actionScopeIdList = StringUtil.strToList(actionScopeIdStr, Long.class, SEPERATOR_COMMA);
        List<String> appNameList = StringUtil.strToList(appNameStr, String.class, SEPERATOR_COMMA);
        List<String> ipList = StringUtil.strToList(ipStr, String.class, SEPERATOR_COMMA);
        if (start == null || start < 0) {
            start = 0;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        baseSearchCondition.setOrderField(orderField);
        baseSearchCondition.setOrder(order);

        val whiteIPRecordList = whiteIPRecordDAO.listWhiteIPRecord(
            ipList,
            appIdList,
            appNameList,
            actionScopeIdList,
            creator,
            lastModifyUser,
            baseSearchCondition
        );
        val total = whiteIPRecordDAO.countWhiteIPRecord(
            ipList,
            appIdList,
            appNameList,
            actionScopeIdList,
            creator,
            lastModifyUser,
            baseSearchCondition
        );

        PageData<WhiteIPRecordVO> whiteIPRecordPageData = new PageData<>(
            start,
            pageSize,
            total,
            whiteIPRecordList
        );
        log.info(
            "Output:whiteIPRecordPageData("
                + start
                + "," + pageSize
                + "," + total
                + "," + whiteIPRecordList.size() + ")"
        );
        return whiteIPRecordPageData;
    }

    private Long getActionScopeId(ActionScopeEnum actionScope) {
        if (actionScope == null) {
            return null;
        }
        ActionScopeDTO actionScopeDTO = actionScopeDAO.getActionScopeByCode(actionScope.name());
        if (actionScopeDTO == null) {
            log.error("cannot find action scope in db by enum {}, please check init data in db", actionScope);
            return JobConstants.UNAVAILABLE_ACTION_SCOPE_ID;
        }
        return actionScopeDTO.getId();
    }

    @Override
    public List<HostDTO> listAvailableWhiteIPHost(Long appId, ActionScopeEnum actionScope, Collection<Long> hostIds) {
        List<Long> effectiveAppIds = getEffectiveAppIdList(appId);
        return new ArrayList<>(new HashSet<>(whiteIPRecordDAO.listWhiteIPHost(
            effectiveAppIds,
            getActionScopeId(actionScope),
            hostIds
        )));
    }

    @Override
    public List<HostDTO> listAvailableWhiteIPHostByIps(Long appId,
                                                       ActionScopeEnum actionScope,
                                                       Collection<String> ips) {
        List<Long> effectiveAppIds = getEffectiveAppIdList(appId);
        return new ArrayList<>(new HashSet<>(whiteIPRecordDAO.listWhiteIPHostByIps(
            effectiveAppIds,
            getActionScopeId(actionScope),
            ips
        )));
    }

    @Override
    public List<HostDTO> listAvailableWhiteIPHostByIpv6s(Long appId,
                                                         ActionScopeEnum actionScope,
                                                         Collection<String> ipv6s) {
        List<Long> effectiveAppIds = getEffectiveAppIdList(appId);
        return new ArrayList<>(new HashSet<>(whiteIPRecordDAO.listWhiteIPHostByIpv6s(
            effectiveAppIds,
            getActionScopeId(actionScope),
            ipv6s
        )));
    }

    private List<WhiteIPIPDTO> buildIpDtoList(List<HostIdWithMeta> hostList,
                                              String username) {
        List<WhiteIPIPDTO> finalIpDtoList = new ArrayList<>();
        finalIpDtoList.addAll(buildHostDtoList(hostList, username));
        return finalIpDtoList;
    }

    private List<WhiteIPIPDTO> buildHostDtoList(List<HostIdWithMeta> hostList,
                                                String username) {
        if (CollectionUtils.isEmpty(hostList)) {
            return Collections.emptyList();
        }
        // 从本地DB查询
        List<Long> hostIdList = hostList.stream().map(HostIdWithMeta::getHostId).collect(Collectors.toList());
        List<ApplicationHostDTO> hostDTOList = applicationHostDAO.listHostInfoByHostIds(hostIdList);
        Map<Long, ApplicationHostDTO> hostMap = new HashMap<>();
        hostDTOList.forEach(hostDTO -> hostMap.put(hostDTO.getHostId(), hostDTO));

        // 本地DB不存在的HostId从CMDB查询hostId
        List<Long> notInDbHostIdList = hostIdList.stream()
            .filter(hostId -> !hostMap.containsKey(hostId))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notInDbHostIdList)) {
            List<ApplicationHostDTO> cmdbHostList = bizCmdbClient.listHostsByHostIds(notInDbHostIdList);
            if (CollectionUtils.isNotEmpty(cmdbHostList)) {
                cmdbHostList.forEach(hostDTO -> hostMap.put(hostDTO.getHostId(), hostDTO));
            }
            // CMDB中也查不到的IP需要记录
            notInDbHostIdList.removeIf(hostMap::containsKey);
            if (!notInDbHostIdList.isEmpty()) {
                log.warn("Cannot find hostIds in db/cmdb:{}", notInDbHostIdList);
                throw new InvalidParamException(
                    ErrorCode.HOST_ID_NOT_EXIST_IN_CMDB,
                    StringUtil.concatCollection(notInDbHostIdList)
                );
            }
        }

        return hostIdList.stream().map(hostId -> {
            ApplicationHostDTO hostDTO = hostMap.get(hostId);
            return new WhiteIPIPDTO(
                null,
                null,
                hostDTO.getCloudAreaId(),
                hostDTO.getHostId(),
                hostDTO.getIp(),
                hostDTO.getIpv6(),
                username,
                System.currentTimeMillis(),
                username,
                System.currentTimeMillis()
            );
        }).collect(Collectors.toList());
    }

    private List<Long> parseTargetAppIds(WhiteIPRecordCreateUpdateReq createUpdateReq) {
        List<Long> appIdList;
        if (createUpdateReq.isAllScope()) {
            // 对所有业务生效
            appIdList = Collections.singletonList(JobConstants.PUBLIC_APP_ID);
        } else {
            List<ResourceScope> scopeList = createUpdateReq.getScopeList();
            Map<ResourceScope, Long> scopeAppIdMap = appScopeMappingService.getAppIdByScopeList(scopeList);
            appIdList = scopeList.stream()
                .map(scope -> {
                    Long appId = scopeAppIdMap.get(scope);
                    if (appId == null) {
                        String msg = "Cannot find appId by scope " + scope;
                        throw new InternalException(msg, ErrorCode.INTERNAL_ERROR);
                    }
                    return appId;
                }).collect(Collectors.toList());
        }
        return appIdList;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_WHITELIST,
        content = "Create a white list row"
    )
    public WhiteIPRecordDTO createWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        log.info("Input(" + username + "," + createUpdateReq.toString() + ")");
        // 1.参数校验、预处理
        List<HostIdWithMeta> hostList = createUpdateReq.getHostList();
        // 2.appId转换
        List<Long> appIdList = parseTargetAppIds(createUpdateReq);

        List<Long> actionScopeIdList = createUpdateReq.getActionScopeIdList();
        val ipDtoList = buildIpDtoList(hostList, username);
        val actionScopeDtoList = actionScopeIdList.stream().map(actionScopeId -> new WhiteIPActionScopeDTO(
            null,
            null,
            actionScopeId,
            username,
            System.currentTimeMillis(),
            username,
            System.currentTimeMillis()
        )).collect(Collectors.toList());

        long recordId = whiteIPRecordDAO.insertWhiteIPRecord(new WhiteIPRecordDTO(
            null,
            appIdList,
            createUpdateReq.getRemark(),
            ipDtoList,
            actionScopeDtoList,
            username,
            System.currentTimeMillis(),
            username,
            System.currentTimeMillis()
        ));
        log.info("Insert success");
        return getWhiteIPDetailById(username, recordId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_WHITELIST,
        content = EventContentConstants.EDIT_WHITE_LIST
    )
    public WhiteIPRecordDTO updateWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        log.info("Input(" + username + "," + createUpdateReq.toString() + ")");
        // 1.参数校验、预处理
        List<HostIdWithMeta> hostList = createUpdateReq.getHostList();
        // 2.appId转换
        List<Long> appIdList = parseTargetAppIds(createUpdateReq);

        List<Long> actionScopeIdList = createUpdateReq.getActionScopeIdList();
        val ipDtoList = buildIpDtoList(hostList, username);
        val actionScopeDtoList = actionScopeIdList.stream().map(actionScopeId -> new WhiteIPActionScopeDTO(
            null,
            null,
            actionScopeId,
            username,
            System.currentTimeMillis(),
            username,
            System.currentTimeMillis()
        )).collect(Collectors.toList());
        var recordId = createUpdateReq.getId();
        ipDtoList.forEach(it -> it.setRecordId(recordId));
        actionScopeDtoList.forEach(it -> it.setRecordId(recordId));
        WhiteIPRecordDTO whiteIPRecordDTO = whiteIPRecordDAO.getWhiteIPRecordById(recordId);
        whiteIPRecordDTO.setAppIdList(appIdList);
        whiteIPRecordDTO.setRemark(createUpdateReq.getRemark());
        whiteIPRecordDTO.setIpList(ipDtoList);
        whiteIPRecordDTO.setActionScopeList(actionScopeDtoList);
        whiteIPRecordDTO.setLastModifier(username);
        whiteIPRecordDTO.setLastModifyTime(System.currentTimeMillis());

        //修改
        int affectedRows = whiteIPRecordDAO.updateWhiteIPRecordById(whiteIPRecordDTO);
        log.info("{} white ip records updated", affectedRows);
        return getWhiteIPDetailById(username, recordId);
    }

    @Override
    public WhiteIPRecordDTO getWhiteIPDetailById(String username, Long id) {
        WhiteIPRecordDTO record = whiteIPRecordDAO.getWhiteIPRecordById(id);
        List<ApplicationDTO> applicationInfoList = applicationDAO.listAppsByAppIds(record.getAppIdList());
        record.setAppList(applicationInfoList);
        return record;
    }

    @Override
    public List<CloudAreaInfoVO> listCloudAreas(String username) {
        log.info("Input(" + username + ")");
        List<CcCloudAreaInfoDTO> cloudAreaInfoList = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang())
            .getCloudAreaList();
        return cloudAreaInfoList.stream().map(it ->
            new CloudAreaInfoVO(it.getId(), it.getName())).collect(Collectors.toList());
    }

    @Override
    public List<ActionScopeVO> listActionScope(String username) {
        return actionScopeDAO.listActionScopeDTO().stream().map(it ->
            new ActionScopeVO(
                it.getId(),
                i18nService.getI18n(ActionScopeEnum.getI18nCodeByName(it.getCode())),
                it.getCreator(),
                it.getCreateTime(),
                it.getLastModifier(),
                it.getLastModifyTime()
            )
        ).collect(Collectors.toList());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_WHITELIST,
        content = "Delete a white list row"
    )
    public Long deleteWhiteIPById(String username, Long id) {
        return (long) whiteIPRecordDAO.deleteWhiteIPRecordById(id);
    }

    @Override
    public List<String> getWhiteIPActionScopes(Long appId, String ip, Long cloudAreaId, Long hostId) {
        log.info("Input=({},{},{},{})", appId, ip, cloudAreaId, hostId);
        // 1.找出与当前业务关联的所有appId
        List<Long> effectiveAppIds = getEffectiveAppIdList(appId);
        // 2.再查对应的白名单
        List<String> actionScopes = new ArrayList<>();
        if (hostId != null) {
            actionScopes = whiteIPRecordDAO.getWhiteIPActionScopes(effectiveAppIds, hostId);
        }
        return actionScopes;
    }

    @Override
    public List<ServiceWhiteIPInfo> listWhiteIPInfos() {
        List<ServiceWhiteIPInfo> resultList = new ArrayList<>();
        //查出所有Record
        List<WhiteIPRecordDTO> recordList = whiteIPRecordDAO.listAllWhiteIPRecord();
        if (CollectionUtils.isEmpty(recordList)) {
            return resultList;
        }
        //查询appIdList长度为1的业务信息
        Set<Long> scopeIdSet = new HashSet<>();
        recordList.forEach(record -> {
            //添加所有包含生效范围id列表，方便后续一次性查出关联的生效范围code
            scopeIdSet.addAll(record.getActionScopeList().stream()
                .map(WhiteIPActionScopeDTO::getActionScopeId).collect(Collectors.toSet()));
        });

        int maxInCount = 1000;
        List<List<Long>> scopeIdsList = Lists.partition(new ArrayList<>(scopeIdSet), maxInCount);
        List<ActionScopeDTO> actionScopeDTOList = new ArrayList<>();
        for (List<Long> scopeIdList : scopeIdsList) {
            actionScopeDTOList.addAll(actionScopeDAO.getActionScopeByIds(scopeIdList));
        }

        Map<Long, List<ActionScopeDTO>> actionScopeDTOMap = actionScopeDTOList.stream().collect(
            Collectors.groupingBy(ActionScopeDTO::getId));

        for (WhiteIPRecordDTO whiteIPRecordDTO : recordList) {
            boolean isAllApp = false;
            List<Long> appIdList = whiteIPRecordDTO.getAppIdList();
            for (Long appId : appIdList) {
                isAllApp = appId == JobConstants.PUBLIC_APP_ID;
                if (isAllApp) {
                    break;
                }
            }
            if (isAllApp) {
                genAllAppWhiteIPInfo(whiteIPRecordDTO, actionScopeDTOMap, resultList);
            } else {
                genNormalAppWhiteIPInfo(whiteIPRecordDTO, actionScopeDTOMap, resultList);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("WhiteIPInfos before merge:{}", JsonUtils.toJson(resultList));
        }
        // 合并HostId相同的多条记录
        Map<Long, ServiceWhiteIPInfo> resultMap = new HashMap<>();
        for (ServiceWhiteIPInfo whiteIPInfo : resultList) {
            Long hostId = whiteIPInfo.getHostId();
            if (hostId == null) {
                log.warn("Ignore whiteIp whose hostId is null:{}", whiteIPInfo);
                continue;
            }
            if (resultMap.containsKey(hostId)) {
                resultMap.put(hostId, mergeServiceWhiteIPInfo(resultMap.get(hostId), whiteIPInfo));
            } else {
                resultMap.put(hostId, whiteIPInfo);
            }
        }
        resultList.clear();
        resultList.addAll(resultMap.values());
        if (log.isDebugEnabled()) {
            log.debug("WhiteIPInfos after merge:{}", JsonUtils.toJson(resultList));
        }
        return resultList;
    }

    private ServiceWhiteIPInfo mergeServiceWhiteIPInfo(ServiceWhiteIPInfo whiteIPInfo1,
                                                       ServiceWhiteIPInfo whiteIPInfo2) {
        log.debug(String.format("merge ServiceWhiteIPInfo:whiteIPInfo1=%s,whiteIPInfo2=%s", whiteIPInfo1,
            whiteIPInfo2));
        if (whiteIPInfo1 == null) {
            return whiteIPInfo2;
        } else if (whiteIPInfo2 == null) {
            return whiteIPInfo1;
        }
        if (!(whiteIPInfo1.getHostId().equals(whiteIPInfo2.getHostId()))) {
            throw new RuntimeException("Cannot merge ServiceWhiteIPInfo with different hostId");
        }
        ServiceWhiteIPInfo finalServiceWhiteIPInfo = new ServiceWhiteIPInfo();
        finalServiceWhiteIPInfo.setHostId(whiteIPInfo1.getHostId());
        finalServiceWhiteIPInfo.setAllAppActionScopeList(new ArrayList<>());
        finalServiceWhiteIPInfo.setForAllApp(false);
        if (whiteIPInfo1.isForAllApp()) {
            finalServiceWhiteIPInfo.setForAllApp(true);
            finalServiceWhiteIPInfo.setAllAppActionScopeList(
                mergeList(
                    finalServiceWhiteIPInfo.getAllAppActionScopeList(),
                    whiteIPInfo1.getAllAppActionScopeList()
                )
            );
        }
        if (whiteIPInfo2.isForAllApp()) {
            finalServiceWhiteIPInfo.setForAllApp(true);
            finalServiceWhiteIPInfo.setAllAppActionScopeList(
                mergeList(
                    finalServiceWhiteIPInfo.getAllAppActionScopeList(),
                    whiteIPInfo2.getAllAppActionScopeList()
                )
            );
        }
        finalServiceWhiteIPInfo.setAppIdActionScopeMap(mergeMap(finalServiceWhiteIPInfo.getAppIdActionScopeMap(),
            whiteIPInfo1.getAppIdActionScopeMap()));
        finalServiceWhiteIPInfo.setAppIdActionScopeMap(mergeMap(finalServiceWhiteIPInfo.getAppIdActionScopeMap(),
            whiteIPInfo2.getAppIdActionScopeMap()));
        log.debug(String.format("merge ServiceWhiteIPInfo:finalServiceWhiteIPInfo=%s", finalServiceWhiteIPInfo));
        return finalServiceWhiteIPInfo;
    }

    private <T, R> Map<T, List<R>> mergeMap(Map<T, List<R>> map1, Map<T, List<R>> map2) {
        if (map1 == null) {
            return map2;
        } else if (map2 == null) {
            return map1;
        }
        Map<T, List<R>> finalMap = new HashMap<>(map1);
        map2.keySet().forEach(key -> {
            if (finalMap.containsKey(key)) {
                finalMap.put(key, mergeList(finalMap.get(key), map2.get(key)));
            } else {
                finalMap.put(key, map2.get(key));
            }
        });
        return finalMap;
    }

    private <T> List<T> mergeList(List<T> list1, List<T> list2) {
        List<T> finalList = new ArrayList<>();
        if (list1 != null) {
            finalList.addAll(list1);
        }
        if (list2 != null) {
            finalList.addAll(list2);
        }
        HashSet<T> set = new HashSet<>(finalList);
        finalList.clear();
        finalList.addAll(set);
        return finalList;
    }

    /**
     * 生成全业务对应的白名单信息
     *
     * @param whiteIPRecordDTO  白名单记录
     * @param actionScopeDTOMap 作用范围Map
     * @param resultList        白名单信息结果列表
     */
    private void genAllAppWhiteIPInfo(WhiteIPRecordDTO whiteIPRecordDTO,
                                      Map<Long, List<ActionScopeDTO>> actionScopeDTOMap,
                                      List<ServiceWhiteIPInfo> resultList) {
        //封装全业务
        List<WhiteIPIPDTO> whiteIPIPDTOList = whiteIPRecordDTO.getIpList();
        for (WhiteIPIPDTO whiteIPIPDTO : whiteIPIPDTOList) {
            Long hostId = whiteIPIPDTO.getHostId();
            if (hostId == null) {
                log.warn("Ignore allApp whiteIp whose hostId is null:{}", whiteIPIPDTO);
                continue;
            }
            ServiceWhiteIPInfo serviceWhiteIPInfo = new ServiceWhiteIPInfo();
            serviceWhiteIPInfo.setForAllApp(true);
            List<String> allAppActionScopeList = new ArrayList<>();
            for (WhiteIPActionScopeDTO actionScope : whiteIPRecordDTO.getActionScopeList()) {
                ActionScopeDTO actionScopeDTO =
                    actionScopeDTOMap.get(actionScope.getActionScopeId()) == null ? null :
                        actionScopeDTOMap.get(actionScope.getActionScopeId()).get(0);
                if (actionScopeDTO == null) {
                    log.warn("Cannot find actionScopeDTO by id {}", actionScope.getActionScopeId());
                } else {
                    allAppActionScopeList.add(actionScopeDTO.getCode());
                }
            }
            serviceWhiteIPInfo.setAllAppActionScopeList(allAppActionScopeList);
            serviceWhiteIPInfo.setAppIdActionScopeMap(new HashMap<>());
            serviceWhiteIPInfo.setHostId(hostId);
            resultList.add(serviceWhiteIPInfo);
        }
    }

    /**
     * 生成普通业务对应的白名单信息
     *
     * @param whiteIPRecordDTO  白名单记录
     * @param actionScopeDTOMap 作用范围Map
     * @param resultList        白名单信息结果列表
     */
    private void genNormalAppWhiteIPInfo(WhiteIPRecordDTO whiteIPRecordDTO,
                                         Map<Long, List<ActionScopeDTO>> actionScopeDTOMap,
                                         List<ServiceWhiteIPInfo> resultList) {
        List<Long> appIdList = whiteIPRecordDTO.getAppIdList();
        List<WhiteIPIPDTO> whiteIPIPDTOList = whiteIPRecordDTO.getIpList();

        for (WhiteIPIPDTO whiteIPIPDTO : whiteIPIPDTOList) {
            Long hostId = whiteIPIPDTO.getHostId();
            if (hostId == null) {
                log.warn("Ignore normal whiteIp whose hostId is null:{}", whiteIPIPDTO);
                continue;
            }
            ServiceWhiteIPInfo serviceWhiteIPInfo = new ServiceWhiteIPInfo();
            serviceWhiteIPInfo.setForAllApp(false);
            serviceWhiteIPInfo.setAllAppActionScopeList(new ArrayList<>());
            serviceWhiteIPInfo.setHostId(hostId);
            HashMap<Long, List<String>> map = new HashMap<>();
            List<String> actionScopeList = new ArrayList<>();
            whiteIPRecordDTO.getActionScopeList().forEach(actionScope -> {
                ActionScopeDTO actionScopeDTO = actionScopeDTOMap.get(actionScope.getActionScopeId()) == null ? null :
                    actionScopeDTOMap.get(actionScope.getActionScopeId()).get(0);
                if (actionScopeDTO == null) {
                    log.warn("Cannot find actionScope by id {}", actionScope.getActionScopeId());
                    actionScopeList.add("");
                } else {
                    actionScopeList.add(actionScopeDTO.getCode());
                }
            });
            for (Long appId : appIdList) {
                map.put(appId, actionScopeList);
            }
            serviceWhiteIPInfo.setAppIdActionScopeMap(map);
            resultList.add(serviceWhiteIPInfo);
        }
    }
}
