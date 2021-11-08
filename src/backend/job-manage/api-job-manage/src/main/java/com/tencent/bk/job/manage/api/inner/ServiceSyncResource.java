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
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 同步业务、主机资源服务
 */
@RequestMapping("/service/sync")
@Api(tags = {"job-manage:service:App_Management"})
@RestController
@InternalAPI
public interface ServiceSyncResource {
    /**
     * 查询所有业务
     *
     * @return
     */
    @RequestMapping("/app/list")
    List<ServiceApplicationDTO> listAllApps();

    /**
     * 根据账号id获取账号信息
     *
     * @param appId 业务ID
     * @return
     */
    @GetMapping("/host/app/{appId}")
    @ApiOperation(value = "根据业务ID获取主机", produces = "application/json")
    InternalResponse<List<ServiceHostInfoDTO>> getHostByAppId(
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId);

    /**
     * 同步某业务的主机
     *
     * @param appId 业务ID
     * @return
     */
    @PutMapping("/syncHost/app/{appId}")
    @ApiOperation(value = "根据业务ID同步主机", produces = "application/json")
    InternalResponse<Boolean> syncHostByAppId(
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId);

    /**
     * 开启业务事件监听
     *
     * @return
     */
    @PutMapping("/appWatch/enable")
    @ApiOperation(value = "开启业务事件监听", produces = "application/json")
    InternalResponse<Boolean> enableAppWatch();

    /**
     * 关闭业务事件监听
     *
     * @return
     */
    @PutMapping("/appWatch/disable")
    @ApiOperation(value = "关闭业务事件监听", produces = "application/json")
    InternalResponse<Boolean> disableAppWatch();

    /**
     * 开启主机事件监听
     *
     * @return
     */
    @PutMapping("/hostWatch/enable")
    @ApiOperation(value = "开启主机事件监听", produces = "application/json")
    InternalResponse<Boolean> enableHostWatch();

    /**
     * 关闭主机事件监听
     *
     * @return
     */
    @PutMapping("/hostWatch/disable")
    @ApiOperation(value = "关闭主机事件监听", produces = "application/json")
    InternalResponse<Boolean> disableHostWatch();

    /**
     * 开启业务同步
     *
     * @return
     */
    @PutMapping("/syncApp/enable")
    @ApiOperation(value = "开启业务同步", produces = "application/json")
    InternalResponse<Boolean> enableSyncApp();

    /**
     * 关闭业务同步
     *
     * @return
     */
    @PutMapping("/syncApp/disable")
    @ApiOperation(value = "关闭业务同步", produces = "application/json")
    InternalResponse<Boolean> disableSyncApp();

    /**
     * 开启主机同步
     *
     * @return
     */
    @PutMapping("/syncHost/enable")
    @ApiOperation(value = "开启主机同步", produces = "application/json")
    InternalResponse<Boolean> enableSyncHost();

    /**
     * 关闭主机同步
     *
     * @return
     */
    @PutMapping("/syncHost/disable")
    @ApiOperation(value = "关闭主机同步", produces = "application/json")
    InternalResponse<Boolean> disableSyncHost();

    /**
     * 开启主机状态同步
     *
     * @return
     */
    @PutMapping("/syncAgentStatus/enable")
    @ApiOperation(value = "开启主机状态同步", produces = "application/json")
    InternalResponse<Boolean> enableSyncAgentStatus();

    /**
     * 关闭主机状态同步
     *
     * @return
     */
    @PutMapping("/syncAgentStatus/disable")
    @ApiOperation(value = "关闭主机状态同步", produces = "application/json")
    InternalResponse<Boolean> disableSyncAgentStatus();
}
