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

package com.tencent.bk.job.manage.model.dto.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 2/12/2019 21:31
 */
@PersistenceObject
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskHostNodeDTO {

    @JsonProperty("nodeInfoList")
    private List<TaskNodeInfoDTO> nodeInfoList;

    @JsonProperty("dynamicGroupId")
    private List<String> dynamicGroupId;

    @JsonProperty("hostList")
    private List<ApplicationHostDTO> hostList;

    public static TaskHostNodeVO toVO(TaskHostNodeDTO hostNode) {
        if (hostNode == null) {
            return null;
        }
        TaskHostNodeVO hostNodeVO = new TaskHostNodeVO();
        if (CollectionUtils.isNotEmpty(hostNode.getNodeInfoList())) {
            hostNodeVO.setNodeList(
                hostNode.getNodeInfoList().parallelStream()
                    .map(TaskNodeInfoDTO::toVO).collect(Collectors.toList()));
        }
        hostNodeVO.setDynamicGroupIdList(hostNode.getDynamicGroupId());
        if (hostNode.getHostList() != null) {
            hostNodeVO
                .setHostList(hostNode.getHostList().stream()
                    .filter(Objects::nonNull)
                    .map(ApplicationHostDTO::toVO)
                    .collect(Collectors.toList()));
        }
        return hostNodeVO;
    }

    public static TaskHostNodeDTO fromVO(TaskHostNodeVO hostNode) {
        if (hostNode == null) {
            return null;
        }
        TaskHostNodeDTO taskHostNodeDTO = new TaskHostNodeDTO();
        if (CollectionUtils.isNotEmpty(hostNode.getNodeList())) {
            taskHostNodeDTO.setNodeInfoList(
                hostNode.getNodeList().parallelStream()
                    .map(TaskNodeInfoDTO::fromVO).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(hostNode.getDynamicGroupIdList())) {
            taskHostNodeDTO.setDynamicGroupId(hostNode.getDynamicGroupIdList());
        }
        if (CollectionUtils.isNotEmpty(hostNode.getHostList())) {
            taskHostNodeDTO
                .setHostList(hostNode.getHostList().stream()
                    .map(ApplicationHostDTO::fromVO).collect(Collectors.toList()));
        }
        return taskHostNodeDTO;
    }
}
