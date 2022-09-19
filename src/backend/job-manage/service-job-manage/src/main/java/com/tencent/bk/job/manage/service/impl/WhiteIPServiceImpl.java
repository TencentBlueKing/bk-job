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
import com.tencent.bk.job.common.RequestIdLogger;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.common.util.SimpleRequestIdLogger;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPRecordDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.CloudIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.DSLContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
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

    private static final RequestIdLogger LOG =
        new SimpleRequestIdLogger(LoggerFactory.getLogger(WhiteIPServiceImpl.class));
    private static final String SEPERATOR_COMMA = ",";
    private static final String SEPERATOR_ENTER = "\n";
    private final MessageI18nService i18nService;
    private final DSLContext dslContext;
    private final ActionScopeDAO actionScopeDAO;
    private final WhiteIPRecordDAO whiteIPRecordDAO;
    private final ApplicationDAO applicationDAO;
    private final ApplicationService applicationService;
    private final AppScopeMappingService appScopeMappingService;
    private final ApplicationHostDAO applicationHostDAO;
    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public WhiteIPServiceImpl(
        @Qualifier("job-manage-dsl-context") DSLContext dslContext,
        ActionScopeDAO actionScopeDAO,
        WhiteIPRecordDAO whiteIPRecordDAO,
        ApplicationDAO applicationDAO,
        ApplicationService applicationService,
        MessageI18nService i18nService,
        AppScopeMappingService appScopeMappingService,
        ApplicationHostDAO applicationHostDAO,
        IBizCmdbClient bizCmdbClient) {
        this.dslContext = dslContext;
        this.actionScopeDAO = actionScopeDAO;
        this.whiteIPRecordDAO = whiteIPRecordDAO;
        this.applicationDAO = applicationDAO;
        this.i18nService = i18nService;
        this.applicationService = applicationService;
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
        if (appId != null && !appId.equals(JobConstants.DEFAULT_ALL_BIZ_SET_ID)) {
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
        LOG.infoWithRequestId(
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
            dslContext,
            ipList,
            appIdList,
            appNameList,
            actionScopeIdList,
            creator,
            lastModifyUser,
            baseSearchCondition
        );
        val total = whiteIPRecordDAO.countWhiteIPRecord(
            dslContext,
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
        LOG.infoWithRequestId(
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

    @Override
    public List<CloudIPDTO> listWhiteIP(Long appId, ActionScopeEnum actionScope) {
        List<Long> effectiveAppIds = getEffectiveAppIdList(appId);
        return whiteIPRecordDAO.listWhiteIPByAppIds(
            dslContext,
            effectiveAppIds,
            getActionScopeId(actionScope)
        );
    }

    private List<String> checkReqAndGetIpList(WhiteIPRecordCreateUpdateReq createUpdateReq) {
        List<ResourceScope> scopeList = createUpdateReq.getScopeList();
        if (!createUpdateReq.isAllScope() && (null == scopeList || scopeList.isEmpty())) {
            log.warn("scopeList cannot be null or empty");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                ArrayUtil.toArray("scopeList", "scopeList cannot be null or empty"));
        }
        String remark = createUpdateReq.getRemark();
        if (null != remark && remark.length() > 100) {
            log.warn("remark cannot be null and length cannot be over 100");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                ArrayUtil.toArray("remark", "remark cannot be null and length cannot be over 100"));
        }
        String ipStr = createUpdateReq.getIpStr();
        Set<String> finalIpv4Set = new HashSet<>();
        if (ipStr != null) {
            if (ipStr.length() > 5000) {
                log.warn("length of ipStr cannot be over 5000");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            } else {
                List<String> ipListFromIpStr = Arrays.asList(ipStr.trim().split(SEPERATOR_ENTER));
                finalIpv4Set.addAll(checkAndGetIpv4List("ipStr", ipListFromIpStr));
            }
        }
        return new ArrayList<>(finalIpv4Set);
    }

    private List<String> checkAndGetIpv4List(@SuppressWarnings("SameParameterValue") String paramName,
                                             List<String> ipv4List) {
        //过滤空值
        ipv4List =
            ipv4List.stream().filter(ip -> !ip.trim().isEmpty()).collect(Collectors.toList());
        ipv4List.forEach(ip -> {
            if (!IpUtils.checkIp(ip.trim())) {
                String msg = "contains invalid ipv4 format";
                log.warn(msg);
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    ArrayUtil.toArray(paramName, msg));
            }
        });
        return ipv4List;
    }

    private List<WhiteIPIPDTO> buildIpDtoList(Long cloudAreaId,
                                              List<String> ipv4List,
                                              List<HostIdWithMeta> hostList,
                                              String username) {
        List<WhiteIPIPDTO> finalIpDtoList = new ArrayList<>();
        finalIpDtoList.addAll(buildIpv4DtoList(cloudAreaId, ipv4List, username));
        finalIpDtoList.addAll(buildHostDtoList(hostList, username));
        return finalIpDtoList;
    }

    private List<WhiteIPIPDTO> buildIpv4DtoList(Long cloudAreaId,
                                                List<String> ipv4List,
                                                String username) {
        List<String> cloudIpList = new ArrayList<>();
        ipv4List.forEach(ipv4 -> cloudIpList.add(new CloudIPDTO(cloudAreaId, ipv4).getCloudIP()));
        // 根据IP查询hostId、IPv6信息
        // 从本地DB查询
        List<ApplicationHostDTO> hostDTOList = applicationHostDAO.listHostsByCloudIps(cloudIpList);
        Map<String, ApplicationHostDTO> hostMap = new HashMap<>();
        hostDTOList.forEach(hostDTO -> hostMap.put(hostDTO.getCloudIp(), hostDTO));

        // 本地DB不存在的IP从CMDB查询hostId
        List<String> notInDbCloudIpList = cloudIpList.stream()
            .filter(cloudIp -> !hostMap.containsKey(cloudIp))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notInDbCloudIpList)) {
            List<ApplicationHostDTO> cmdbHostList = bizCmdbClient.listHostsByCloudIps(notInDbCloudIpList);
            if (CollectionUtils.isNotEmpty(cmdbHostList)) {
                cmdbHostList.forEach(hostDTO -> hostMap.put(hostDTO.getCloudIp(), hostDTO));
            }
        }

        // CMDB中也查不到的IP需要记录
        notInDbCloudIpList.removeIf(hostMap::containsKey);
        if (!notInDbCloudIpList.isEmpty()) {
            log.warn("Cannot find ips in db/cmdb:{}", notInDbCloudIpList);
            throw new InvalidParamException(
                ErrorCode.IP_NOT_EXIST_IN_CMDB,
                StringUtil.concatCollection(notInDbCloudIpList)
            );
        }

        return ipv4List.stream().map(ipv4 -> {
            ApplicationHostDTO hostDTO = hostMap.get(new CloudIPDTO(cloudAreaId, ipv4).getCloudIP());
            return new WhiteIPIPDTO(
                null,
                null,
                cloudAreaId,
                hostDTO == null ? null : hostDTO.getHostId(),
                ipv4,
                hostDTO == null ? null : hostDTO.getIpv6(),
                username,
                System.currentTimeMillis(),
                username,
                System.currentTimeMillis()
            );
        }).collect(Collectors.toList());
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
            appIdList = scopeList.parallelStream()
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
    public Long saveWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        LOG.infoWithRequestId("Input(" + username + "," + createUpdateReq.toString() + ")");
        // 1.参数校验、预处理
        List<String> ipv4List = checkReqAndGetIpList(createUpdateReq);
        List<HostIdWithMeta> hostList = createUpdateReq.getHostList();
        // 2.appId转换
        List<Long> appIdList = parseTargetAppIds(createUpdateReq);

        List<Long> actionScopeIdList = createUpdateReq.getActionScopeIdList();
        val ipDtoList = buildIpDtoList(createUpdateReq.getCloudAreaId(), ipv4List, hostList, username);
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
        if (recordId != null && recordId > 0) {
            Long finalRecordId = recordId;
            ipDtoList.forEach(it -> it.setRecordId(finalRecordId));
            actionScopeDtoList.forEach(it -> it.setRecordId(finalRecordId));
            WhiteIPRecordDTO whiteIPRecordDTO = whiteIPRecordDAO.getWhiteIPRecordById(dslContext, recordId);
            whiteIPRecordDTO.setAppIdList(appIdList);
            whiteIPRecordDTO.setRemark(createUpdateReq.getRemark());
            whiteIPRecordDTO.setIpList(ipDtoList);
            whiteIPRecordDTO.setActionScopeList(actionScopeDtoList);
            whiteIPRecordDTO.setLastModifier(username);
            whiteIPRecordDTO.setLastModifyTime(System.currentTimeMillis());
            //修改
            int affectedRows = whiteIPRecordDAO.updateWhiteIPRecordById(dslContext, whiteIPRecordDTO);
            log.info("{} white ip records updated", affectedRows);
        } else {
            //新增
            recordId = whiteIPRecordDAO.insertWhiteIPRecord(dslContext, new WhiteIPRecordDTO(
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
            LOG.infoWithRequestId("insert success,recordId=" + recordId);
        }
        return recordId;
    }

    private boolean isAllScope(List<Long> appIdList) {
        return appIdList != null
            && appIdList.size() > 0
            && new HashSet<>(appIdList).contains(JobConstants.PUBLIC_APP_ID);
    }

    @Override
    public WhiteIPRecordVO getWhiteIPDetailById(String username, Long id) {
        LOG.infoWithRequestId("Input(" + username + "," + id + ")");
        val record = whiteIPRecordDAO.getWhiteIPRecordById(dslContext, id);
        Long cloudAreaId = null;
        if (record.getIpList().size() > 0) {
            cloudAreaId = record.getIpList().get(0).getCloudAreaId();
        }
        boolean allScope = isAllScope(record.getAppIdList());
        val applicationInfoList = applicationDAO.listAppsByAppIds(record.getAppIdList());
        List<ScopeVO> scopeVOList = null;
        if (!allScope) {
            scopeVOList = applicationInfoList.stream().map(it -> new ScopeVO(
                it.getScope().getType().getValue(),
                it.getScope().getId(),
                it.getName()
            )).collect(Collectors.toList());
        }
        WhiteIPRecordVO whiteIPRecord = new WhiteIPRecordVO();
        whiteIPRecord.setId(id);
        whiteIPRecord.setCloudAreaId(cloudAreaId);
        whiteIPRecord.setHostList(record.getIpList().stream().map(WhiteIPIPDTO::extractWhiteIPHostVO)
            .collect(Collectors.toList()));
        whiteIPRecord.setActionScopeList(record.getActionScopeList().stream().map(actionScopeDTO -> {
            val actionScopeId = actionScopeDTO.getActionScopeId();
            return actionScopeDAO.getActionScopeVOById(actionScopeId);
        }).collect(Collectors.toList()));
        whiteIPRecord.setAllScope(allScope);
        whiteIPRecord.setScopeList(scopeVOList);
        whiteIPRecord.setRemark(record.getRemark());
        whiteIPRecord.setCreator(record.getCreator());
        whiteIPRecord.setCreateTime(record.getCreateTime());
        whiteIPRecord.setLastModifier(record.getLastModifier());
        whiteIPRecord.setLastModifyTime(record.getLastModifyTime());
        return whiteIPRecord;
    }

    @Override
    public List<CloudAreaInfoVO> listCloudAreas(String username) {
        LOG.infoWithRequestId("Input(" + username + ")");
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
    public Long deleteWhiteIPById(String username, Long id) {
        return (long) whiteIPRecordDAO.deleteWhiteIPRecordById(dslContext, id);
    }

    @Override
    public List<String> getWhiteIPActionScopes(Long appId, String ip, Long cloudAreaId, Long hostId) {
        log.info("Input=({},{},{},{})", appId, ip, cloudAreaId, hostId);
        // 1.找出与当前业务关联的所有appId
        List<Long> effectiveAppIds = getEffectiveAppIdList(appId);
        // 2.再查对应的白名单
        if (hostId != null) {
            return whiteIPRecordDAO.getWhiteIPActionScopes(dslContext, effectiveAppIds, hostId);
        } else {
            // TODO: IPv6兼容代码，发布完成后删除
            return whiteIPRecordDAO.getWhiteIPActionScopes(dslContext, effectiveAppIds, ip, cloudAreaId);
        }
    }

    @Override
    public List<ServiceWhiteIPInfo> listWhiteIPInfos() {
        List<ServiceWhiteIPInfo> resultList = new ArrayList<>();
        //查出所有Record
        List<WhiteIPRecordDTO> recordList = whiteIPRecordDAO.listAllWhiteIPRecord(dslContext);
        if (CollectionUtils.isEmpty(recordList)) {
            return resultList;
        }
        //查询appIdList长度为1的业务信息
        Set<Long> appIdSet = new HashSet<>();
        Set<Long> scopeIdSet = new HashSet<>();
        List<ApplicationDTO> applicationDTOList = new ArrayList<>();
        recordList.forEach(record -> {
            appIdSet.addAll(record.getAppIdList());
            //添加所有包含生效范围id列表，方便后续一次性查出关联的生效范围code
            scopeIdSet.addAll(record.getActionScopeList().parallelStream()
                .map(WhiteIPActionScopeDTO::getActionScopeId).collect(Collectors.toSet()));
        });
        if (CollectionUtils.isNotEmpty(appIdSet)) {
            applicationDTOList = applicationService.listAppsByAppIds(appIdSet);
        }

        Map<Long, List<ApplicationDTO>> applicationDTOMap = applicationDTOList.parallelStream().collect(
            Collectors.groupingBy(ApplicationDTO::getId));

        int maxInCount = 1000;
        List<List<Long>> scopeIdsList = Lists.partition(new ArrayList<>(scopeIdSet), maxInCount);
        List<ActionScopeDTO> actionScopeDTOList = new ArrayList<>();
        for (List<Long> scopeIdList : scopeIdsList) {
            actionScopeDTOList.addAll(actionScopeDAO.getActionScopeByIds(scopeIdList));
        }

        Map<Long, List<ActionScopeDTO>> actionScopeDTOMap = actionScopeDTOList.parallelStream().collect(
            Collectors.groupingBy(ActionScopeDTO::getId));

        for (WhiteIPRecordDTO whiteIPRecordDTO : recordList) {
            boolean isAllApp = false;
            List<Long> appIdList = whiteIPRecordDTO.getAppIdList();
            for (Long appId : appIdList) {
                ApplicationDTO applicationDTO = applicationDTOMap.get(appId) == null ? null :
                    applicationDTOMap.get(appId).get(0);
                // TODO:兼容实现，数据迁移后去除全业务对所有业务生效
                isAllApp = appId == JobConstants.PUBLIC_APP_ID
                    || (applicationDTO != null && applicationDTO.isAllBizSet());
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
        // 合并IP相同的多条记录
        Map<String, ServiceWhiteIPInfo> resultMap = new HashMap<>();
        for (ServiceWhiteIPInfo whiteIPInfo : resultList) {
            String key = whiteIPInfo.getCloudId().toString() + ":" + whiteIPInfo.getIp();
            if (resultMap.containsKey(key)) {
                resultMap.put(key, mergeServiceWhiteIPInfo(resultMap.get(key), whiteIPInfo));
            } else {
                resultMap.put(key, whiteIPInfo);
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
        if (!(whiteIPInfo1.getCloudId().equals(whiteIPInfo2.getCloudId())
            && whiteIPInfo1.getIp().equals(whiteIPInfo2.getIp()))) {
            throw new RuntimeException("Cannot merge ServiceWhiteIPInfo with different cloudId and Ip");
        }
        ServiceWhiteIPInfo finalServiceWhiteIPInfo = new ServiceWhiteIPInfo();
        finalServiceWhiteIPInfo.setHostId(whiteIPInfo1.getHostId());
        finalServiceWhiteIPInfo.setCloudId(whiteIPInfo1.getCloudId());
        finalServiceWhiteIPInfo.setIp(whiteIPInfo1.getIp());
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
        whiteIPIPDTOList.forEach(whiteIPIPDTO -> {
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
            serviceWhiteIPInfo.setHostId(whiteIPIPDTO.getHostId());
            serviceWhiteIPInfo.setCloudId(whiteIPIPDTO.getCloudAreaId());
            serviceWhiteIPInfo.setIp(whiteIPIPDTO.getIp());
            resultList.add(serviceWhiteIPInfo);
        });
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
            ServiceWhiteIPInfo serviceWhiteIPInfo = new ServiceWhiteIPInfo();
            serviceWhiteIPInfo.setForAllApp(false);
            serviceWhiteIPInfo.setAllAppActionScopeList(new ArrayList<>());
            serviceWhiteIPInfo.setHostId(whiteIPIPDTO.getHostId());
            serviceWhiteIPInfo.setCloudId(whiteIPIPDTO.getCloudAreaId());
            serviceWhiteIPInfo.setIp(whiteIPIPDTO.getIp());
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
