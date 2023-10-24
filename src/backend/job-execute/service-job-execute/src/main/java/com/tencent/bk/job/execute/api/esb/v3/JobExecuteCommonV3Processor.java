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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业执行公用
 */
@Slf4j
public class JobExecuteCommonV3Processor {

    protected ValidateResult checkServer(EsbServerV3DTO server) {
        if (server == null) {
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "target_server");
        }

        if (!server.checkHostParamsNonEmpty()) {
            return ValidateResult.fail(ErrorCode.SERVER_EMPTY);
        }
        return ValidateResult.pass();
    }

    protected ServersDTO convertToServersDTO(EsbServerV3DTO server) {
        if (server == null) {
            return null;
        }
        ServersDTO serversDTO = new ServersDTO();

        // 拓扑节点
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            server.getTopoNodes().forEach(topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(),
                topoNode.getNodeType())));
            serversDTO.setTopoNodes(topoNodes);
        }

        // 动态分组
        if (CollectionUtils.isNotEmpty(server.getDynamicGroups())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            server.getDynamicGroups().forEach(
                group -> dynamicServerGroups.add(new DynamicServerGroupDTO(group.getId())));
            serversDTO.setDynamicServerGroups(dynamicServerGroups);
        }

        // 主机列表，优先使用hostId
        if (CollectionUtils.isNotEmpty(server.getHostIds())) {
            serversDTO.setStaticIpList(
                server.getHostIds().stream().map(HostDTO::fromHostId).collect(Collectors.toList()));
        } else if (CollectionUtils.isNotEmpty(server.getIps())) {
            serversDTO.setStaticIpList(
                server.getIps().stream()
                    .map(host -> new HostDTO(host.getBkCloudId(), host.getIp()))
                    .collect(Collectors.toList()));
        }

        return serversDTO;
    }
}
