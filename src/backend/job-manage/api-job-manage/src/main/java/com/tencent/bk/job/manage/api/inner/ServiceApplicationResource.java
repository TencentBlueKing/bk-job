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
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:service:App_Management"})
@RestController
@InternalAPI
public interface ServiceApplicationResource {
    /**
     * 查询CMDB中的常规业务列表
     *
     * @return 业务列表
     */
    @RequestMapping("/service/app/list/normal")
    InternalResponse<List<ServiceAppBaseInfoDTO>> listNormalApps();

    /**
     * 查询CMDB中的业务集、全业务等非常规业务列表
     *
     * @return 业务集、全业务列表
     */
    @RequestMapping("/service/app/list/bizSet")
    InternalResponse<List<ServiceApplicationDTO>> listBizSetApps();

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
     * @param appIds 业务ID列表，英文逗号分隔
     * @return 业务列表
     */
    @ApiOperation("根据Job业务id批量查询业务")
    @RequestMapping("/service/apps")
    List<ServiceApplicationDTO> listAppsByAppIds(@ApiParam(value = "业务ID列表，英文逗号分隔", required = true)
                                                  @RequestParam("appIds") String appIds);

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
}
