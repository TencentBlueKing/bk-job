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

package com.tencent.bk.job.manage.model.dto.converter;

import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskNodeInfoDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskHostNodeConverter {
    public static ServiceTaskHostNodeDTO convertToServiceTaskHostNodeDTO(TaskHostNodeDTO taskHostNodeDTO) {
        if (taskHostNodeDTO == null) {
            return null;
        }
        ServiceTaskHostNodeDTO serviceTaskHostNodeDTO = new ServiceTaskHostNodeDTO();
        serviceTaskHostNodeDTO.setDynamicGroupId(taskHostNodeDTO.getDynamicGroupId());
        List<ApplicationHostInfoDTO> hostInfoDTOList = taskHostNodeDTO.getHostList();
        List<ServiceHostInfoDTO> serviceHostInfoDTOList = new ArrayList<>();
        if (hostInfoDTOList != null && !hostInfoDTOList.isEmpty()) {
            serviceHostInfoDTOList =
                hostInfoDTOList.parallelStream()
                    .map(HostInfoConverter::convertToServiceHostInfoDTO).collect(Collectors.toList());
        }
        serviceTaskHostNodeDTO.setHostList(serviceHostInfoDTOList);
        List<TaskNodeInfoDTO> taskNodeInfoDTOList = taskHostNodeDTO.getNodeInfoList();
        List<ServiceTaskNodeInfoDTO> serviceTaskNodeInfoDTOList = new ArrayList<>();
        if (taskNodeInfoDTOList != null && !taskNodeInfoDTOList.isEmpty()) {
            serviceTaskNodeInfoDTOList =
                taskNodeInfoDTOList.parallelStream()
                    .map(TaskNodeInfoConverter::convertToServiceTaskNodeInfoDTO).collect(Collectors.toList());
        }
        serviceTaskHostNodeDTO.setNodeInfoList(serviceTaskNodeInfoDTOList);
        return serviceTaskHostNodeDTO;
    }
}
