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

package com.tencent.bk.job.file_gateway.model.req.inner;

import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class HeartBeatReq {
    @ApiModelProperty(value = "ID")
    Long id;
    @ApiModelProperty(value = "名称")
    String name;
    @ApiModelProperty(value = "标签列表")
    List<String> tagList;
    @ApiModelProperty(value = "业务ID", required = true)
    Long appId;
    @ApiModelProperty(value = "密钥", required = true)
    String token;
    @ApiModelProperty(value = "访问worker使用的host", required = true)
    String accessHost;
    @ApiModelProperty(value = "访问worker使用的port", required = true)
    Integer accessPort;
    @ApiModelProperty(value = "worker所在云区域Id", required = true)
    Long cloudAreaId;
    @ApiModelProperty(value = "worker的内网IP", required = true)
    String innerIp;
    @ApiModelProperty(value = "能力标签列表")
    List<String> abilityTagList;
    @ApiModelProperty(value = "CPU负载")
    Float cpuOverload;
    @ApiModelProperty(value = "内存使用率")
    Float memRate;
    @ApiModelProperty(value = "内存空闲空间")
    Float memFreeSpace;
    @ApiModelProperty(value = "磁盘使用率")
    Float diskRate;
    @ApiModelProperty(value = "磁盘剩余空间")
    Float diskFreeSpace;
    @ApiModelProperty(value = "worker版本", required = true)
    String version;
    @ApiModelProperty(value = "在线状态", required = true)
    Byte onlineStatus;
    @ApiModelProperty(value = "FileWorker配置信息", required = true)
    FileWorkerConfig fileWorkerConfig;

    @Override
    public String toString() {
        return "HeartBeatReq{" +
            "id=" + id +
            ", appId=" + appId +
            ", token='" + token + '\'' +
            ", accessHost='" + accessHost + '\'' +
            ", accessPort=" + accessPort +
            ", cloudAreaId=" + cloudAreaId +
            ", innerIp='" + innerIp + '\'' +
            ", abilityTagList=" + abilityTagList +
            ", cpuOverload=" + cpuOverload +
            ", memRate=" + memRate +
            ", memFreeSpace=" + memFreeSpace +
            ", diskRate=" + diskRate +
            ", diskFreeSpace=" + diskFreeSpace +
            ", version='" + version + '\'' +
            ", onlineStatus=" + onlineStatus +
            '}';
    }
}
