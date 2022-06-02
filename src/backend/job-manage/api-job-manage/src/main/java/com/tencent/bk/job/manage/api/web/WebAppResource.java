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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.HostCheckReq;
import com.tencent.bk.job.manage.model.web.request.app.FavorAppReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupInfoVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.PageDataWithAvailableIdList;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * 业务管理 WEB API
 */
@Api(tags = {"job-manage:web:App_Management"})
@RequestMapping("/web")
@RestController
@WebAPI
public interface WebAppResource {

    @ApiOperation(value = "获取用户的业务列表（带收藏标识、分页）", produces = "application/json")
    @GetMapping(value = {"/app/list/favor", "/scope/list/favor"})
    Response<PageDataWithAvailableIdList<AppVO, Long>> listAppWithFavor(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    @ApiOperation(value = "收藏业务", produces = "application/json")
    @PostMapping(value = "/scope/{scopeType}/{scopeId}/favor")
    Response<Integer> favorApp(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "当前所在的资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "当前所在的资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @RequestBody FavorAppReq favorAppReq
    );

    @ApiOperation(value = "取消收藏业务", produces = "application/json")
    @PostMapping(value = "/scope/{scopeType}/{scopeId}/cancelFavor")
    Response<Integer> cancelFavorApp(
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
        @RequestBody FavorAppReq favorAppReq
    );

    @ApiOperation(value = "获取业务下的机器列表", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/host"})
    Response<PageData<HostInfoVO>> listAppHost(
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
        @ApiParam(value = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam(value = "模块类型 0-所有模块 1-普通模块，2-DB模块")
        @RequestParam(value = "moduleType",
            required = false) Long moduleType,
        @ApiParam(value = "ip，搜索条件，模糊匹配")
        @RequestParam(value = "ipCondition", required = false)
            String ipCondition
    );

    @ApiOperation(value = "获取业务拓扑列表", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/topology"})
    Response<CcTopologyNodeVO> listAppTopologyTree(
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
            String scopeId
    );

    @ApiOperation(value = "获取业务拓扑主机列表", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/topology/host"})
    Response<CcTopologyNodeVO> listAppTopologyHostTree(
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
            String scopeId
    );

    @ApiOperation(value = "获取业务拓扑树（含各节点主机数）", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/topology/hostCount"})
    Response<CcTopologyNodeVO> listAppTopologyHostCountTree(
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
            String scopeId
    );

    @ApiOperation(value = "IP选择器根据拓扑节点集合获取机器列表", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/topology/hosts/nodes"})
    Response<PageData<HostInfoVO>> listHostByBizTopologyNodes(
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
            ListHostByBizTopologyNodesReq req
    );

    @ApiOperation(value = "获取节点详情", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/node/detail"})
    Response<List<AppTopologyTreeNode>> getNodeDetail(
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
        @ApiParam(value = "选中的拓扑节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody
            List<TargetNodeVO> targetNodeVOList
    );

    @ApiOperation(value = "获取节点拓扑路径", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/node/queryPath"})
    Response<List<List<CcTopologyNodeVO>>> queryNodePaths(
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
        @ApiParam(value = "需要查询拓扑路径的节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody
            List<TargetNodeVO> targetNodeVOList
    );

    @ApiOperation(value = "根据模块获取机器列表", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/host/node"})
    Response<List<NodeInfoVO>> listHostByNode(
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
        @ApiParam(value = "选中的拓扑节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody
            List<TargetNodeVO> targetNodeVOList
    );

    @ApiOperation(value = "获取业务动态分组列表", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/dynamicGroup"})
    Response<List<DynamicGroupInfoVO>> listAppDynamicGroup(
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
            String scopeId
    );

    @ApiOperation(value = "获取业务动态分组主机列表")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/dynamicGroup/{dynamicGroupId}"})
    Response<List<DynamicGroupInfoVO>> listAppDynamicGroupHost(
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
        @ApiParam(value = "动态分组 ID，逗号分割", required = true)
        @PathVariable("dynamicGroupId")
            List<String> dynamicGroupIds
    );

    @ApiOperation(value = "获取业务动态分组信息(不含主机)")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/dynamicGroup/{dynamicGroupId}/detailWithoutHosts"})
    Response<List<DynamicGroupInfoVO>> listAppDynamicGroupWithoutHosts(
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
        @ApiParam(value = "动态分组 ID，逗号分割", required = true)
        @PathVariable("dynamicGroupId")
            List<String> dynamicGroupIds
    );

    @ApiOperation(value = "根据用户选择/输入的主机信息获取真实存在的机器信息")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/ip/check", "/scope/{scopeType}/{scopeId}/host/check"})
    Response<List<HostInfoVO>> checkHosts(
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
        @ApiParam(value = "用户选择/输入的主机信息", required = true)
        @RequestBody
            HostCheckReq req
    );

    @ApiOperation(value = "查询主机统计信息")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/host/statistics"})
    Response<AgentStatistics> agentStatistics(
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
        @ApiParam(value = "请求体", required = true)
        @RequestBody
            AgentStatisticsReq agentStatisticsReq
    );

}
