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

package com.tencent.bk.job.common.model.dto;

import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务器信息
 */
@Data
@NoArgsConstructor
@ApiModel("服务器信息")
public class HostDTO {
    /**
     * 云区域ID
     */
    @ApiModelProperty(value = "云区域ID", required = true)
    private Long cloudAreaId;

    @ApiModelProperty("云区域名称")
    private String cloudAreaName;

    /**
     * 服务器IP
     */
    @ApiModelProperty(value = "服务器IP", required = true)
    private String ip;

    @ApiModelProperty(value = "agent状态，0-异常，1-正常")
    private Integer alive;

    public HostDTO(Long cloudAreaId, String ip) {
        this.cloudAreaId = cloudAreaId;
        this.ip = ip;
    }

    public static HostInfoVO toVO(HostDTO host) {
        if (host == null) {
            return null;
        }
        HostInfoVO hostInfo = new HostInfoVO();
        hostInfo.setIp(host.getIp());
        hostInfo.setAlive(host.getAlive());
        CloudAreaInfoVO cloudAreaInfo = new CloudAreaInfoVO();
        cloudAreaInfo.setId(host.getCloudAreaId());
        cloudAreaInfo.setName(host.getCloudAreaName());
        hostInfo.setCloudAreaInfo(cloudAreaInfo);
        return hostInfo;
    }

    public static HostDTO fromVO(HostInfoVO hostInfo) {
        if (hostInfo == null) {
            return null;
        }
        HostDTO host = new HostDTO();
        host.setIp(hostInfo.getIp());
        host.setCloudAreaId(hostInfo.getCloudAreaInfo().getId());
        host.setCloudAreaName(hostInfo.getCloudAreaInfo().getName());
        host.setAlive(hostInfo.getAlive());
        return host;
    }
}
