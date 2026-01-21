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

package com.tencent.bk.job.manage.model.dto.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.model.vo.TaskExecuteObjectsInfoVO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTargetDTO;
import com.tencent.bk.job.manage.service.host.CurrentTenantHostService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 执行目标主机
 */
@PersistenceObject
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskTargetDTO {

    @JsonProperty("variable")
    private String variable;

    @JsonProperty("hostNodeList")
    private TaskHostNodeDTO hostNodeList;

    public static TaskTargetVO toVO(TaskTargetDTO executeTarget) {
        if (executeTarget == null) {
            return null;
        }
        TaskTargetVO taskTargetVO = new TaskTargetVO();
        taskTargetVO.setVariable(executeTarget.getVariable());
        TaskHostNodeVO taskHostNodeVO = TaskHostNodeDTO.toVO(executeTarget.getHostNodeList());
        if (taskHostNodeVO != null) {
            taskTargetVO.setHostNodeInfo(taskHostNodeVO);
            TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = new TaskExecuteObjectsInfoVO();
            taskExecuteObjectsInfoVO.setHostList(taskHostNodeVO.getHostList());
            taskExecuteObjectsInfoVO.setNodeList(taskHostNodeVO.getNodeList());
            taskExecuteObjectsInfoVO.setDynamicGroupList(taskHostNodeVO.getDynamicGroupList());
            taskTargetVO.setExecuteObjectsInfo(taskExecuteObjectsInfoVO);
        }
        return taskTargetVO;
    }

    public static TaskTargetDTO fromVO(TaskTargetVO taskTargetVO) {
        if (taskTargetVO == null) {
            return null;
        }
        TaskTargetDTO taskTargetDTO = new TaskTargetDTO();
        if (StringUtils.isNotBlank(taskTargetVO.getVariable())) {
            taskTargetDTO.setVariable(taskTargetVO.getVariable());
        }
        taskTargetDTO.setHostNodeList(TaskHostNodeDTO.fromVO(taskTargetVO.getExecuteObjectsInfoCompatibly()));
        fillHostDetail(taskTargetDTO);
        return taskTargetDTO;
    }

    private static void fillHostDetail(TaskTargetDTO target) {
        CurrentTenantHostService currentTenantHostService =
            ApplicationContextRegister.getBean(CurrentTenantHostService.class);
        if (target.getHostNodeList() != null && CollectionUtils.isNotEmpty(target.getHostNodeList().getHostList())) {
            List<ApplicationHostDTO> hostList = target.getHostNodeList().getHostList();
            Set<Long> hostIds = new HashSet<>();
            Set<String> hostCloudIps = new HashSet<>();
            for (ApplicationHostDTO host : hostList) {
                if (host.getHostId() != null) {
                    hostIds.add(host.getHostId());
                }
                if (StringUtils.isNotBlank(host.getCloudIp())) {
                    hostCloudIps.add(host.getCloudIp());
                }
            }

            if (hostIds.isEmpty() && hostCloudIps.isEmpty()) {
                return;
            }
            Map<Long, ApplicationHostDTO> hostIdHostMapping = currentTenantHostService.listHostsByHostIds(hostIds);
            Map<String, ApplicationHostDTO> cloudIpHostMapping = currentTenantHostService.listHostsByIps(hostCloudIps);

            hostList.forEach(hostNode -> {
                ApplicationHostDTO hostDTO = cloudIpHostMapping.get(hostNode.getCloudIp());
                if (hostDTO == null && StringUtils.isBlank(hostNode.getIp())) {
                    hostDTO = hostIdHostMapping.get(hostNode.getHostId());
                }
                if (hostDTO != null) {
                    hostNode.setHostId(hostDTO.getHostId());
                    hostNode.setAgentId(hostDTO.getAgentId());
                    hostNode.setCloudAreaId(hostDTO.getCloudAreaId());
                    hostNode.setIp(hostDTO.getIp());
                    hostNode.setIpv6(hostDTO.getIpv6());
                    hostNode.setDisplayIp(hostDTO.getDisplayIp());
                    hostNode.setOsName(hostDTO.getOsName());
                    hostNode.setOsType(hostDTO.getOsType());
                    hostNode.setGseAgentStatus(hostDTO.getGseAgentStatus());
                } else {
                    log.warn("Cannot find host by hostId={} or by cloudIp={}",
                        hostNode.getHostId(), hostNode.getCloudIp());
                    hostNode.setHostId(-1L);
                }
            });
        }
    }

    public static TaskTargetDTO fromJsonString(String targetString) {
        if (StringUtils.isBlank(targetString)) {
            return null;
        }
        TaskTargetDTO taskTarget = JsonMapper.nonEmptyMapper().fromJson(targetString, TaskTargetDTO.class);
        return standardizeDynamicGroupId(taskTarget);
    }

    private static TaskTargetDTO standardizeDynamicGroupId(TaskTargetDTO taskTarget) {
        // 移除动态分组ID中多余的appId(历史问题)
        if (taskTarget != null && taskTarget.getHostNodeList() != null &&
            CollectionUtils.isNotEmpty(taskTarget.getHostNodeList().getDynamicGroupId())) {
            List<String> standardDynamicGroupIdList = new ArrayList<>();
            taskTarget.getHostNodeList().getDynamicGroupId().forEach(dynamicGroupId -> {
                if (StringUtils.isNotEmpty(dynamicGroupId)) {
                    // appId:groupId
                    String[] appIdAndGroupId = dynamicGroupId.split(":");
                    if (appIdAndGroupId.length == 2) {
                        standardDynamicGroupIdList.add(appIdAndGroupId[1]);
                    } else {
                        standardDynamicGroupIdList.add(dynamicGroupId);
                    }
                }
            });
            taskTarget.getHostNodeList().setDynamicGroupId(standardDynamicGroupIdList);
        }
        return taskTarget;
    }

    public static EsbServerV3DTO toEsbServerV3(TaskTargetDTO taskTarget) {
        if (taskTarget == null) {
            return null;
        }
        EsbServerV3DTO esbServer = new EsbServerV3DTO();
        esbServer.setVariable(taskTarget.getVariable());
        if (taskTarget.getHostNodeList() != null) {
            if (CollectionUtils.isNotEmpty(taskTarget.getHostNodeList().getHostList())) {
                esbServer.setIps(taskTarget.getHostNodeList().getHostList().stream()
                    .map(EsbIpDTO::fromApplicationHostInfo).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(taskTarget.getHostNodeList().getDynamicGroupId())) {
                esbServer.setDynamicGroups(taskTarget.getHostNodeList().getDynamicGroupId().stream().map(id -> {
                    EsbDynamicGroupDTO esbDynamicGroup = new EsbDynamicGroupDTO();
                    esbDynamicGroup.setId(id);
                    return esbDynamicGroup;
                }).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(taskTarget.getHostNodeList().getNodeInfoList())) {
                esbServer.setTopoNodes(taskTarget.getHostNodeList().getNodeInfoList().stream()
                    .map(TaskNodeInfoDTO::toEsbCmdbTopoNode).collect(Collectors.toList()));
            }
        }
        return esbServer;
    }

    public ServiceTaskTargetDTO toServiceTaskTargetDTO() {
        ServiceTaskTargetDTO targetDTO = new ServiceTaskTargetDTO();
        targetDTO.setVariable(variable);
        if (hostNodeList != null) {
            ServiceTaskHostNodeDTO targetServer = new ServiceTaskHostNodeDTO();
            if (CollectionUtils.isNotEmpty(hostNodeList.getNodeInfoList())) {
                targetServer.setNodeInfoList(hostNodeList.getNodeInfoList().stream()
                    .map(TaskNodeInfoDTO::toServiceTaskHostNodeDTO).collect(Collectors.toList()));
            } else {
                targetServer.setNodeInfoList(Collections.emptyList());
            }
            targetServer.setDynamicGroupId(hostNodeList.getDynamicGroupId());
            if (hostNodeList.getHostList() != null) {
                List<ServiceHostInfoDTO> hostInfoDTOS = new ArrayList<>();
                hostNodeList.getHostList().forEach(hostNode -> {
                    ServiceHostInfoDTO hostInfoDTO = new ServiceHostInfoDTO();
                    if (hostNode.getHostId() != null) {
                        hostInfoDTO.setHostId(hostNode.getHostId());
                    }
                    hostInfoDTO.setCloudAreaId(hostNode.getCloudAreaId());
                    hostInfoDTO.setIp(hostNode.getIp());
                    hostInfoDTOS.add(hostInfoDTO);
                });
                targetServer.setHostList(hostInfoDTOS);
            }
            targetDTO.setTargetServer(targetServer);
        }
        return targetDTO;
    }

    public String toJsonString() {
        if (StringUtils.isNotBlank(variable)) {
            this.hostNodeList = null;
        } else {
            this.variable = null;
            if (hostNodeList == null) {
                return "null";
            }
        }
        return JsonMapper.nonEmptyMapper().toJson(this);
    }

    @Override
    public String toString() {
        log.info("TaskTargetDTO_toString");
        return toJsonString();
    }
}
