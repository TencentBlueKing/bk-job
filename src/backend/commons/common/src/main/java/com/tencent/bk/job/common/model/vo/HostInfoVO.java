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

package com.tencent.bk.job.common.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @since 7/11/2019 16:08
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("主机信息")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostInfoVO {

    @ApiModelProperty(value = "主机ID", required = true)
    private Long hostId;

    @ApiModelProperty("主机 IP")
    private String ip;

    @ApiModelProperty("主机 IPv6")
    private String ipv6;

    @ApiModelProperty("展示用的IP，主要针对多内网IP问题")
    private String displayIp;

    @ApiModelProperty("主机名称")
    private String hostName;

    // agent状态：-2：未找到，-1：查询失败，0：初始安装，1：启动中，2：运行中，3：有损状态，4：繁忙，5：升级中，6：停止中，7：解除安装
    @JsonIgnore
    private Integer agentStatus;

    @ApiModelProperty("agent 状态 0-异常 1-正常")
    private Integer alive;

    @ApiModelProperty("云区域信息")
    private CloudAreaInfoVO cloudArea;

    /**
     * 操作系统
     */
    @ApiModelProperty("操作系统")
    private String osName;

    @ApiModelProperty("系统类型")
    @JsonProperty("osType")
    private String osTypeName;

    @ApiModelProperty("AgentId")
    private String agentId;

    @ApiModelProperty("所属云厂商")
    @JsonProperty("cloudVendor")
    private String cloudVendorName;

    public void validate() throws InvalidParamException {
        if (!JobContextUtil.isAllowMigration() && (hostId == null || hostId <= 0)) {
            log.warn("Missing host_id!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostInfoVO)) return false;
        HostInfoVO that = (HostInfoVO) o;
        return Objects.equals(hostId, that.hostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostId);
    }
}
