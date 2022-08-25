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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.HostCheckReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.GetHostAgentStatisticsByDynamicGroupsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.GetHostAgentStatisticsByNodesReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostDetailReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListDynamicGroupsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListTopologyHostCountTreesReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.PageListHostsByDynamicGroupReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupBasicVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupInfoVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.DynamicGroupHostStatisticsVO;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.NodeHostStatisticsVO;
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
 * 主机管理 WEB API
 */
@Api(tags = {"job-manage:web:Host_Management"})
@RequestMapping("/web")
@RestController
@WebAPI
public interface WebHostResource {

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(
        explain = "仅用作IPv6上线发布过程中的兼容，后续切换为listHostIdByBizTopologyNodes", version = "3.7.0")
    @ApiOperation(value = "IP选择器根据拓扑节点集合获取机器列表（纯IP），返回IP格式为[cloudId:IP]"
        , produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/topology/IPs/nodes"})
    Response<PageData<String>> listIpByBizTopologyNodes(
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
    @ApiOperation(value = "获取节点详情", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/node/detail"})
    Response<List<BizTopoNode>> getNodeDetail(
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    @Deprecated
    @CompatibleImplementation(name = "ipv6", version = "3.8.0", explain = "仅用于发布期间兼容")
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

    // 标准接口1
    @ApiOperation(value = "获取拓扑树（含各节点主机数）", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/topology/hostCount"})
    Response<List<CcTopologyNodeVO>> listTopologyHostCountTrees(
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
        @ApiParam(value = "资源范围信息", required = true)
        @RequestBody
            ListTopologyHostCountTreesReq req
    );

    // 标准接口2
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

    // 标准接口3
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

    // 标准接口4
    @ApiOperation(value = "IP选择器根据拓扑节点集合获取机器hostIds"
        , produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/topology/hostIds/nodes"})
    Response<PageData<HostIdWithMeta>> listHostIdByBizTopologyNodes(
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

    // 标准接口5
    @ApiOperation(value = "获取多个节点下的主机统计信息", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/host/agentStatistics/nodes"})
    Response<List<NodeHostStatisticsVO>> getHostAgentStatisticsByNodes(
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
        @ApiParam(value = "要查询主机Agent状态统计信息的拓扑节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody
            GetHostAgentStatisticsByNodesReq req
    );

    // 标准接口6
    @ApiOperation(value = "批量获取动态分组信息(不含主机)")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/dynamicGroups"})
    Response<List<DynamicGroupBasicVO>> listDynamicGroups(
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
        @ApiParam(value = "批量获取动态分组信息请求体", required = true)
        @RequestBody(required = false)
            ListDynamicGroupsReq req
    );

    // 标准接口7
    @ApiOperation(value = "获取多个动态分组下的主机Agent状态统计信息", produces = "application/json")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/host/agentStatistics/dynamicGroups"})
    Response<List<DynamicGroupHostStatisticsVO>> getHostAgentStatisticsByDynamicGroups(
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
            GetHostAgentStatisticsByDynamicGroupsReq req
    );

    // 标准接口8
    @ApiOperation(value = "分页查询某个动态分组下的主机列表")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/hosts/dynamicGroup"})
    Response<PageData<HostInfoVO>> pageListHostsByDynamicGroup(
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
        @ApiParam(value = "分页查询某个动态分组下的主机列表请求体", required = true)
        @RequestBody
            PageListHostsByDynamicGroupReq req
    );

    // 标准接口9
    @CompatibleImplementation(explain = "旧的/ip/check仅用于发布过程兼容", version = "3.7.0")
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

    // 标准接口10
    @ApiOperation(value = "根据hostId批量查询主机详情信息")
    @PostMapping(value = {"/scope/{scopeType}/{scopeId}/hosts/details"})
    Response<List<HostInfoVO>> getHostDetails(
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
        @ApiParam(value = "主机ID及元数据信息", required = true)
        @RequestBody
            HostDetailReq req
    );

}
