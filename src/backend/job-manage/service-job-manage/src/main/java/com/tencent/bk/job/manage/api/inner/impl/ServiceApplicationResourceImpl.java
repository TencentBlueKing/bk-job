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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.model.inner.ServiceAppBaseInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByIpReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceApplicationResourceImpl implements ServiceApplicationResource {
    private final ApplicationService applicationService;
    private final ApplicationInfoDAO applicationInfoDAO;

    @Autowired
    public ServiceApplicationResourceImpl(ApplicationService applicationService,
                                          ApplicationInfoDAO applicationInfoDAO) {
        this.applicationService = applicationService;
        this.applicationInfoDAO = applicationInfoDAO;
    }

    @Override
    public InternalResponse<List<ServiceAppBaseInfoDTO>> listNormalApps() {
        List<ApplicationInfoDTO> applicationInfoDTOList = applicationInfoDAO.listAppInfoByType(AppTypeEnum.NORMAL);
        List<ServiceAppBaseInfoDTO> resultList =
            applicationInfoDTOList.parallelStream().map(this::convertToServiceAppBaseInfo).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(resultList);
    }

    @Override
    public ServiceApplicationDTO queryAppById(Long appId) {
        ApplicationInfoDTO appInfo = applicationService.getAppInfoById(appId);
        if (appInfo == null) {
            return null;
        }
        return convertToServiceApp(appInfo);
    }

    private ServiceApplicationDTO convertToServiceApp(ApplicationInfoDTO appInfo) {
        ServiceApplicationDTO app = new ServiceApplicationDTO();
        app.setId(appInfo.getId());
        app.setName(appInfo.getName());
        app.setAppType(appInfo.getAppType().getValue());
        app.setSubAppIds(appInfo.getSubAppIds());
        app.setMaintainers(appInfo.getMaintainers());
        app.setOwner(appInfo.getBkSupplierAccount());
        app.setOperateDeptId(appInfo.getOperateDeptId());
        app.setTimeZone(appInfo.getTimeZone());
        app.setLanguage(appInfo.getLanguage());
        return app;
    }

    private ServiceAppBaseInfoDTO convertToServiceAppBaseInfo(ApplicationInfoDTO appInfo) {
        ServiceAppBaseInfoDTO appBaseInfoDTO = new ServiceAppBaseInfoDTO();
        appBaseInfoDTO.setId(appInfo.getId());
        appBaseInfoDTO.setName(appInfo.getName());
        return appBaseInfoDTO;
    }

    @Override
    public InternalResponse<Boolean> checkAppPermission(Long appId, String username) {
        if (appId == null || appId < 0) {
            return InternalResponse.buildSuccessResp(false);
        }
        return InternalResponse.buildSuccessResp(applicationService.checkAppPermission(appId, username));
    }

    @Override
    public InternalResponse<List<ServiceApplicationDTO>> listLocalDBApps(Integer appType) {
        List<ApplicationInfoDTO> applicationInfoDTOList;
        if (appType == null || appType <= 0) {
            applicationInfoDTOList = applicationInfoDAO.listAppInfo();
        } else {
            applicationInfoDTOList = applicationInfoDAO.listAppInfoByType(AppTypeEnum.valueOf(appType));
        }
        List<ServiceApplicationDTO> resultList =
            applicationInfoDTOList.parallelStream().map(this::convertToServiceApp).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(resultList);
    }

    @Override
    public InternalResponse<Boolean> existsHost(Long appId, String ip) {
        return InternalResponse.buildSuccessResp(applicationService.existsHost(appId, ip));
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByNode(
        Long appId,
        String username,
        ServiceGetHostStatusByNodeReq req
    ) {
        List<AppTopologyTreeNode> treeNodeList = req.getTreeNodeList();
        List<NodeInfoVO> nodeInfoVOList = applicationService.getHostsByNode(username, appId, treeNodeList);
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        nodeInfoVOList.parallelStream().forEach(nodeInfoVO -> {
            nodeInfoVO.getIpListStatus().forEach(hostInfoVO -> {
                ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
                serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
                serviceHostStatusDTO.setIp(hostInfoVO.getIp());
                serviceHostStatusDTO.setAlive(hostInfoVO.getAlive());
                if (!hostStatusDTOList.contains(serviceHostStatusDTO)) {
                    hostStatusDTOList.add(serviceHostStatusDTO);
                }
            });
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByDynamicGroup(
        Long appId,
        String username,
        ServiceGetHostStatusByDynamicGroupReq req
    ) {
        List<String> dynamicGroupIdList = req.getDynamicGroupIdList();
        List<DynamicGroupInfoDTO> dynamicGroupInfoDTOList = applicationService.getDynamicGroupHostList(
            username,
            appId,
            dynamicGroupIdList
        );
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        dynamicGroupInfoDTOList.parallelStream().forEach(dynamicGroupInfoDTO -> {
            dynamicGroupInfoDTO.getIpListStatus().forEach(hostInfoVO -> {
                ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
                serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
                serviceHostStatusDTO.setIp(hostInfoVO.getIp());
                serviceHostStatusDTO.setAlive(hostInfoVO.getGseAgentAlive() ? 1 : 0);
                if (!hostStatusDTOList.contains(serviceHostStatusDTO)) {
                    hostStatusDTOList.add(serviceHostStatusDTO);
                }
            });
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByIp(Long appId, String username,
                                                                     ServiceGetHostStatusByIpReq req) {
        List<String> ipList = req.getIpList();
        List<HostInfoVO> hostInfoVOList = applicationService.getHostsByIp(username, appId, null, ipList);
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        hostInfoVOList.forEach(hostInfoVO -> {
            ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
            serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
            serviceHostStatusDTO.setIp(hostInfoVO.getCloudAreaInfo().getId() + ":" + hostInfoVO.getIp());
            serviceHostStatusDTO.setAlive(hostInfoVO.getAlive());
            if (!hostStatusDTOList.contains(serviceHostStatusDTO)) {
                hostStatusDTOList.add(serviceHostStatusDTO);
            }
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }
}
