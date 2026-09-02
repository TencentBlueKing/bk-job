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

package com.tencent.bk.job.manage.service.plan;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.ResolvedSummary.ResolvedGlobalVar;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把创建执行方案预检出的方案变量整理成审批概要里的全局变量章节。
 * <p>
 * 传进来的变量列表是<b>合并后的全部方案变量</b>（模板变量叠加本次请求的覆盖值），而不是请求里传的那几个：
 * 只列本次指定的变量，审批人无从判断这个方案建出来之后实际会拿什么参数去跑。
 */
@Slf4j
@Service
public class PlanGlobalVarSummaryBuilder {

    private final TenantHostService tenantHostService;

    @Autowired
    public PlanGlobalVarSummaryBuilder(TenantHostService tenantHostService) {
        this.tenantHostService = tenantHostService;
    }

    /**
     * 填充概要里的全局变量
     *
     * @param summary          待填充的概要
     * @param variables        合并后的全部方案变量
     * @param assignedVarNames 本次请求显式赋值的变量名，其余变量标为沿用模板默认值
     * @param tenantId         租户 ID，按主机 ID 反查 IP 用
     */
    public void fillGlobalVars(ResolvedSummary summary,
                               List<TaskVariableDTO> variables,
                               Set<String> assignedVarNames,
                               String tenantId) {
        if (summary == null || CollectionUtils.isEmpty(variables)) {
            return;
        }
        Map<TaskVariableDTO, TaskTargetDTO> hostTargets = parseHostTargets(variables);
        Map<Long, String> cloudIpByHostId = resolveCloudIps(hostTargets.values(), tenantId);

        for (TaskVariableDTO variable : variables) {
            if (variable == null || Boolean.TRUE.equals(variable.getDelete())) {
                continue;
            }
            ResolvedGlobalVar globalVar = new ResolvedGlobalVar();
            globalVar.setName(variable.getName());
            globalVar.setType(variable.getType() == null ? null : variable.getType().name());
            globalVar.setAssigned(assignedVarNames.contains(variable.getName()));
            if (variable.getType() == TaskVariableTypeEnum.EXECUTE_OBJECT_LIST) {
                fillHostVar(globalVar, hostTargets.get(variable), cloudIpByHostId);
            } else if (variable.getType() != TaskVariableTypeEnum.CIPHER) {
                // 密文变量的取值不进概要：概要整份明文落库，取值只在加密的参数快照里保存
                globalVar.setValue(variable.getDefaultValue());
            }
            summary.addGlobalVar(globalVar);
            markDynamicTarget(summary, globalVar);
        }
    }

    /**
     * 主机类变量的取值以 {@link TaskTargetDTO} 的 JSON 串存在 defaultValue 里，先统一解析一遍，
     * 免得补 IP 与拼概要各解析一次
     */
    private Map<TaskVariableDTO, TaskTargetDTO> parseHostTargets(List<TaskVariableDTO> variables) {
        Map<TaskVariableDTO, TaskTargetDTO> targets = new IdentityHashMap<>();
        for (TaskVariableDTO variable : variables) {
            if (variable == null || variable.getType() != TaskVariableTypeEnum.EXECUTE_OBJECT_LIST) {
                continue;
            }
            TaskTargetDTO target = TaskTargetDTO.fromJsonString(variable.getDefaultValue());
            if (target != null) {
                targets.put(variable, target);
            }
        }
        return targets;
    }

    /**
     * 按主机 ID 批量反查 云区域ID:IP。
     * <p>
     * 调用方常常只传 bk_host_id，一串主机 ID 摆在审批单据上等于没给信息。台数超过逐台列出的上限时
     * 不查：那种情况下单据只报台数，查回来的 IP 没有用处。
     */
    private Map<Long, String> resolveCloudIps(Collection<TaskTargetDTO> targets, String tenantId) {
        Set<Long> hostIdsToResolve = new HashSet<>();
        for (TaskTargetDTO target : targets) {
            List<ApplicationHostDTO> hosts = hostList(target);
            if (CollectionUtils.isEmpty(hosts) || hosts.size() > ResolvedSummary.MAX_DISPLAY_ITEM_COUNT) {
                continue;
            }
            for (ApplicationHostDTO host : hosts) {
                if (host != null && host.getHostId() != null && StringUtils.isBlank(host.getIp())) {
                    hostIdsToResolve.add(host.getHostId());
                }
            }
        }
        if (hostIdsToResolve.isEmpty()) {
            return new HashMap<>();
        }
        List<HostDTO> query = new ArrayList<>(hostIdsToResolve.size());
        for (Long hostId : hostIdsToResolve) {
            query.add(HostDTO.fromHostId(hostId));
        }
        Map<Long, String> cloudIpByHostId = new HashMap<>();
        try {
            List<ApplicationHostDTO> hosts = tenantHostService.listHosts(tenantId, query);
            if (CollectionUtils.isEmpty(hosts)) {
                return cloudIpByHostId;
            }
            for (ApplicationHostDTO host : hosts) {
                if (host.getHostId() != null && StringUtils.isNotBlank(host.getIp())) {
                    cloudIpByHostId.put(host.getHostId(), host.getCloudIp());
                }
            }
        } catch (Exception e) {
            // 补 IP 只是让单据更好读，查不到就退回展示主机 ID，不能因此让预检失败
            log.warn("Fail to resolve host ip for approval summary, hostIds={}", hostIdsToResolve, e);
        }
        return cloudIpByHostId;
    }

    private void fillHostVar(ResolvedGlobalVar globalVar,
                             TaskTargetDTO target,
                             Map<Long, String> cloudIpByHostId) {
        if (target == null) {
            return;
        }
        TaskHostNodeDTO hostNode = target.getHostNodeList();
        if (hostNode != null) {
            if (CollectionUtils.isNotEmpty(hostNode.getHostList())) {
                for (ApplicationHostDTO host : hostNode.getHostList()) {
                    if (host == null) {
                        continue;
                    }
                    globalVar.addHost(host.getHostId(), cloudIp(host, cloudIpByHostId));
                }
            }
            if (CollectionUtils.isNotEmpty(hostNode.getDynamicGroupId())) {
                globalVar.setDynamicGroupCount(hostNode.getDynamicGroupId().size());
            }
            if (CollectionUtils.isNotEmpty(hostNode.getNodeInfoList())) {
                globalVar.setTopoNodeCount(hostNode.getNodeInfoList().size());
            }
        }
        if (CollectionUtils.isNotEmpty(target.getContainerList())) {
            globalVar.setContainerCount(target.getContainerList().size());
        }
    }

    private String cloudIp(ApplicationHostDTO host, Map<Long, String> cloudIpByHostId) {
        if (StringUtils.isNotBlank(host.getIp())) {
            return host.getCloudIp();
        }
        return cloudIpByHostId.get(host.getHostId());
    }

    /**
     * 动态分组、拓扑节点下的主机在放行时才解析，单据必须给出这个提示，否则台数为 0 会被读成「不动机器」
     */
    private void markDynamicTarget(ResolvedSummary summary, ResolvedGlobalVar globalVar) {
        if (globalVar.getDynamicGroupCount() != null || globalVar.getTopoNodeCount() != null) {
            summary.setContainsDynamicTarget(true);
        }
    }

    private List<ApplicationHostDTO> hostList(TaskTargetDTO target) {
        return target.getHostNodeList() == null ? null : target.getHostNodeList().getHostList();
    }
}
