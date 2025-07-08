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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import com.tencent.bk.job.manage.model.web.request.chooser.ListTopologyTreesReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerCheckReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerDetailReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ListContainerByTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.chooser.container.ContainerTopologyNodeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * 容器管理 WEB API
 */
@Api(tags = "job-manage:web:Container_Management")
@RequestMapping("/web/scope/{scopeType}/{scopeId}/")
@RestController
@WebAPI
public interface WebContainerResource {

    // 容器选择器标准接口-1
    @ApiOperation(value = "获取容器拓扑树（含各节点容器数）", produces = "application/json")
    @PostMapping(value = {"/topology/container"})
    Response<List<ContainerTopologyNodeVO>> listTopologyTrees(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "资源范围信息")
        @RequestBody(required = false)
            ListTopologyTreesReq req
    );

    // 容器选择器标准接口-2
    @ApiOperation(value = "容器选择器根据拓扑节点集合获取容器列表", produces = "application/json")
    @PostMapping(value = {"/topology/containers/nodes"})
    Response<PageData<ContainerVO>> listContainerByTopologyNodes(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "拓扑节点集合与分页信息", required = true)
        @RequestBody
            ListContainerByTopologyNodesReq req
    );

    // 容器选择器标准接口-3
    @ApiOperation(value = "容器选择器根据拓扑节点集合获取容器资源ID列表，用于跨页全选容器功能"
        , produces = "application/json")
    @PostMapping(value = {"/topology/containerIds/nodes"})
    Response<PageData<ContainerIdWithMeta>> listContainerIdByTopologyNodes(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "拓扑节点集合与分页信息", required = true)
        @RequestBody
            ListContainerByTopologyNodesReq req
    );


    // 容器选择器标准接口-4
    @ApiOperation(value = "根据用户选择/输入的容器信息获取容器")
    @PostMapping(value = {"/container/check"})
    Response<List<ContainerVO>> checkContainers(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "用户选择/输入的容器信息", required = true)
        @RequestBody
            ContainerCheckReq req
    );

    // 容器选择器标准接口-5
    @ApiOperation(value = "根据容器资源 ID批量查询容器详情信息")
    @PostMapping(value = {"/containers/details"})
    Response<List<ContainerVO>> getContainerDetails(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "容器ID及元数据信息", required = true)
        @RequestBody
            ContainerDetailReq req
    );

}
