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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@ApiModel("主机信息")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecuteHostVO {
    @ApiModelProperty("主机ID")
    private Long hostId;

    @ApiModelProperty("主机 IP")
    private String ip;

    @ApiModelProperty("主机 IPv6")
    private String ipv6;

    @ApiModelProperty("agent 状态 0-异常 1-正常")
    private Integer alive;

    @ApiModelProperty("云区域ID")
    private Long cloudId;
    @ApiModelProperty("云区域信息")
    private ExecuteCloudAreaInfoVO cloudAreaInfo;

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", deprecatedVersion = "3.8.0")
    public void setCloudAreaInfo(ExecuteCloudAreaInfoVO cloudAreaInfo) {
        this.cloudAreaInfo = cloudAreaInfo;
        if (cloudAreaInfo != null) {
            this.cloudId = cloudAreaInfo.getId();
        }
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", deprecatedVersion = "3.8.0")
    public void setCloudId(Long cloudId) {
        this.cloudId = cloudId;
        this.cloudAreaInfo = new ExecuteCloudAreaInfoVO(cloudId, null);
    }
}
