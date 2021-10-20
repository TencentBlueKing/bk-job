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

import com.tencent.bk.job.common.RequestIdLogger;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.common.util.SimpleRequestIdLogger;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPRecordDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.CloudIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.DSLContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhiteIPServiceImpl implements WhiteIPService {

    private static final RequestIdLogger LOG =
        new SimpleRequestIdLogger(LoggerFactory.getLogger(WhiteIPServiceImpl.class));
    private static final String SEPERATOR_COMMA = ",";
    private static final String SEPERATOR_ENTER = "\n";
    private final MessageI18nService i18nService;
    private DSLContext dslContext;
    private ActionScopeDAO actionScopeDAO;
    private WhiteIPRecordDAO whiteIPRecordDAO;
    private ApplicationInfoDAO applicationInfoDAO;
    private ApplicationService applicationService;

    @Autowired
    public WhiteIPServiceImpl(
        @Qualifier("job-manage-dsl-context") DSLContext dslContext,
        ActionScopeDAO actionScopeDAO,
        WhiteIPRecordDAO whiteIPRecordDAO,
        ApplicationInfoDAO applicationInfoDAO,
        ApplicationService applicationService,
        MessageI18nService i18nService
    ) {
        this.dslContext = dslContext;
        this.actionScopeDAO = actionScopeDAO;
        this.whiteIPRecordDAO = whiteIPRecordDAO;
        this.applicationInfoDAO = applicationInfoDAO;
        this.i18nService = i18nService;
        this.applicationService = applicationService;
        applicationService.setWhiteIPService(this);
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

    @Override
    public List<CloudIPDTO> listWhiteIP(Long appId, ActionScopeEnum actionScope) {
        List<Long> fullAppIds = applicationService.getFullAppIds(appId);
        log.info("appId={}, contains by fullAppIds={}", appId, fullAppIds);
        ActionScopeDTO actionScopeDTO = null;
        if (actionScope != null) {
            actionScopeDTO = actionScopeDAO.getActionScopeByCode(actionScope.name());
        }
        return whiteIPRecordDAO.listWhiteIPByAppIds(
            dslContext,
            fullAppIds,
            actionScopeDTO == null ? null : actionScopeDTO.getId()
        );
    }

    private List<String> checkReqAndGetIpList(WhiteIPRecordCreateUpdateReq createUpdateReq) {
        String appIdStr = createUpdateReq.getAppIdStr();
        if (null == appIdStr || appIdStr.isEmpty()) {
            throw new InvalidParamException("appIdStr", "appIdStr cannot be null or empty");
        }
        List<Long> appIdList = Arrays.stream(appIdStr.split(","))
            .map(Long::parseLong).collect(Collectors.toList());
        if (appIdList.isEmpty()) {
            throw new InvalidParamException("appIdStr", "appIdStr must contain at least one valid ip");
        }
        Long cloudAreaId = createUpdateReq.getCloudAreaId();
        if (null == cloudAreaId) {
            throw new InvalidParamException("cloudAreaId", "cloudAreaId cannot be null");
        }
        String remark = createUpdateReq.getRemark();
        if (null != remark && remark.length() > 100) {
            throw new InvalidParamException("remark", "remark cannot be null and length cannot be over 100");
        }
        String ipStr = createUpdateReq.getIpStr();
        List<String> ipList;
        if (null == ipStr || ipStr.trim().isEmpty()) {
            throw new InvalidParamException("ipStr", "ipStr cannot be null or empty");
        } else if (ipStr.length() > 5000) {
            throw new InvalidParamException("ipStr", "length of ipStr cannot be over 5000");
        } else {
            ipList = Arrays.asList(ipStr.trim().split(SEPERATOR_ENTER));
            //过滤空值
            ipList = ipList.stream().filter(ip -> !ip.trim().isEmpty()).collect(Collectors.toList());
            ipList.forEach(ip -> {
                //正则校验
                if (!IpUtils.checkIp(ip.trim()))
                    throw new InvalidParamException("ipStr", "not a valid ip format");
            });
        }
        List<String> uniqueIpList = new ArrayList<>();
        for (String ip : ipList) {
            if (!uniqueIpList.contains(ip)) {
                uniqueIpList.add(ip);
            }
        }
        return uniqueIpList;
    }

    @Override
    public Long saveWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        LOG.infoWithRequestId("Input(" + username + "," + createUpdateReq.toString() + ")");
        //1.参数校验、预处理
        List<String> ipList = checkReqAndGetIpList(createUpdateReq);
        List<Long> appIdList = Arrays.stream(createUpdateReq.getAppIdStr().split(","))
            .map(Long::parseLong).collect(Collectors.toList());
        List<Long> actionScopeIdList = createUpdateReq.getActionScopeIdList();
        val ipDtoList = ipList.stream().map(ip -> new WhiteIPIPDTO(
            null,
            null,
            createUpdateReq.getCloudAreaId(),
            ip,
            username,
            System.currentTimeMillis(),
            username,
            System.currentTimeMillis()
        )).collect(Collectors.toList());
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
            whiteIPRecordDAO.updateWhiteIPRecordById(dslContext, whiteIPRecordDTO);
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

    @Override
    public WhiteIPRecordVO getWhiteIPDetailById(String username, Long id) {
        LOG.infoWithRequestId("Input(" + username + "," + id + ")");
        val record = whiteIPRecordDAO.getWhiteIPRecordById(dslContext, id);
        Long cloudAreaId = null;
        if (record.getIpList().size() > 0) {
            cloudAreaId = record.getIpList().get(0).getCloudAreaId();
        }
        val applicationInfoList = applicationInfoDAO.getAppInfoByIds(record.getAppIdList());
        List<AppVO> appVOList = applicationInfoList.stream().map(it -> {
            int appType = -1;
            if (it.getAppType() != null) {
                appType = it.getAppType().getValue();
            }
            return new AppVO(
                it.getId(),
                it.getName(),
                appType,
                null,
                null,
                null
            );
        }).collect(Collectors.toList());
        return new WhiteIPRecordVO(
            id,
            cloudAreaId,
            record.getIpList().stream().map(WhiteIPIPDTO::getIp).collect(Collectors.toList()),
            record.getActionScopeList().stream().map(actionScopeDTO -> {
                val actionScopeId = actionScopeDTO.getActionScopeId();
                return actionScopeDAO.getActionScopeVOById(actionScopeId);
            }).collect(Collectors.toList()),
            appVOList,
            record.getRemark(),
            record.getCreator(),
            record.getCreateTime(),
            record.getLastModifier(),
            record.getLastModifyTime()
        );
    }

    @Override
    public List<CloudAreaInfoVO> listCloudAreas(String username) {
        LOG.infoWithRequestId("Input(" + username + ")");
        List<CcCloudAreaInfoDTO> cloudAreaInfoList = CcClientFactory.getCcClient(JobContextUtil.getUserLang())
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
    public List<String> getWhiteIPActionScopes(Long appId, String ip, Long cloudAreaId) {
        log.info("Input=({},{},{})", appId, ip, cloudAreaId);
        // 1.找出包含当前业务的业务集与全业务
        List<Long> fullAppIds = applicationService.getFullAppIds(appId);
        // 2.再找对应的白名单
        return whiteIPRecordDAO.getWhiteIPActionScopes(dslContext, fullAppIds, ip, cloudAreaId);
    }

    @Override
    public List<ServiceWhiteIPInfo> listWhiteIPInfos() {
        List<ServiceWhiteIPInfo> resultList = new ArrayList<>();
        // 1.查出所有Record
        List<Long> recordIdList = whiteIPRecordDAO.listAllWhiteIPRecordId(dslContext);
        for (Long id : recordIdList) {
            WhiteIPRecordDTO whiteIPRecordDTO = whiteIPRecordDAO.getWhiteIPRecordById(dslContext, id);
            if (whiteIPRecordDTO == null) {
                continue;
            }
            List<Long> appIdList = whiteIPRecordDTO.getAppIdList();
            if (appIdList.size() == 1) {
                ApplicationInfoDTO applicationInfoDTO = applicationService.getAppInfoById(appIdList.get(0));
                if (applicationInfoDTO != null && applicationInfoDTO.getAppType() == AppTypeEnum.ALL_APP) {
                    List<WhiteIPIPDTO> whiteIPIPDTOList = whiteIPRecordDTO.getIpList();
                    whiteIPIPDTOList.forEach(whiteIPIPDTO -> {
                        ServiceWhiteIPInfo serviceWhiteIPInfo = new ServiceWhiteIPInfo();
                        serviceWhiteIPInfo.setForAllApp(true);
                        List<String> allAppActionScopeList = new ArrayList<>();
                        for (WhiteIPActionScopeDTO actionScope : whiteIPRecordDTO.getActionScopeList()) {
                            ActionScopeDTO actionScopeDTO = actionScopeDAO.getActionScopeById(
                                actionScope.getActionScopeId());
                            if (actionScopeDTO == null) {
                                log.error("Cannot find actionScopeDTO by id {}", actionScope.getActionScopeId());
                            } else {
                                allAppActionScopeList.add(actionScopeDTO.getCode());
                            }
                        }
                        serviceWhiteIPInfo.setAllAppActionScopeList(allAppActionScopeList);
                        serviceWhiteIPInfo.setAppIdActionScopeMap(new HashMap<>());
                        serviceWhiteIPInfo.setCloudId(whiteIPIPDTO.getCloudAreaId());
                        serviceWhiteIPInfo.setIp(whiteIPIPDTO.getIp());
                        resultList.add(serviceWhiteIPInfo);
                    });
                } else {
                    genNormalAppWhiteIPInfo(whiteIPRecordDTO, resultList);
                }
            } else {
                genNormalAppWhiteIPInfo(whiteIPRecordDTO, resultList);
            }
        }
        log.debug(String.format("WhiteIPInfos before merge:%s", resultList));
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
        log.debug(String.format("WhiteIPInfos after merge:%s", resultList));
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

    private void genNormalAppWhiteIPInfo(WhiteIPRecordDTO whiteIPRecordDTO, List<ServiceWhiteIPInfo> resultList) {
        List<Long> appIdList = whiteIPRecordDTO.getAppIdList();
        List<WhiteIPIPDTO> whiteIPIPDTOList = whiteIPRecordDTO.getIpList();
        for (WhiteIPIPDTO whiteIPIPDTO : whiteIPIPDTOList) {
            ServiceWhiteIPInfo serviceWhiteIPInfo = new ServiceWhiteIPInfo();
            serviceWhiteIPInfo.setForAllApp(false);
            serviceWhiteIPInfo.setAllAppActionScopeList(new ArrayList<>());
            serviceWhiteIPInfo.setCloudId(whiteIPIPDTO.getCloudAreaId());
            serviceWhiteIPInfo.setIp(whiteIPIPDTO.getIp());
            HashMap<Long, List<String>> map = new HashMap<>();
            List<String> actionScopeList = new ArrayList<>();
            whiteIPRecordDTO.getActionScopeList().forEach(actionScope -> {
                ActionScopeDTO actionScopeDTO = actionScopeDAO.getActionScopeById(
                    actionScope.getActionScopeId());
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
