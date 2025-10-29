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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class V4ExecuteTargetConverter {

    public static ExecuteTargetDTO v4ToExecuteTargetDTO(V4ExecuteTargetDTO v4ExecuteTargetDTO) {
        if (v4ExecuteTargetDTO == null || v4ExecuteTargetDTO.isTargetEmpty()) {
            return null;
        }

        ExecuteTargetDTO target = new ExecuteTargetDTO();

        // 主机列表
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getHostList())) {
            List<HostDTO> staticHostList = new ArrayList<>();
            for (OpenApiV4HostDTO host : v4ExecuteTargetDTO.getHostList()) {
                // 优先使用hostId
                if (host.getBkHostId() != null) {
                    staticHostList.add(HostDTO.fromHostId(host.getBkHostId()));
                } else {
                    staticHostList.add(new HostDTO(host.getBkCloudId(), host.getIp()));
                }
            }
            target.setStaticIpList(staticHostList);
        }

        // 动态分组
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getDynamicGroups())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            v4ExecuteTargetDTO.getDynamicGroups().forEach(
                group -> dynamicServerGroups.add(new DynamicServerGroupDTO(group.getId())));
            target.setDynamicServerGroups(dynamicServerGroups);
        }

        // 静态拓扑节点
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getTopoNodes())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            v4ExecuteTargetDTO.getTopoNodes().forEach(topoNode ->
                topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(), topoNode.getNodeType()))
            );
            target.setTopoNodes(topoNodes);
        }

        return target;
    }
}
