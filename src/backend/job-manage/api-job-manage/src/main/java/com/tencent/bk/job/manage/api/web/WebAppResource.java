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
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.FavorAppReq;
import com.tencent.bk.job.manage.model.web.request.IpCheckReq;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务管理API-前端调用
 *
 * @date 2019/10/10
 */
@Api(tags = {"job-manage:web:App_Management"})
@RequestMapping("/web/app")
@RestController
@WebAPI
public interface WebAppResource {

    @Deprecated
    @ApiOperation(value = "获取用户的业务列表", produces = "application/json")
    @GetMapping("/list")
    ServiceResponse<List<AppVO>> listApp(@ApiParam("用户名，网关自动传入") @RequestHeader("username") String username);

    @ApiOperation(value = "获取用户的业务列表（带收藏标识、分页）", produces = "application/json")
    @GetMapping("/list/favor")
    ServiceResponse<PageDataWithAvailableIdList<AppVO, Long>> listAppWithFavor(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "分页-开始") @RequestParam(value = "start", required = false) Integer start,
        @ApiParam(value = "分页-每页大小") @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @ApiOperation(value = "收藏业务", produces = "application/json")
    @PostMapping("/{appId}/favor")
    ServiceResponse<Integer> favorApp(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "收藏业务请求", required = true) @RequestBody FavorAppReq req);

    @ApiOperation(value = "取消收藏业务", produces = "application/json")
    @PostMapping("/{appId}/cancelFavor")
    ServiceResponse<Integer> cancelFavorApp(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "取消收藏业务请求", required = true) @RequestBody FavorAppReq req);

    @ApiOperation(value = "获取业务下的机器列表", produces = "application/json")
    @GetMapping("/{appId}/host")
    ServiceResponse<PageData<HostInfoVO>> listAppHost(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "分页-开始") @RequestParam(value = "start", required = false) Integer start,
        @ApiParam(value = "分页-每页大小") @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @ApiParam(value = "模块类型 0-所有模块 1-普通模块，2-DB模块") @RequestParam(value = "moduleType",
            required = false) Long moduleType,
        @ApiParam(value = "ip，搜索条件，模糊匹配")
        @RequestParam(value = "ipCondition", required = false) String ipCondition);

    @ApiOperation(value = "获取业务拓扑列表", produces = "application/json")
    @GetMapping("/{appId}/topology")
    ServiceResponse<CcTopologyNodeVO> listAppTopologyTree(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId);

    @ApiOperation(value = "获取业务拓扑主机列表", produces = "application/json")
    @GetMapping("/{appId}/topology/host")
    ServiceResponse<CcTopologyNodeVO> listAppTopologyHostTree(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId);

    @ApiOperation(value = "获取业务拓扑树（含各节点主机数）", produces = "application/json")
    @GetMapping("/{appId}/topology/hostCount")
    ServiceResponse<CcTopologyNodeVO> listAppTopologyHostCountTree(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId);

    @ApiOperation(value = "IP选择器根据拓扑节点集合获取机器列表", produces = "application/json")
    @PostMapping("/{appId}/topology/hosts/nodes")
    ServiceResponse<PageData<HostInfoVO>> listHostByBizTopologyNodes(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "拓扑节点集合与分页信息", required = true) @RequestBody ListHostByBizTopologyNodesReq req
    );

    @ApiOperation(value = "IP选择器根据拓扑节点集合获取机器列表（纯IP），返回IP格式为[cloudId:IP]"
        , produces = "application/json")
    @PostMapping("/{appId}/topology/IPs/nodes")
    ServiceResponse<PageData<String>> listIpByBizTopologyNodes(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "拓扑节点集合与分页信息", required = true) @RequestBody ListHostByBizTopologyNodesReq req
    );

    @ApiOperation(value = "获取节点详情", produces = "application/json")
    @PostMapping("/{appId}/node/detail")
    ServiceResponse<List<AppTopologyTreeNode>> getNodeDetail(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "选中的拓扑节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody List<TargetNodeVO> targetNodeVOList);

    @ApiOperation(value = "获取节点拓扑路径", produces = "application/json")
    @PostMapping("/{appId}/node/queryPath")
    ServiceResponse<List<List<CcTopologyNodeVO>>> queryNodePaths(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "需要查询拓扑路径的节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody List<TargetNodeVO> targetNodeVOList);

    @ApiOperation(value = "根据模块获取机器列表", produces = "application/json")
    @PostMapping("/{appId}/host/node")
    ServiceResponse<List<NodeInfoVO>> listHostByNode(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "选中的拓扑节点列表(将拓扑树节点中的objectId与instanceId传入)", required = true)
        @RequestBody List<TargetNodeVO> targetNodeVOList);

    @ApiOperation(value = "获取业务动态分组列表", produces = "application/json")
    @GetMapping("/{appId}/dynamicGroup")
    ServiceResponse<List<DynamicGroupInfoVO>> listAppDynamicGroup(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId);

    @ApiOperation(value = "获取业务动态分组主机列表")
    @GetMapping("/{appId}/dynamicGroup/{dynamicGroupId}")
    ServiceResponse<List<DynamicGroupInfoVO>> listAppDynamicGroupHost(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "动态分组 ID，逗号分割",
            required = true) @PathVariable("dynamicGroupId") List<String> dynamicGroupIds);

    @ApiOperation(value = "获取业务动态分组信息(不含主机)")
    @GetMapping("/{appId}/dynamicGroup/{dynamicGroupId}/detailWithoutHosts")
    ServiceResponse<List<DynamicGroupInfoVO>> listAppDynamicGroupWithoutHosts(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "动态分组 ID，逗号分割",
            required = true) @PathVariable("dynamicGroupId") List<String> dynamicGroupIds);

    @ApiOperation(value = "根据输入 IP 获取机器信息")
    @PostMapping("/{appId}/ip/check")
    ServiceResponse<List<HostInfoVO>> listHostByIp(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "用户输入的 IP 列表", required = true) @RequestBody IpCheckReq req);

    @ApiOperation(value = "查询主机统计信息")
    @PostMapping("/{appId}/host/statistics")
    ServiceResponse<AgentStatistics> agentStatistics(
        @ApiParam(value = "用户名，网关自动传入", required = true) @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "请求体", required = true) @RequestBody AgentStatisticsReq agentStatisticsReq);

}
