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
import com.tencent.bk.job.manage.model.inner.ServiceAppBaseInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByIpReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/service/app")
@Api(tags = {"job-manage:service:App_Management"})
@RestController
@InternalAPI
public interface ServiceApplicationResource {
    /**
     * 查询CMDB中的常规业务列表
     *
     * @return
     */
    @RequestMapping("/list/normal")
    InternalResponse<List<ServiceAppBaseInfoDTO>> listNormalApps();

    /**
     * 根据业务id查询业务
     *
     * @param appId
     * @return
     */
    @RequestMapping("/{appId}")
    ServiceApplicationDTO queryAppById(
        @PathVariable("appId") Long appId
    );

    @GetMapping("/{appId}/permission")
    InternalResponse<Boolean> checkAppPermission(
        @PathVariable("appId") Long appId,
        @RequestParam("username") String username
    );

    @ApiOperation(value = "获取业务列表", produces = "application/json")
    @GetMapping("/list")
    InternalResponse<List<ServiceApplicationDTO>> listLocalDBApps(
        @RequestParam(value = "appType", required = false) Integer appType
    );

    @ApiOperation(value = "查询业务下主机是否存在", produces = "application/json")
    @GetMapping("/{appId}/host/exists/{ip}")
    InternalResponse<Boolean> existsHost(
        @PathVariable("appId") Long appId,
        @PathVariable("ip") String ip
    );

    @ApiOperation(value = "查询节点下的主机状态", produces = "application/json")
    @PostMapping("/{appId}/host/status/nodes")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByNode(
        @PathVariable("appId") Long appId,
        @RequestHeader("username") String username,
        @RequestBody ServiceGetHostStatusByNodeReq req
    );

    @ApiOperation(value = "查询动态分组下的主机状态", produces = "application/json")
    @PostMapping("/{appId}/host/status/dynamicGroups")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByDynamicGroup(
        @PathVariable("appId") Long appId,
        @RequestHeader("username") String username,
        @RequestBody ServiceGetHostStatusByDynamicGroupReq req
    );

    @ApiOperation(value = "查询IP对应的主机状态", produces = "application/json")
    @PostMapping("/{appId}/host/status/ips")
    InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByIp(
        @PathVariable("appId") Long appId,
        @RequestHeader("username") String username,
        @RequestBody ServiceGetHostStatusByIpReq req
    );
}
