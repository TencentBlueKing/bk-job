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

package com.tencent.bk.job.common.consul.provider;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthChecksForServiceRequest;
import com.ecwid.consul.v1.health.model.Check;
import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecwid.consul.v1.health.model.Check.CheckStatus.CRITICAL;
import static org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE;
import static org.springframework.boot.actuate.health.Status.UNKNOWN;
import static org.springframework.boot.actuate.health.Status.UP;

@Slf4j
@Component
public class ConsulServiceInfoProvider implements ServiceInfoProvider {

    public final String KEY_WORD_MAINTENANCE = "maintenance";
    public static final Map<String, Byte> statusMap = new HashMap<>();

    static {
        statusMap.put(Status.UP.getCode(), ServiceInstanceInfoDTO.STATUS_OK);
        statusMap.put(Status.DOWN.getCode(), ServiceInstanceInfoDTO.STATUS_ERROR);
        statusMap.put(Status.OUT_OF_SERVICE.getCode(), ServiceInstanceInfoDTO.STATUS_ERROR);
        statusMap.put(Status.UNKNOWN.getCode(), ServiceInstanceInfoDTO.STATUS_UNKNOWN);
    }

    private final DiscoveryClient discoveryClient;

    @Autowired
    public ConsulServiceInfoProvider(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        log.debug("ConsulServiceInfoServiceImpl inited");
    }

    private ConsulClient consulClient;
    private boolean consulInited = false;

    public void initConsul() {
        this.consulClient = ApplicationContextRegister.getBean(ConsulClient.class);
        consulInited = true;
    }

    private boolean checkNodeStatus(String nodeName) {
        Response<List<Check>> response = consulClient.getHealthChecksForNode(nodeName, QueryParams.DEFAULT);
        List<Check> checks = response.getValue();
        for (Check check : checks) {
            if (StringUtils.isBlank(check.getServiceId())
                && (check.getCheckId().contains(KEY_WORD_MAINTENANCE)
                || check.getName().toLowerCase().contains(KEY_WORD_MAINTENANCE)
                || check.getStatus() == CRITICAL)) {
                return false;
            }
        }
        return true;
    }

    private void fillVersionAndStatus(ServiceInstanceInfoDTO serviceInstanceInfoDTO) {
        HealthChecksForServiceRequest request = HealthChecksForServiceRequest.newBuilder()
            .setQueryParams(QueryParams.DEFAULT)
            .build();
        Response<List<Check>> response =
            consulClient.getHealthChecksForService(serviceInstanceInfoDTO.getServiceName(), request);
        List<Check> checks = response.getValue();
        Check targetCheck = null;
        for (Check check : checks) {
            if (check.getServiceId().equals(serviceInstanceInfoDTO.getName())) {
                targetCheck = check;
                break;
            }
        }
        if (targetCheck == null) {
            serviceInstanceInfoDTO.setStatusCode(statusMap.get(UNKNOWN.getCode()));
            return;
        } else if (targetCheck.getName().toLowerCase().contains(KEY_WORD_MAINTENANCE)) {
            serviceInstanceInfoDTO.setStatusCode(statusMap.get(OUT_OF_SERVICE.getCode()));
            serviceInstanceInfoDTO.setStatusMessage(targetCheck.getNotes());
        } else {
            if (checkNodeStatus(targetCheck.getNode())) {
                serviceInstanceInfoDTO.setStatusCode(statusMap.get(UP.getCode()));
                serviceInstanceInfoDTO.setStatusMessage(targetCheck.getNotes());
            } else {
                serviceInstanceInfoDTO.setStatusCode(statusMap.get(OUT_OF_SERVICE.getCode()));
                serviceInstanceInfoDTO.setStatusMessage("Node " + targetCheck.getNode() + " not healthy");
            }
        }
        List<String> tagList = targetCheck.getServiceTags();
        for (String tag : tagList) {
            if (tag.startsWith("version=")) {
                serviceInstanceInfoDTO.setVersion(StringUtils.removeStart(tag, "version="));
            }
        }
    }

    @Override
    public List<ServiceInstanceInfoDTO> listServiceInfo() {
        if (!consulInited) initConsul();
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
            fillVersionAndStatus(serviceInstanceInfoDTO);
            return serviceInstanceInfoDTO;
        }).collect(Collectors.toList());
    }

}
