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

package com.tencent.bk.job.common.k8s.provider;

import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class K8SServiceInfoProvider implements ServiceInfoProvider {

    public final String KEY_JOB_MS_VERSION = "bk.job.image/tag";
    private final DiscoveryClient discoveryClient;

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        log.debug("K8sServiceInfoServiceImpl inited");
    }

    private void fillVersionAndStatus(
        ServiceInstance serviceInstance,
        ServiceInstanceInfoDTO serviceInstanceInfoDTO
    ) {
        Map<String, String> metaData = serviceInstance.getMetadata();
        if (metaData == null) {
            log.warn("No metadata found in serviceInstance {}", serviceInstance);
            return;
        }
        String version = metaData.getOrDefault(KEY_JOB_MS_VERSION, "Unknown");
        serviceInstanceInfoDTO.setVersion(version);
        // TODO:通过K8s API查询POD状态
        serviceInstanceInfoDTO.setStatusCode((byte) 1);
        serviceInstanceInfoDTO.setStatusMessage("");
    }

    @Override
    public List<ServiceInstanceInfoDTO> listServiceInfo() {
        List<String> serviceIdList = discoveryClient.getServices();
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (String serviceId : serviceIdList) {
            serviceInstanceList.addAll(discoveryClient.getInstances(serviceId));
        }
        for (ServiceInstance serviceInstance : serviceInstanceList) {
            log.debug("serviceInstance={}", JsonUtils.toJson(serviceInstance));
        }
        return serviceInstanceList.parallelStream().filter(serviceInstance -> {
            if (serviceInstance.getServiceId().equals("job-gateway-management")) {
                return false;
            } else {
                Map<String, String> metaData = serviceInstance.getMetadata();
                log.debug("metaData={}", JsonUtils.toJson(metaData));
                return serviceInstance.getServiceId().startsWith("job");
            }
        }).map(serviceInstance -> {
            ServiceInstanceInfoDTO serviceInstanceInfoDTO = new ServiceInstanceInfoDTO();
            serviceInstanceInfoDTO.setServiceName(serviceInstance.getServiceId());
            serviceInstanceInfoDTO.setName(serviceInstance.getInstanceId());
            serviceInstanceInfoDTO.setIp(serviceInstance.getHost());
            serviceInstanceInfoDTO.setPort(serviceInstance.getPort());
            fillVersionAndStatus(serviceInstance, serviceInstanceInfoDTO);
            return serviceInstanceInfoDTO;
        }).collect(Collectors.toList());
    }

}
