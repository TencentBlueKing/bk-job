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

package com.tencent.bk.job.manage.model.web.vo.whiteip;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * IP白名单VO
 */
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("IP白名单")
@Data
public class WhiteIPRecordVO {
    @ApiModelProperty("IP白名单ID")
    private Long id;
    @ApiModelProperty("云区域ID")
    private Long cloudAreaId;
    @ApiModelProperty("主机列表")
    private List<WhiteIPHostVO> hostList;
    @ApiModelProperty("生效范围")
    private List<ActionScopeVO> actionScopeList;
    @ApiModelProperty("业务")
    private List<AppVO> appList;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("创建人")
    private String creator;
    @ApiModelProperty("创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    @ApiModelProperty("最后一次更新人")
    private String lastModifier;
    @ApiModelProperty("最后一次更新时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;
}
