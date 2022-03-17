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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceCheckAppHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByIpReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CompatibleImplementation(explain = "部分host相关的API需要修改Path，后续版本需移除service/app这个路径", version = "3.5.1")
@RequestMapping("/service")
@Api(tags = {"job-manage:service:Host_Management"})
@RestController
@InternalAPI
public interface ServiceHostResource {

    @ApiOperation(value = "查询节点下的主机状态", produces = "application/json")
    @PostMapping("/app/{appId}/host/status/nodes")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByNode(
        @PathVariable("appId") Long appId,
        @RequestHeader("username") String username,
        @RequestBody ServiceGetHostStatusByNodeReq req
    );

    @ApiOperation(value = "查询动态分组下的主机状态", produces = "application/json")
    @PostMapping("/app/{appId}/host/status/dynamicGroups")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByDynamicGroup(
        @PathVariable("appId") Long appId,
        @RequestHeader("username") String username,
        @RequestBody ServiceGetHostStatusByDynamicGroupReq req
    );

    @ApiOperation(value = "查询IP对应的主机状态", produces = "application/json")
    @PostMapping("/app/{appId}/host/status/ips")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByIp(
        @PathVariable("appId") Long appId,
        @RequestHeader("username") String username,
        @RequestBody ServiceGetHostStatusByIpReq req
    );

    /**
     * 检查主机是否在业务下
     *
     * @param appId Job业务ID
     * @param req   请求
     * @return 非法的主机
     */
    @ApiOperation(value = "检查主机是否在业务下", produces = "application/json")
    @PostMapping("/app/{appId}/host/checkAppHosts")
    InternalResponse<List<IpDTO>> checkAppHosts(
        @PathVariable("appId") Long appId,
        @RequestBody ServiceCheckAppHostsReq req
    );

    /**
     * 批量获取主机信息
     *
     * @param hostIps 主机Ip列表
     * @return 主机信息
     */
    @ApiOperation(value = "检查主机是否在业务下", produces = "application/json")
    @PostMapping("/hosts/batchGet")
    InternalResponse<List<ServiceHostDTO>> batchGetHosts(List<IpDTO> hostIps);
}
