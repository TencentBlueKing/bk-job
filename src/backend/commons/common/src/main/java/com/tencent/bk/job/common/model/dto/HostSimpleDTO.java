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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 主机
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class HostSimpleDTO {

    /**
     * 主机ID
     */
    private Long hostId;
    /**
     * cmdb业务ID
     */
    private Long bizId;
    /**
     * 主机Agent状态
     */
    private Integer gseAgentAlive;
    /**
     * 云区域+ip
     */
    private String cloudIp;

    public ApplicationHostDTO convertToHostDTO(){
        ApplicationHostDTO hostDTO = new ApplicationHostDTO();
        Long cloudAreaId = Long.valueOf(this.getCloudIp().split(":")[0]);
        String ip = this.getCloudIp().split(":")[1];
        hostDTO.setIp(ip);
        hostDTO.setCloudAreaId(cloudAreaId);
        hostDTO.setCloudIp(this.getCloudIp());
        hostDTO.setGseAgentAlive(this.getGseAgentAlive().intValue() == 1);
        hostDTO.setBizId(this.getBizId());
        hostDTO.setHostId(this.getHostId());
        return hostDTO;
    }
}