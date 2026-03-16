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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 同步业务、主机资源服务
 */
@Tag(name = "job-manage:api:Sync-OP")
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
    @Operation(summary = "根据业务ID获取主机")
    InternalResponse<List<ServiceHostInfoDTO>> getHostByAppId(
        @Parameter(description = "业务ID", required = true)
        @PathVariable("appId") Long appId);

    /**
     * 同步所有租户下的所有业务、业务集等
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncApp")
    @Operation(summary = "同步所有租户下的业务、业务集等")
    InternalResponse<Boolean> syncApp();

    /**
     * 触发指定租户下的所有业务的主机同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncHost")
    @Operation(summary = "触发指定租户下的所有业务的主机同步")
    InternalResponse<Void> syncHost(@Parameter(description = "租户ID")
                                    @RequestHeader(JobCommonHeaders.BK_TENANT_ID) String tenantId);

    /**
     * 同步某业务的主机
     *
     * @param bizId 业务ID
     * @return 是否操作成功
     */
    @PutMapping("/syncHost/biz/{bizId}")
    @Operation(summary = "根据业务ID同步主机")
    InternalResponse<Boolean> syncHostByBizId(@Parameter(description = "业务ID", required = true)
                                              @PathVariable("bizId") Long bizId);

    /**
     * 开启业务（集）同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncApp/enable")
    @Operation(summary = "开启业务同步")
    InternalResponse<Boolean> enableSyncApp();

    /**
     * 关闭业务（集）同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncApp/disable")
    @Operation(summary = "关闭业务同步")
    InternalResponse<Boolean> disableSyncApp();

    /**
     * 开启主机同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncHost/enable")
    @Operation(summary = "开启主机同步")
    InternalResponse<Boolean> enableSyncHost();

    /**
     * 关闭主机同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncHost/disable")
    @Operation(summary = "关闭主机同步")
    InternalResponse<Boolean> disableSyncHost();

    /**
     * 开启主机状态同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncAgentStatus/enable")
    @Operation(summary = "开启主机状态同步")
    InternalResponse<Boolean> enableSyncAgentStatus();

    /**
     * 关闭主机状态同步
     *
     * @return 是否操作成功
     */
    @PutMapping("/syncAgentStatus/disable")
    @Operation(summary = "关闭主机状态同步")
    InternalResponse<Boolean> disableSyncAgentStatus();
}
