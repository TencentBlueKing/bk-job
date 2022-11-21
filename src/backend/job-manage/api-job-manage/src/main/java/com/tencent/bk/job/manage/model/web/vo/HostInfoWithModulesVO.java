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

package com.tencent.bk.job.manage.model.web.vo;

import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @since 7/11/2019 16:08
 */
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("主机信息")
@Data
public class HostInfoWithModulesVO {

    @ApiModelProperty("服务器 ID")
    private Long hostId;

    @ApiModelProperty("主机 IP")
    private String ip;

    @ApiModelProperty("展示用的IP，主要针对多内网IP问题")
    private String displayIp;

    @ApiModelProperty("描述")
    private String ipDesc;

    @ApiModelProperty("agent 状态 0-异常 1-正常")
    private Integer alive;

    @ApiModelProperty("云区域信息")
    private CloudAreaInfoVO cloudAreaInfo;

    /**
     * 操作系统
     */
    @ApiModelProperty("操作系统")
    private String os;

    @ApiModelProperty("所有模块节点信息")
    private List<BizTopoNode> moduleNodes;

    public boolean validate(boolean isCreate) {
        if (cloudAreaInfo == null) {
            JobContextUtil.addDebugMessage("Missing host info cloud area info!");
            return false;
        }
        if (StringUtils.isNotBlank(ip) && cloudAreaInfo.validate(isCreate)) {
            return true;
        }
        return false;
    }
}
