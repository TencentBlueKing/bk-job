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
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

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

    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty("兼容字段，请勿使用：IP列表")
    private List<String> ipList;

    @ApiModelProperty("主机列表")
    private List<WhiteIPHostVO> hostList;
    @ApiModelProperty("生效范围")
    private List<ActionScopeVO> actionScopeList;

    @ApiModelProperty(value = "是否对所有资源范围生效，默认为false")
    private boolean allScope = false;

    @ApiModelProperty("生效的资源范围列表")
    private List<ScopeVO> scopeList;

    @ApiModelProperty("兼容字段，请勿使用：生效业务列表")
    private List<ScopeVO> appList;

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

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setScopeList(List<ScopeVO> scopeList) {
        this.scopeList = scopeList;
        this.appList = scopeList;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setHostList(List<WhiteIPHostVO> hostList) {
        this.hostList = hostList;
        this.ipList = getIpListByHostList(hostList);
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public List<String> getIpListByHostList(List<WhiteIPHostVO> hostList) {
        if (hostList != null) {
            return hostList.stream().map(WhiteIPHostVO::getIpv4).collect(Collectors.toList());
        }
        return null;
    }
}
