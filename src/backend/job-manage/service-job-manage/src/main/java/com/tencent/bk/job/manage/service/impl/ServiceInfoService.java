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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ProfileEnum;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.model.web.vo.serviceinfo.ServiceInfoVO;
import com.tencent.bk.job.manage.model.web.vo.serviceinfo.ServiceInstanceInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("!" + ProfileEnum.Constants.TEST)
public class ServiceInfoService {

    private final ServiceInfoProvider serviceInfoProvider;
    private final TenantEnvService tenantEnvService;

    @Autowired
    public ServiceInfoService(ServiceInfoProvider serviceInfoProvider, TenantEnvService tenantEnvService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.tenantEnvService = tenantEnvService;
    }

    /**
     * 服务信息相关接口的租户访问校验：开启多租户时仅允许系统租户访问。
     * 与 Web 端 {@link com.tencent.bk.job.manage.api.web.impl.WebServiceInfoResourceImpl}
     * 中原有的内联实现保持一致，供 Web/ESB 共用，避免逻辑漂移。
     */
    public void checkTenantAccess() {
        if (tenantEnvService.isTenantEnabled()) {
            String tenantId = JobContextUtil.getTenantId();
            if (!TenantIdConstants.SYSTEM_TENANT_ID.equals(tenantId)) {
                throw new NotFoundException(ErrorCode.NOT_SUPPORT_FEATURE);
            }
        }
    }

    private static ServiceInstanceInfoVO convert(ServiceInstanceInfoDTO serviceInstanceInfoDTO) {
        if (serviceInstanceInfoDTO == null) return null;
        ServiceInstanceInfoVO serviceInstanceInfoVO = new ServiceInstanceInfoVO();
        serviceInstanceInfoVO.setName(serviceInstanceInfoDTO.getName());
        serviceInstanceInfoVO.setVersion(serviceInstanceInfoDTO.getVersion());
        serviceInstanceInfoVO.setIp(serviceInstanceInfoDTO.getIp());
        serviceInstanceInfoVO.setPort(serviceInstanceInfoDTO.getPort());
        serviceInstanceInfoVO.setStatus(serviceInstanceInfoDTO.getStatusCode());
        serviceInstanceInfoVO.setStatusMessage(serviceInstanceInfoDTO.getStatusMessage());
        return serviceInstanceInfoVO;
    }

    public List<ServiceInfoVO> listServiceInfo() {
        List<ServiceInstanceInfoDTO> serviceInstanceInfoDTOList = serviceInfoProvider.listServiceInfo();
        // groupBy serviceName
        Map<String, List<ServiceInstanceInfoVO>> map = new HashMap<>();
        for (ServiceInstanceInfoDTO serviceInstanceInfoDTO : serviceInstanceInfoDTOList) {
            String serviceName = serviceInstanceInfoDTO.getServiceName();
            map.putIfAbsent(serviceName, new ArrayList<>());
            map.get(serviceName).add(convert(serviceInstanceInfoDTO));
        }
        List<ServiceInfoVO> resultList = new ArrayList<>();
        for (Map.Entry<String, List<ServiceInstanceInfoVO>> entry : map.entrySet()) {
            String serviceName = entry.getKey();
            List<ServiceInstanceInfoVO> serviceInstanceInfoVOList = entry.getValue();
            ServiceInfoVO serviceInfoVO = new ServiceInfoVO();
            serviceInfoVO.setName(serviceName);
            boolean versionConsistent = true;
            String version = null;
            if (!serviceInstanceInfoDTOList.isEmpty()) {
                version = serviceInstanceInfoVOList.get(0).getVersion();
            }
            if (version != null && serviceInstanceInfoVOList.size() > 1) {
                for (int i = 1; i < serviceInstanceInfoVOList.size(); i++) {
                    if (!version.equals(serviceInstanceInfoVOList.get(i).getVersion())) {
                        versionConsistent = false;
                        version = null;
                        break;
                    }
                }
            }
            serviceInfoVO.setVersionConsistent(versionConsistent);
            serviceInfoVO.setVersion(version);
            serviceInfoVO.setInstanceList(serviceInstanceInfoVOList);
            resultList.add(serviceInfoVO);
        }
        // 服务排序
        resultList.sort((o1, o2) -> CompareUtil.safeCompareNullBack(o1.getName(), o2.getName()));
        return resultList;
    }
}
