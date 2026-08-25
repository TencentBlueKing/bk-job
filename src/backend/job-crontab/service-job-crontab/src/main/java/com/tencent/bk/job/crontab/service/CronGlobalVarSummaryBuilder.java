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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.ResolvedSummary.ResolvedGlobalVar;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTargetDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把保存定时任务预检出的变量取值整理成审批概要里的全局变量章节。
 * <p>
 * 列出的是<b>执行方案的全部全局变量</b>，而不只是本次请求传的那几个：定时任务到点就会拿这套变量去跑，
 * 只列本次传的，审批人无从判断这个定时任务实际会用什么参数执行。本次没传的变量按现有取值展示
 * （更新时沿用定时任务原有取值，否则是执行方案默认值），并标注非本次指定。
 */
@Slf4j
@Service
public class CronGlobalVarSummaryBuilder {

    private final ServiceTaskPlanResource taskPlanResource;
    private final HostService hostService;

    @Autowired
    public CronGlobalVarSummaryBuilder(ServiceTaskPlanResource taskPlanResource,
                                       HostService hostService) {
        this.taskPlanResource = taskPlanResource;
        this.hostService = hostService;
    }

    /**
     * 填充概要里的全局变量
     *
     * @param summary     待填充的概要
     * @param cronJobInfo 预检通过的定时任务信息，其变量取值已是更新后实际生效的那一套
     * @param operator    操作人，取租户用
     */
    public void fillGlobalVars(ResolvedSummary summary, CronJobInfoDTO cronJobInfo, User operator) {
        Long planId = cronJobInfo.getTaskPlanId();
        if (summary == null || planId == null || planId <= 0) {
            // 定时执行脚本的定时任务没有全局变量
            return;
        }
        List<ServiceTaskVariableDTO> planVars = listPlanVars(cronJobInfo, operator);
        if (CollectionUtils.isEmpty(planVars)) {
            return;
        }
        Map<Long, CronJobVariableDTO> requestVarById = new HashMap<>();
        Map<String, CronJobVariableDTO> requestVarByName = new HashMap<>();
        if (CollectionUtils.isNotEmpty(cronJobInfo.getVariableValue())) {
            for (CronJobVariableDTO variable : cronJobInfo.getVariableValue()) {
                if (variable == null) {
                    continue;
                }
                if (variable.getId() != null) {
                    requestVarById.put(variable.getId(), variable);
                }
                if (StringUtils.isNotBlank(variable.getName())) {
                    requestVarByName.put(variable.getName(), variable);
                }
            }
        }

        List<HostTarget> hostTargets = new ArrayList<>();
        List<ResolvedGlobalVar> globalVars = new ArrayList<>(planVars.size());
        for (ServiceTaskVariableDTO planVar : planVars) {
            CronJobVariableDTO assignedVar = resolveAssignedVar(planVar, requestVarById, requestVarByName);
            TaskVariableTypeEnum type = planVar.getType() == null
                ? null : TaskVariableTypeEnum.valOf(planVar.getType());

            ResolvedGlobalVar globalVar = new ResolvedGlobalVar();
            globalVar.setName(planVar.getName());
            globalVar.setType(type == null ? null : type.name());
            globalVar.setAssigned(assignedVar != null);
            if (type == TaskVariableTypeEnum.EXECUTE_OBJECT_LIST) {
                hostTargets.add(new HostTarget(globalVar, assignedVar == null
                    ? fromPlanDefault(planVar.getDefaultTargetValue())
                    : fromCronVar(assignedVar.getServer())));
            } else if (type != TaskVariableTypeEnum.CIPHER) {
                // 密文变量的取值不进概要：概要整份明文落库，取值只在加密的参数快照里保存
                globalVar.setValue(assignedVar == null ? planVar.getDefaultValue() : assignedVar.getValue());
            }
            globalVars.add(globalVar);
        }

        fillHostTargets(hostTargets, operator.getTenantId());
        for (ResolvedGlobalVar globalVar : globalVars) {
            summary.addGlobalVar(globalVar);
            if (globalVar.getDynamicGroupCount() != null || globalVar.getTopoNodeCount() != null) {
                // 动态分组、拓扑节点下的主机在放行时才解析，单据必须给出这个提示，
                // 否则台数为 0 会被读成「不动机器」
                summary.setContainsDynamicTarget(true);
            }
        }
    }

    /**
     * 与请求转换时的定位规则保持一致：给了 ID 以 ID 为准，只给名称才按名称定位
     */
    private CronJobVariableDTO resolveAssignedVar(ServiceTaskVariableDTO planVar,
                                                  Map<Long, CronJobVariableDTO> requestVarById,
                                                  Map<String, CronJobVariableDTO> requestVarByName) {
        if (planVar.getId() != null) {
            CronJobVariableDTO byId = requestVarById.get(planVar.getId());
            if (byId != null) {
                return byId;
            }
        }
        return StringUtils.isBlank(planVar.getName()) ? null : requestVarByName.get(planVar.getName());
    }

    private List<ServiceTaskVariableDTO> listPlanVars(CronJobInfoDTO cronJobInfo, User operator) {
        try {
            InternalResponse<List<ServiceTaskVariableDTO>> resp = taskPlanResource.getPlanVariable(
                operator.getUsername(),
                cronJobInfo.getAppId(),
                cronJobInfo.getTaskTemplateId(),
                cronJobInfo.getTaskPlanId()
            );
            return resp.isSuccess() ? resp.getData() : Collections.emptyList();
        } catch (Exception e) {
            // 变量章节缺失总比整个预检失败好：预检失败会让调用方拿不到审批单
            log.warn("Fail to list plan global vars for approval summary, planId={}", cronJobInfo.getTaskPlanId(), e);
            return Collections.emptyList();
        }
    }

    private HostTargetValue fromCronVar(ServerDTO server) {
        if (server == null) {
            return null;
        }
        HostTargetValue value = new HostTargetValue();
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            for (HostDTO host : server.getIps()) {
                if (host != null) {
                    value.addHost(host.getHostId(), host.toCloudIp());
                }
            }
        }
        value.dynamicGroupCount = CollectionUtils.size(server.getDynamicGroupIds());
        value.topoNodeCount = CollectionUtils.size(server.getTopoNodes());
        value.containerCount = CollectionUtils.size(server.getContainers());
        return value;
    }

    private HostTargetValue fromPlanDefault(ServiceTaskTargetDTO target) {
        if (target == null) {
            return null;
        }
        HostTargetValue value = new HostTargetValue();
        ServiceTaskHostNodeDTO targetServer = target.getTargetServer();
        if (targetServer != null) {
            if (CollectionUtils.isNotEmpty(targetServer.getHostList())) {
                for (ServiceHostInfoDTO host : targetServer.getHostList()) {
                    if (host != null) {
                        value.addHost(host.getHostId(), cloudIp(host.getCloudAreaId(), host.getIp()));
                    }
                }
            }
            value.dynamicGroupCount = CollectionUtils.size(targetServer.getDynamicGroupId());
            value.topoNodeCount = CollectionUtils.size(targetServer.getNodeInfoList());
        }
        value.containerCount = CollectionUtils.size(target.getContainerList());
        return value;
    }

    /**
     * 按主机 ID 批量反查 云区域ID:IP 后写入各主机类变量。
     * <p>
     * 调用方常常只传 bk_host_id，一串主机 ID 摆在审批单据上等于没给信息。台数超过逐台列出的上限时
     * 不查：那种情况下单据只报台数，查回来的 IP 没有用处
     */
    private void fillHostTargets(List<HostTarget> hostTargets, String tenantId) {
        Set<Long> hostIdsToResolve = new HashSet<>();
        for (HostTarget hostTarget : hostTargets) {
            HostTargetValue value = hostTarget.value;
            if (value == null || value.hosts.size() > ResolvedSummary.MAX_GLOBAL_VAR_HOST_COUNT) {
                continue;
            }
            for (HostRef host : value.hosts) {
                if (host.hostId != null && StringUtils.isBlank(host.cloudIp)) {
                    hostIdsToResolve.add(host.hostId);
                }
            }
        }
        Map<Long, String> cloudIpByHostId = resolveCloudIps(hostIdsToResolve, tenantId);
        for (HostTarget hostTarget : hostTargets) {
            HostTargetValue value = hostTarget.value;
            if (value == null) {
                continue;
            }
            ResolvedGlobalVar globalVar = hostTarget.globalVar;
            for (HostRef host : value.hosts) {
                globalVar.addHost(host.hostId, StringUtils.isNotBlank(host.cloudIp)
                    ? host.cloudIp : cloudIpByHostId.get(host.hostId));
            }
            globalVar.setDynamicGroupCount(value.dynamicGroupCount > 0 ? value.dynamicGroupCount : null);
            globalVar.setTopoNodeCount(value.topoNodeCount > 0 ? value.topoNodeCount : null);
            globalVar.setContainerCount(value.containerCount > 0 ? value.containerCount : null);
        }
    }

    private Map<Long, String> resolveCloudIps(Set<Long> hostIds, String tenantId) {
        Map<Long, String> cloudIpByHostId = new HashMap<>();
        if (hostIds.isEmpty()) {
            return cloudIpByHostId;
        }
        List<HostDTO> query = new ArrayList<>(hostIds.size());
        for (Long hostId : hostIds) {
            query.add(HostDTO.fromHostId(hostId));
        }
        try {
            hostService.fillHosts(tenantId, query);
            for (HostDTO host : query) {
                String cloudIp = host.toCloudIp();
                if (host.getHostId() != null && StringUtils.isNotBlank(cloudIp)) {
                    cloudIpByHostId.put(host.getHostId(), cloudIp);
                }
            }
        } catch (Exception e) {
            // 补 IP 只是让单据更好读，查不到就退回展示主机 ID，不能因此让预检失败
            log.warn("Fail to resolve host ip for approval summary, hostIds={}", hostIds, e);
        }
        return cloudIpByHostId;
    }

    /**
     * 定时任务的主机变量取值与执行方案的主机变量默认值是两种结构，先归一成这个中间形态，
     * 台数上限判断、补 IP、写回概要就只需要写一份
     */
    private static class HostTargetValue {

        private final List<HostRef> hosts = new ArrayList<>();
        private int dynamicGroupCount;
        private int topoNodeCount;
        private int containerCount;

        private void addHost(Long hostId, String cloudIp) {
            hosts.add(new HostRef(hostId, cloudIp));
        }
    }

    private static class HostRef {

        private final Long hostId;
        private final String cloudIp;

        private HostRef(Long hostId, String cloudIp) {
            this.hostId = hostId;
            this.cloudIp = cloudIp;
        }
    }

    private static class HostTarget {

        private final ResolvedGlobalVar globalVar;
        private final HostTargetValue value;

        private HostTarget(ResolvedGlobalVar globalVar, HostTargetValue value) {
            this.globalVar = globalVar;
            this.value = value;
        }
    }

    private String cloudIp(Long cloudAreaId, String ip) {
        if (cloudAreaId == null || StringUtils.isBlank(ip)) {
            return null;
        }
        return cloudAreaId + ":" + ip;
    }
}
