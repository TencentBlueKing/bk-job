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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.request.ServiceListAppByAppIdListReq;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Api(tags = {"job-manage:service:App_Management"})
@SmartFeignClient(value = "job-manage", contextId = "applicationResource")
@InternalAPI
public interface ServiceApplicationResource {
    /**
     * 根据业务id查询业务
     * 根据Job业务id查询业务
     *
     * @param appId 业务ID
     * @return 业务
     */
    @ApiOperation("根据Job业务id查询业务")
    @RequestMapping("/service/app/{appId}")
    ServiceApplicationDTO queryAppById(@ApiParam(value = "Job业务ID", required = true)
                                       @PathVariable("appId") Long appId);

    /**
     * 根据Job业务id批量查询业务
     *
     * @param req 请求体
     * @return 业务列表
     */
    @ApiOperation("根据Job业务id批量查询业务")
    @PostMapping("/service/apps")
    List<ServiceApplicationDTO> listAppsByAppIdList(@ApiParam(value = "业务ID列表", required = true)
                                                 @RequestBody ServiceListAppByAppIdListReq req);

    /**
     * 根据资源范围查询业务
     *
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @return 业务
     */
    @ApiOperation("根据资源范围查询业务")
    @RequestMapping("/service/app/scope/{scopeType}/{scopeId}")
    ServiceApplicationDTO queryAppByScope(@ApiParam(value = "资源范围类型", allowableValues = "1-业务,2-业务集", required = true)
                                          @PathVariable("scopeType") String scopeType,
                                          @ApiParam(value = "资源范围ID", required = true)
                                          @PathVariable("scopeId") String scopeId);

    @ApiOperation(value = "获取业务列表", produces = "application/json")
    @GetMapping("/service/app/list")
    InternalResponse<List<ServiceApplicationDTO>> listApps(
        @ApiParam(value = "资源范围类型", allowableValues = "1-业务,2-业务集")
        @RequestParam(value = "scopeType", required = false) String scopeType);

    @ApiOperation(value = "获取所有已归档的业务(集)id", produces = "application/json")
    @GetMapping("/service/app/listArchived")
    InternalResponse<List<Long>> listAllAppIdOfArchivedScope();

    @ApiOperation("根据Job业务id查询业务是否存在")
    @RequestMapping("/service/app/exists/{appId}")
    InternalResponse<Boolean> existsAppById(@ApiParam(value = "Job业务ID", required = true)
                                       @PathVariable("appId") Long appId);

    @ApiOperation(value = "获取租户下所有未删除的Job业务ID", produces = "application/json")
    @GetMapping("/service/app/listAppIdByTenant")
    InternalResponse<List<Long>> listAppIdByTenant(
        @RequestHeader(value = JobCommonHeaders.BK_TENANT_ID, required = false)
        String tenantId
    );

    @ApiOperation(value = "获取租户下所有未删除的Job业务", produces = "application/json")
    @GetMapping("/service/app/listAppByTenant")
    InternalResponse<List<ServiceApplicationDTO>> listAppByTenant(
        @RequestHeader(value = JobCommonHeaders.BK_TENANT_ID, required = false)
        String tenantId
    );
}
