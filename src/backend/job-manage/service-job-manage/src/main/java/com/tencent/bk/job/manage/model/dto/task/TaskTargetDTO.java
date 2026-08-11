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
import com.tencent.bk.job.common.model.vo.ContainerVO;
import com.tencent.bk.job.common.model.vo.TaskExecuteObjectsInfoVO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.ip.IpUtils;
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

    /**
     * 当步骤中当执行对象为变量时的变量名，若是手动添加的执行对象，该字段为空
     */
    @JsonProperty("variable")
    private String variable;

    @JsonProperty("hostNodeList")
    private TaskHostNodeDTO hostNodeList;

    @JsonProperty("containerList")
    private List<TaskTargetContainerDTO> containerList;

    public static TaskTargetVO toVO(TaskTargetDTO executeTarget) {
        if (executeTarget == null) {
            return null;
        }
        TaskTargetVO taskTargetVO = new TaskTargetVO();
        taskTargetVO.setVariable(executeTarget.getVariable());
        // 主机对象
        TaskHostNodeVO taskHostNodeVO = TaskHostNodeDTO.toVO(executeTarget.getHostNodeList());
        if (taskHostNodeVO != null) {
            taskTargetVO.setHostNodeInfo(taskHostNodeVO);
            TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = new TaskExecuteObjectsInfoVO();
            taskExecuteObjectsInfoVO.setHostList(taskHostNodeVO.getHostList());
            taskExecuteObjectsInfoVO.setNodeList(taskHostNodeVO.getNodeList());
            taskExecuteObjectsInfoVO.setDynamicGroupList(taskHostNodeVO.getDynamicGroupList());
            taskTargetVO.setExecuteObjectsInfo(taskExecuteObjectsInfoVO);
        }

        // 容器对象
        if (CollectionUtils.isNotEmpty(executeTarget.getContainerList())) {
            List<ContainerVO> containerVOList = executeTarget.getContainerList().stream()
                .map(TaskTargetContainerDTO::toContainerVO)
                .collect(Collectors.toList());
            if (taskTargetVO.getExecuteObjectsInfo() != null) {
                taskTargetVO.getExecuteObjectsInfo().setContainerList(containerVOList);
            } else {
                TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = new TaskExecuteObjectsInfoVO();
                taskExecuteObjectsInfoVO.setContainerList(containerVOList);
                taskTargetVO.setExecuteObjectsInfo(taskExecuteObjectsInfoVO);
            }
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
        // 主机对象
        taskTargetDTO.setHostNodeList(TaskHostNodeDTO.fromVO(taskTargetVO.getExecuteObjectsInfoCompatibly()));
        // 容器对象
        if (taskTargetVO.getExecuteObjectsInfo() != null
            && CollectionUtils.isNotEmpty(taskTargetVO.getExecuteObjectsInfo().getContainerList())) {
            taskTargetDTO.setContainerList(
                taskTargetVO.getExecuteObjectsInfo().getContainerList()
                    .stream()
                    .map(TaskTargetContainerDTO::fromContainerVO)
                    .collect(Collectors.toList())
            );
        }
        fillHostDetail(taskTargetDTO);
        return taskTargetDTO;
    }

    private static void fillHostDetail(TaskTargetDTO target) {
        if (target.getHostNodeList() == null || CollectionUtils.isEmpty(target.getHostNodeList().getHostList())) {
            return;
        }
        List<ApplicationHostDTO> hostList = target.getHostNodeList().getHostList();
        Set<Long> hostIds = collectHostIds(hostList);
        Set<String> hostCloudIps = collectCloudIps(hostList);

        if (hostIds.isEmpty() && hostCloudIps.isEmpty()) {
            // 没有任何可用查询键时不必查询，但仍要走匹配流程，保持“匹配不到置 -1”的既有行为
            fillHostDetail(hostList, Collections.emptyMap(), Collections.emptyMap());
            return;
        }

        CurrentTenantHostService currentTenantHostService =
            ApplicationContextRegister.getBean(CurrentTenantHostService.class);
        fillHostDetail(hostList,
            currentTenantHostService.listHostsByHostIds(hostIds),
            currentTenantHostService.listHostsByIps(hostCloudIps));
    }

    static Set<Long> collectHostIds(List<ApplicationHostDTO> hostList) {
        Set<Long> hostIds = new HashSet<>();
        for (ApplicationHostDTO host : hostList) {
            if (isValidHostId(host.getHostId())) {
                hostIds.add(host.getHostId());
            }
        }
        return hostIds;
    }

    /**
     * 历史脏数据中 hostId 可能为 -1（匹配不到时的占位值），这类值不是有效的主机标识，不应作为查询与匹配的键
     */
    private static boolean isValidHostId(Long hostId) {
        return hostId != null && hostId > 0;
    }

    static Set<String> collectCloudIps(List<ApplicationHostDTO> hostList) {
        Set<String> cloudIps = new HashSet<>();
        for (ApplicationHostDTO host : hostList) {
            String cloudIp = buildValidCloudIp(host);
            if (cloudIp != null) {
                cloudIps.add(cloudIp);
            }
        }
        return cloudIps;
    }

    /**
     * 构造可用于精确匹配的 cloudIp；cloudAreaId 或 IPv4 缺失时返回 null。
     * <p>
     * 判据只能基于 cloudAreaId/ip 字段本身，不能用 ApplicationHostDTO.getCloudIp()：
     * 后者在 cloudIp 字段为空时会拼接出 "0:null"、"null:null" 这类在多台主机间完全相同的退化键，
     * 永远不为 blank，用它判空会让守卫失效。
     */
    private static String buildValidCloudIp(ApplicationHostDTO host) {
        if (host.getCloudAreaId() == null || StringUtils.isBlank(host.getIp())) {
            return null;
        }
        return IpUtils.buildCloudIp(host.getCloudAreaId(), host.getIp());
    }

    static void fillHostDetail(List<ApplicationHostDTO> hostList,
                               Map<Long, ApplicationHostDTO> hostIdHostMapping,
                               Map<String, ApplicationHostDTO> cloudIpHostMapping) {
        for (ApplicationHostDTO hostNode : hostList) {
            // hostId 是主机唯一标识，优先按 hostId 匹配
            ApplicationHostDTO hostDTO = isValidHostId(hostNode.getHostId())
                ? hostIdHostMapping.get(hostNode.getHostId()) : null;
            if (hostDTO == null) {
                // hostId 缺失或查不到（备份恢复导入、历史脏数据）时回退到 cloudIp 匹配
                String cloudIp = buildValidCloudIp(hostNode);
                if (cloudIp != null) {
                    hostDTO = cloudIpHostMapping.get(cloudIp);
                }
            }
            if (hostDTO == null) {
                log.warn("Cannot find host by hostId={} or by cloudIp={}",
                    hostNode.getHostId(), hostNode.getCloudIp());
                hostNode.setHostId(-1L);
                continue;
            }
            hostNode.setHostId(hostDTO.getHostId());
            hostNode.setAgentId(hostDTO.getAgentId());
            hostNode.setCloudAreaId(hostDTO.getCloudAreaId());
            hostNode.setIp(hostDTO.getIp());
            hostNode.setIpv6(hostDTO.getIpv6());
            hostNode.setDisplayIp(hostDTO.getDisplayIp());
            hostNode.setOsName(hostDTO.getOsName());
            hostNode.setOsType(hostDTO.getOsType());
            hostNode.setGseAgentStatus(hostDTO.getGseAgentStatus());
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
        // 主机对象
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
        // 容器对象
        if (CollectionUtils.isNotEmpty(containerList)) {
            targetDTO.setContainerList(containerList.stream().map(TaskTargetContainerDTO::toServiceTargetContainerDTO)
                .collect(Collectors.toList()));
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
