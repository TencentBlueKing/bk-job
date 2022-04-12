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
import java.util.Map;

@ApiModel("文件源")
@Data
public class FileSourceVO {
    /**
     * id
     */
    @ApiModelProperty("id")
    private Integer id;

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
     * 文件源标识
     */
    @ApiModelProperty("文件源标识")
    private String code;
    /**
     * 文件源别名
     */
    @ApiModelProperty("文件源别名")
    private String alias;
    /**
     * 状态
     */
    @ApiModelProperty("状态：-1：文件Worker不存在，0：文件Worker异常，1：文件Worker正常")
    private Integer status;
    /**
     * 存储类型
     */
    @ApiModelProperty("存储类型：FILE_SYSTEM(文件系统)，OSS(对象存储)")
    private String storageType;
    /**
     * 类型
     */
    @ApiModelProperty("类型")
    private FileSourceTypeVO fileSourceType;
    /**
     * 文件源信息Map
     */
    @ApiModelProperty("文件源信息Map")
    private Map<String, Object> fileSourceInfoMap;
    /**
     * 是否为公共文件源
     */
    @ApiModelProperty("是否为公共文件源")
    private Boolean publicFlag;
    /**
     * 共享的业务Id列表
     */
    @ApiModelProperty("共享的业务Id列表")
    private List<Long> sharedAppIdList;
    /**
     * 是否共享到全业务
     */
    @ApiModelProperty(value = "是否共享到全业务", required = true)
    private Boolean shareToAllApp;
    /**
     * 凭证Id
     */
    @ApiModelProperty("凭证Id")
    private String credentialId;
    /**
     * 文件前缀
     */
    @ApiModelProperty("文件前缀")
    private String filePrefix;
    /**
     * 接入点选择范围
     */
    @ApiModelProperty("接入点选择范围:APP/PUBLIC/ALL，分别为业务私有接入点/公共接入点/全部")
    private String workerSelectScope;
    /**
     * 接入点选择模式
     */
    @ApiModelProperty("接入点选择模式：AUTO/MANUAL，分别为自动选择/手动选择")
    private String workerSelectMode;
    /**
     * 接入点Id
     */
    @ApiModelProperty("接入点Id")
    private Long workerId;
    /**
     * 是否开启
     */
    @ApiModelProperty("是否开启")
    private Boolean enable;
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
     * 更新人
     */
    @ApiModelProperty("更新人")
    private String lastModifyUser;

    @ApiModelProperty("更新时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    @ApiModelProperty("是否可以管理")
    private Boolean canManage;

    @ApiModelProperty("是否可以查看")
    private Boolean canView;
}

