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

import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.common.constant.JobDiscoveryConsts;
import com.tencent.bk.job.common.web.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.manage.model.web.vo.serviceinfo.ServiceInfoVO;
import com.tencent.bk.job.manage.model.web.vo.serviceinfo.ServiceInstanceInfoVO;
import com.tencent.bk.job.manage.service.ServiceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServiceInfoServiceImpl implements ServiceInfoService {

    public static final Map<String, Byte> statusMap = new HashMap<>();

    static {
        statusMap.put(Status.UP.getCode(), (byte) 1);
        statusMap.put(Status.DOWN.getCode(), (byte) 0);
        statusMap.put(Status.OUT_OF_SERVICE.getCode(), (byte) 0);
        statusMap.put(Status.UNKNOWN.getCode(), (byte) -1);
    }

    private DiscoveryClient discoveryClient;

    @Autowired
    public ServiceInfoServiceImpl(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    private static ServiceInstanceInfoVO convert(ServiceInstanceInfoDTO serviceInstanceInfoDTO) {
        if (serviceInstanceInfoDTO == null) return null;
        ServiceInstanceInfoVO serviceInstanceInfoVO = new ServiceInstanceInfoVO();
        serviceInstanceInfoVO.setName(serviceInstanceInfoDTO.getName());
        serviceInstanceInfoVO.setVersion(serviceInstanceInfoDTO.getVersion());
        serviceInstanceInfoVO.setIp(serviceInstanceInfoDTO.getIp());
        serviceInstanceInfoVO.setPort(serviceInstanceInfoDTO.getPort());
        serviceInstanceInfoVO.setStatus(statusMap.get(serviceInstanceInfoDTO.getStatusCode()));
        serviceInstanceInfoVO.setStatusMessage(serviceInstanceInfoDTO.getStatusMessage());
        return serviceInstanceInfoVO;
    }

    private boolean checkNodeStatus(String nodeName) {
        return true;
    }

    private void fillVersionAndStatus(ServiceInstanceInfoDTO serviceInstanceInfoDTO) {
        serviceInstanceInfoDTO.setVersion("not implemented");
    }

    private List<ServiceInstanceInfoDTO> listServiceInstanceInfoDTO() {
        List<String> serviceIdList = discoveryClient.getServices();
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (String serviceId : serviceIdList) {
            serviceInstanceList.addAll(discoveryClient.getInstances(serviceId));
        }
        return serviceInstanceList.parallelStream().filter(serviceInstance -> {
            if (serviceInstance.getServiceId().equals("job-gateway-management")) {
                return false;
            } else {
                Map<String, String> tagMap = serviceInstance.getMetadata();
                return tagMap.containsKey(JobDiscoveryConsts.TAG_KEY_TYPE)
                    && JobDiscoveryConsts.TAG_VALUE_TYPE_JOB_BACKEND_SERVICE
                    .equals(tagMap.get(JobDiscoveryConsts.TAG_KEY_TYPE))
                    && serviceInstance.getServiceId().startsWith("job");
            }
        }).map(serviceInstance -> {
            ServiceInstanceInfoDTO serviceInstanceInfoDTO = new ServiceInstanceInfoDTO();
            serviceInstanceInfoDTO.setServiceName(serviceInstance.getServiceId());
            serviceInstanceInfoDTO.setName(serviceInstance.getInstanceId());
            serviceInstanceInfoDTO.setIp(serviceInstance.getHost());
            serviceInstanceInfoDTO.setPort(serviceInstance.getPort());
            fillVersionAndStatus(serviceInstanceInfoDTO);
            return serviceInstanceInfoDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ServiceInfoVO> listServiceInfo() {
        List<ServiceInstanceInfoDTO> serviceInstanceInfoDTOList = listServiceInstanceInfoDTO();
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
