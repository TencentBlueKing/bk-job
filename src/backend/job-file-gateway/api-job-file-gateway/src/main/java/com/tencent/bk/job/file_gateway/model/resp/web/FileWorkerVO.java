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

package com.tencent.bk.job.file_gateway.model.resp.web;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel("文件接入点")
@Data
public class FileWorkerVO {
    /**
     * id
     */
    @ApiModelProperty("id")
    private Long id;
    /**
     * 资源范围类型
     */
    @ApiModelProperty(value = "资源范围类型", allowableValues = "biz-业务,biz_set-业务集")
    private String scopeType;
    /**
     * 资源范围ID
     */
    @ApiModelProperty("资源范围ID")
    private String scopeId;
    /**
     * 名称
     */
    @ApiModelProperty("名称")
    private String name;
    /**
     * 描述
     */
    @ApiModelProperty("描述")
    private String description;
    /**
     * 密钥
     */
    @ApiModelProperty("密钥")
    private String token;
    /**
     * 所在机器云区域Id
     */
    @ApiModelProperty("所在机器云区域Id")
    private Long cloudAreaId;
    /**
     * 内网IP
     */
    @ApiModelProperty("内网IP")
    private String innerIp;
    /**
     * 能力标签
     */
    @ApiModelProperty("能力标签列表")
    private List<String> abilityTagList;
    /**
     * Ping延迟
     */
    @ApiModelProperty("Ping延迟")
    private Integer latency;
    /**
     * CPU负载
     */
    @ApiModelProperty("CPU负载")
    private Float cpuOverload;
    /**
     * 内存使用率
     */
    @ApiModelProperty("内存使用率")
    private Float memRate;
    /**
     * 内存剩余
     */
    @ApiModelProperty("内存剩余")
    private Float memFreeSpace;
    /**
     * 磁盘使用率
     */
    @ApiModelProperty("磁盘使用率")
    private Float diskRate;
    /**
     * 磁盘剩余
     */
    @ApiModelProperty("磁盘剩余")
    private Float diskFreeSpace;
    /**
     * Worker版本
     */
    @ApiModelProperty("Worker版本")
    private String version;
    /**
     * Worker在线状态
     */
    @ApiModelProperty("Worker在线状态")
    private Byte onlineStatus;
    /**
     * 上一次心跳时间
     */
    @ApiModelProperty("上一次心跳时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastHeartBeat;
    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    private String creator;
    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    /**
     * 最后修改人
     */
    @ApiModelProperty("最后修改人")
    private String lastModifyUser;
    /**
     * 最后修改时间
     */
    @ApiModelProperty("最后修改时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;
}

