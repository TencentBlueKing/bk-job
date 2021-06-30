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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.api.web.WebServiceInfoResource;
import com.tencent.bk.job.manage.model.web.vo.serviceinfo.ServiceInfoVO;
import com.tencent.bk.job.manage.model.web.vo.serviceinfo.ServiceInstanceInfoVO;
import com.tencent.bk.job.manage.service.ServiceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebServiceInfoResourceImpl implements WebServiceInfoResource {

    private final WebAuthService authService;
    private final ServiceInfoService serviceInfoService;

    @Autowired
    public WebServiceInfoResourceImpl(WebAuthService authService, ServiceInfoService serviceInfoService) {
        this.authService = authService;
        this.serviceInfoService = serviceInfoService;
    }

    private List<ServiceInstanceInfoVO> fakeServiceInstanceInfo(String serviceName) {
        List<ServiceInstanceInfoVO> serviceInstanceInfoVOList = new ArrayList<>();
        if (serviceName.equals("job-manage")) {
            ServiceInstanceInfoVO serviceInstanceInfoVO = new ServiceInstanceInfoVO();
            serviceInstanceInfoVO.setIp("192.168.1.1");
            serviceInstanceInfoVO.setPort(8080);
            serviceInstanceInfoVO.setName(serviceName + "-192-168-01-01-8080");
            serviceInstanceInfoVO.setStatus((byte) 1);
            serviceInstanceInfoVO.setStatusMessage("OK");
            serviceInstanceInfoVO.setVersion("3.2.7.0");
            serviceInstanceInfoVOList.add(serviceInstanceInfoVO);
            serviceInstanceInfoVO = new ServiceInstanceInfoVO();
            serviceInstanceInfoVO.setIp("192.168.1.2");
            serviceInstanceInfoVO.setPort(8080);
            serviceInstanceInfoVO.setName(serviceName + "-192-168-01-02-8080");
            serviceInstanceInfoVO.setStatus((byte) 1);
            serviceInstanceInfoVO.setStatusMessage("OK");
            serviceInstanceInfoVO.setVersion("3.2.7.0");
            serviceInstanceInfoVOList.add(serviceInstanceInfoVO);
            serviceInstanceInfoVO.setIp("192.168.1.3");
            serviceInstanceInfoVO.setPort(8080);
            serviceInstanceInfoVO.setName(serviceName + "-192-168-01-03-8080");
            serviceInstanceInfoVO.setStatus((byte) 1);
            serviceInstanceInfoVO.setStatusMessage("OK");
            serviceInstanceInfoVO.setVersion("3.2.7.0");
            serviceInstanceInfoVOList.add(serviceInstanceInfoVO);
        } else if (serviceName.equals("job-execute")) {
            ServiceInstanceInfoVO serviceInstanceInfoVO = new ServiceInstanceInfoVO();
            serviceInstanceInfoVO.setIp("192.168.1.1");
            serviceInstanceInfoVO.setPort(8081);
            serviceInstanceInfoVO.setName(serviceName + "-192-168-01-01-8081");
            serviceInstanceInfoVO.setStatus((byte) 1);
            serviceInstanceInfoVO.setStatusMessage("OK");
            serviceInstanceInfoVO.setVersion("3.2.7.1");
            serviceInstanceInfoVOList.add(serviceInstanceInfoVO);
            serviceInstanceInfoVO = new ServiceInstanceInfoVO();
            serviceInstanceInfoVO.setIp("192.168.1.2");
            serviceInstanceInfoVO.setPort(8081);
            serviceInstanceInfoVO.setName(serviceName + "-192-168-01-02-8081");
            serviceInstanceInfoVO.setStatus((byte) 0);
            serviceInstanceInfoVO.setStatusMessage("SERVICE UNAVAILABLE(503)");
            serviceInstanceInfoVO.setVersion("3.2.7.0");
            serviceInstanceInfoVOList.add(serviceInstanceInfoVO);
            serviceInstanceInfoVO = new ServiceInstanceInfoVO();
            serviceInstanceInfoVO.setIp("192.168.1.3");
            serviceInstanceInfoVO.setPort(8081);
            serviceInstanceInfoVO.setName(serviceName + "-192-168-01-03-8081");
            serviceInstanceInfoVO.setStatus((byte) -1);
            serviceInstanceInfoVO.setStatusMessage("NO MAPPING");
            serviceInstanceInfoVO.setVersion("3.2.7.0");
            serviceInstanceInfoVOList.add(serviceInstanceInfoVO);
        }
        return serviceInstanceInfoVOList;
    }

    private List<ServiceInfoVO> fakeServiceInfo() {
        List<ServiceInfoVO> serviceInfoVOList = new ArrayList<>();
        ServiceInfoVO serviceInfoVO = new ServiceInfoVO();
        serviceInfoVO.setName("job-manage");
        serviceInfoVO.setVersionConsistent(true);
        serviceInfoVO.setInstanceList(fakeServiceInstanceInfo("job-manage"));
        serviceInfoVO.setVersion(serviceInfoVO.getInstanceList().get(0).getVersion());
        serviceInfoVOList.add(serviceInfoVO);
        serviceInfoVO = new ServiceInfoVO();
        serviceInfoVO.setName("job-execute");
        serviceInfoVO.setVersionConsistent(false);
        serviceInfoVO.setInstanceList(fakeServiceInstanceInfo("job-execute"));
        serviceInfoVOList.add(serviceInfoVO);
        return serviceInfoVOList;
    }

    @Override
    public ServiceResponse<List<ServiceInfoVO>> listServiceInfo(String username) {
        return ServiceResponse.buildSuccessResp(serviceInfoService.listServiceInfo());
    }
}
