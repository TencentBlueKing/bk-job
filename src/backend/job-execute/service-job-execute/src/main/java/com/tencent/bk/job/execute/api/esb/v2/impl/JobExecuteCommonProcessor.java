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

package com.tencent.bk.job.execute.api.esb.v2.impl;

import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.EsbServerDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业执行公用
 */
@Slf4j
public class JobExecuteCommonProcessor {

    /**
     * 转换目标服务器
     *
     * @param requestTargetServers
     * @param ipList
     * @param dynamicGroupIdList
     * @return
     */
    protected ExecuteTargetDTO convertToStandardServers(EsbServerDTO requestTargetServers, List<EsbIpDTO> ipList,
                                                        List<String> dynamicGroupIdList) {
        // 优先使用servers参数
        if (requestTargetServers != null) {
            ExecuteTargetDTO executeTargetDTO = new ExecuteTargetDTO();
            if (requestTargetServers.getTopoNodes() != null) {
                List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
                requestTargetServers.getTopoNodes().forEach(
                    topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(), topoNode.getNodeType())));
                executeTargetDTO.setTopoNodes(topoNodes);
            }
            if (requestTargetServers.getDynamicGroupIds() != null) {
                List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
                requestTargetServers.getDynamicGroupIds().forEach(
                    groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
                executeTargetDTO.setDynamicServerGroups(dynamicServerGroups);
            }
            if (requestTargetServers.getIps() != null) {
                List<HostDTO> staticIpList = new ArrayList<>();
                requestTargetServers.getIps().forEach(ip -> staticIpList.add(new HostDTO(ip.getBkCloudId(),
                    ip.getIp())));
                executeTargetDTO.setStaticIpList(staticIpList);
            }
            return executeTargetDTO;
        } else {
            // 兼容历史版本API
            ExecuteTargetDTO executeTargetDTO = new ExecuteTargetDTO();
            if (ipList != null) {
                List<HostDTO> staticIpList = new ArrayList<>();
                ipList.forEach(ip -> staticIpList.add(new HostDTO(ip.getBkCloudId(), ip.getIp())));
                executeTargetDTO.setStaticIpList(staticIpList);
            }
            if (dynamicGroupIdList != null) {
                List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
                dynamicGroupIdList.forEach(groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
                executeTargetDTO.setDynamicServerGroups(dynamicServerGroups);
            }
            return executeTargetDTO;
        }
    }
}
