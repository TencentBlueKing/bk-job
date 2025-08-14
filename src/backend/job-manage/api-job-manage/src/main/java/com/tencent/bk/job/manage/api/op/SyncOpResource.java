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

package com.tencent.bk.job.manage.api.op;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 同步业务、主机资源服务
 */
@Api(tags = {"job-manage:api:Sync-OP"})
@RequestMapping("/op/sync")
@InternalAPI
public interface SyncOpResource {

    /**
     * 根据账号id获取账号信息
     *
     * @param appId 业务ID
     * @return 主机列表数据
     */
    @GetMapping("/host/app/{appId}")
    @ApiOperation(value = "根据业务ID获取主机", produces = "application/json")
    InternalResponse<List<ServiceHostInfoDTO>> getHostByAppId(
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId);

    /**
     * 同步所有租户下的所有业务、业务集等
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncApp")
    @ApiOperation(value = "同步所有租户下的业务、业务集等", produces = "application/json")
    InternalResponse<Boolean> syncApp();

    /**
     * 触发指定租户下的所有业务的主机同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncHost")
    @ApiOperation(value = "触发指定租户下的所有业务的主机同步", produces = "application/json")
    InternalResponse<Void> syncHost(@ApiParam("租户ID")
                                    @RequestHeader(JobCommonHeaders.BK_TENANT_ID) String tenantId);

    /**
     * 同步某业务的主机
     *
     * @param bizId 业务ID
     * @return 是否操作成功
     */
    @PutMapping("/syncHost/biz/{bizId}")
    @ApiOperation(value = "根据业务ID同步主机", produces = "application/json")
    InternalResponse<Boolean> syncHostByBizId(@ApiParam(value = "业务ID", required = true)
                                              @PathVariable("bizId") Long bizId);

    /**
     * 开启业务事件监听
     *
     * @return 是否操作成功
     */
    @PutMapping("/bizWatch/enable")
    @ApiOperation(value = "开启业务事件监听", produces = "application/json")
    InternalResponse<Boolean> enableBizWatch();

    /**
     * 关闭业务事件监听
     *
     * @return 是否操作成功
     */
    @PutMapping("/bizWatch/disable")
    @ApiOperation(value = "关闭业务事件监听", produces = "application/json")
    InternalResponse<Boolean> disableBizWatch();

    /**
     * 开启主机事件监听
     *
     * @return 是否操作成功
     */
    @PutMapping("/hostWatch/enable")
    @ApiOperation(value = "开启主机事件监听", produces = "application/json")
    InternalResponse<Boolean> enableHostWatch();

    /**
     * 关闭主机事件监听
     *
     * @return 是否操作成功
     */
    @PutMapping("/hostWatch/disable")
    @ApiOperation(value = "关闭主机事件监听", produces = "application/json")
    InternalResponse<Boolean> disableHostWatch();

    /**
     * 开启业务（集）同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncApp/enable")
    @ApiOperation(value = "开启业务同步", produces = "application/json")
    InternalResponse<Boolean> enableSyncApp();

    /**
     * 关闭业务（集）同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncApp/disable")
    @ApiOperation(value = "关闭业务同步", produces = "application/json")
    InternalResponse<Boolean> disableSyncApp();

    /**
     * 开启主机同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncHost/enable")
    @ApiOperation(value = "开启主机同步", produces = "application/json")
    InternalResponse<Boolean> enableSyncHost();

    /**
     * 关闭主机同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncHost/disable")
    @ApiOperation(value = "关闭主机同步", produces = "application/json")
    InternalResponse<Boolean> disableSyncHost();

    /**
     * 开启主机状态同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncAgentStatus/enable")
    @ApiOperation(value = "开启主机状态同步", produces = "application/json")
    InternalResponse<Boolean> enableSyncAgentStatus();

    /**
     * 关闭主机状态同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncAgentStatus/disable")
    @ApiOperation(value = "关闭主机状态同步", produces = "application/json")
    InternalResponse<Boolean> disableSyncAgentStatus();
}
