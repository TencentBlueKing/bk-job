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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetAppHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByHostReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostsByCloudIpv6Req;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Api(tags = {"job-manage:service:Host_Management"})
@SmartFeignClient(value = "job-manage", contextId = "hostResource")
@InternalAPI
public interface ServiceHostResource {

    @ApiOperation(value = "查询节点下的主机状态", produces = "application/json")
    @PostMapping("/service/app/{appId}/host/status/nodes")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByNode(
        @PathVariable("appId") Long appId,
        @RequestBody ServiceGetHostStatusByNodeReq req
    );

    @ApiOperation(value = "查询动态分组下的主机状态", produces = "application/json")
    @PostMapping("/service/app/{appId}/host/status/dynamicGroups")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByDynamicGroup(
        @PathVariable("appId") Long appId,
        @RequestBody ServiceGetHostStatusByDynamicGroupReq req
    );

    @ApiOperation(value = "查询主机对应的主机状态", produces = "application/json")
    @PostMapping("/service/app/{appId}/host/status/hosts")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByHost(
        @PathVariable("appId") Long appId,
        @RequestBody ServiceGetHostStatusByHostReq req
    );

    /**
     * 查询业务下的主机
     *
     * @param appId Job业务ID
     * @param req   请求
     */
    @ApiOperation(value = "查询业务下的主机", produces = "application/json")
    @PostMapping("/service/app/{appId}/host/batchGet")
    InternalResponse<ServiceListAppHostResultDTO> batchGetAppHosts(
        @PathVariable("appId") Long appId,
        @RequestBody ServiceBatchGetAppHostsReq req
    );

    /**
     * 批量获取主机信息
     *
     * @param req 请求
     * @return 主机信息
     */
    @ApiOperation(value = "检查主机是否在业务下", produces = "application/json")
    @PostMapping("/service/hosts/batchGet")
    InternalResponse<List<ServiceHostDTO>> batchGetHosts(
        @RequestBody
            ServiceBatchGetHostsReq req);

    /**
     * 通过云区域ID与Ipv6地址获取主机信息
     *
     * @param req 请求
     * @return 主机信息
     */
    @ApiOperation(value = "通过云区域ID与Ipv6地址查询主机信息", produces = "application/json")
    @PostMapping("/service/hosts/getByCloudIpv6")
    InternalResponse<List<ServiceHostDTO>> getHostsByCloudIpv6(
        @RequestBody
            ServiceGetHostsByCloudIpv6Req req);
}
