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

package com.tencent.bk.job.execute.util;

import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteObjectDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ExecuteObjectCompositeKeyUtils {

    public static List<ExecuteObjectCompositeKey> fromEsbHostParams(List<Long> hostIds, List<EsbIpDTO> esbIps) {
        if (CollectionUtils.isNotEmpty(hostIds)) {
            // hostId方式作为主机标识
            return hostIds.stream()
                .map(ExecuteObjectCompositeKey::ofHostId)
                .collect(Collectors.toList());
        } else if (CollectionUtils.isNotEmpty(esbIps)) {
            EsbIpDTO anyEsbIp = esbIps.get(0);
            if (anyEsbIp.getHostId() != null) {
                // hostId方式作为主机标识
                return esbIps.stream()
                    .map(esbIp -> ExecuteObjectCompositeKey.ofHostId(esbIp.getHostId()))
                    .collect(Collectors.toList());
            } else {
                // 管控区域+ip方式作为主机标识
                return esbIps.stream()
                    .map(esbIp -> ExecuteObjectCompositeKey.ofHostIp(
                        IpUtils.buildCloudIp(esbIp.getBkCloudId(), esbIp.getIp())))
                    .collect(Collectors.toList());
            }
        } else {
            throw new IllegalArgumentException("Invalid host params");
        }
    }


    public static ExecuteObjectCompositeKey fromHostParam(Long hostId, Long bkCloudId, String ip) {
        if (hostId != null) {
            // hostId方式作为主机标识
            return ExecuteObjectCompositeKey.ofHostId(hostId);
        } else if (bkCloudId != null && ip != null) {
            // 管控区域+ip方式作为主机标识
            return ExecuteObjectCompositeKey.ofHostIp(IpUtils.buildCloudIp(bkCloudId, ip));
        } else {
            throw new IllegalArgumentException("Invalid host params");
        }
    }

    public static List<ExecuteObjectCompositeKey> fromOpenApiExecuteObjectDTOList(
        List<OpenApiExecuteObjectDTO> executeObjects) {

        return executeObjects.stream()
            .map(executeObject ->
                ExecuteObjectCompositeKey.ofExecuteObjectResource(
                    ExecuteObjectTypeEnum.valOf(executeObject.getType()), executeObject.getResourceId()))
            .collect(Collectors.toList());
    }
}
