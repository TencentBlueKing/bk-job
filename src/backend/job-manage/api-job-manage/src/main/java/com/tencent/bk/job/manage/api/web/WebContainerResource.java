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
import com.tencent.bk.job.manage.model.web.request.chooser.container.QueryKubeNodeNamesReq;
import com.tencent.bk.job.manage.model.web.request.dynamicfilter.PreviewDynamicContainerReq;
import com.tencent.bk.job.manage.model.web.vo.chooser.container.ContainerTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.chooser.container.QueryKubeNodeNamesResp;
import com.tencent.bk.job.manage.model.web.vo.dynamicfilter.DynamicContainerFilterMetadataVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

/**
 * 容器管理 WEB API
 */
@Tag(name = "job-manage:web:Container_Management")
@RequestMapping("/web/scope/{scopeType}/{scopeId}/")
@RestController
@WebAPI
public interface WebContainerResource {

    // 容器选择器标准接口-1
    @Operation(summary = "获取容器拓扑树（含各节点容器数）")
    @PostMapping(value = {"/topology/container"})
    Response<List<ContainerTopologyNodeVO>> listTopologyTrees(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "资源范围信息")
        @RequestBody(required = false)
            ListTopologyTreesReq req
    );

    // 容器选择器标准接口-2
    @Operation(summary = "容器选择器根据拓扑节点集合获取容器列表")
    @PostMapping(value = {"/topology/containers/nodes"})
    Response<PageData<ContainerVO>> listContainerByTopologyNodes(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "拓扑节点集合与分页信息", required = true)
        @RequestBody
            ListContainerByTopologyNodesReq req
    );

    // 容器选择器标准接口-3
    @Operation(summary = "容器选择器根据拓扑节点集合获取容器资源ID列表，用于跨页全选容器功能"
        )
    @PostMapping(value = {"/topology/containerIds/nodes"})
    Response<PageData<ContainerIdWithMeta>> listContainerIdByTopologyNodes(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "拓扑节点集合与分页信息", required = true)
        @RequestBody
            ListContainerByTopologyNodesReq req
    );


    // 容器选择器标准接口-4
    @Operation(summary = "根据用户选择/输入的容器信息获取容器")
    @PostMapping(value = {"/container/check"})
    Response<List<ContainerVO>> checkContainers(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "用户选择/输入的容器信息", required = true)
        @RequestBody
            ContainerCheckReq req
    );

    // 容器选择器标准接口-5
    @Operation(summary = "根据容器资源 ID批量查询容器详情信息")
    @PostMapping(value = {"/containers/details"})
    Response<List<ContainerVO>> getContainerDetails(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "容器ID及元数据信息", required = true)
        @RequestBody
            ContainerDetailReq req
    );

    // 动态条件过滤器-1
    @Operation(summary = "获取动态条件过滤器的字段/运算符元数据")
    @GetMapping(value = {"/dynamic/container/field/metadata"})
    Response<DynamicContainerFilterMetadataVO> getDynamicContainerFilterMetadata(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );

    // 动态条件过滤器-2
    @Operation(summary = "动态条件过滤器命中预览")
    @PostMapping(value = {"/dynamic/container/preview"})
    Response<PageData<ContainerVO>> previewDynamicContainerByCondition(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "动态条件过滤器与分页参数", required = true)
        @Valid
        @RequestBody
            PreviewDynamicContainerReq req
    );

    @Operation(summary = "根据 (type,id) 批量查询 kube 拓扑节点的展示名（用于详情页/编辑页回显）")
    @PostMapping(value = {"/topology/container/nodes/names"})
    Response<QueryKubeNodeNamesResp> queryKubeNodeNames(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "待反查名称的节点列表", required = true)
        @Valid
        @RequestBody
            QueryKubeNodeNamesReq req
    );

}
