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

package com.tencent.bk.job.file_gateway.model.req.web;


import com.tencent.bk.job.common.model.dto.ResourceScope;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("文件源创建、更新请求")
public class FileSourceCreateUpdateReq {

    @ApiModelProperty(value = "ID,更新文件源的时候需要传入，新建文件源不需要", required = false)
    private Integer id;
    /**
     * 文件源Code
     */
    @ApiModelProperty(value = "文件源Code", required = true)
    private String code;
    /**
     * 文件源别名
     */
    @ApiModelProperty(value = "文件源名称", required = true)
    private String alias;

    @ApiModelProperty(value = "存储类型", required = true)
    private String storageType;

    @ApiModelProperty(value = "文件源类型Code", required = true)
    private String fileSourceTypeCode;
    /**
     * 文件源信息Map
     */
    @ApiModelProperty(value = "文件源信息Map")
    private Map<String, Object> fileSourceInfoMap;
    /**
     * 是否为公共文件源
     */
    @ApiModelProperty(value = "是否为公共文件源", required = true)
    private Boolean publicFlag;
    /**
     * 共享的业务Id集合
     */
    @ApiModelProperty(value = "共享的资源范围列表", required = true)
    private List<ResourceScope> sharedScopeList;
    /**
     * 是否共享到全业务
     */
    @ApiModelProperty(value = "是否共享到全业务", required = true)
    private Boolean shareToAllApp;
    /**
     * 文件源凭证Id
     */
    @ApiModelProperty(value = "文件源凭证Id", required = true)
    private String credentialId;
    /**
     * 文件前缀
     */
    @ApiModelProperty(value = "文件前缀：后台自动生成UUID传${UUID}，自定义字符串直接传")
    private String filePrefix;
    /**
     * 接入点选择范围
     */
    @ApiModelProperty(value = "接入点选择范围:APP/PUBLIC/ALL，分别为业务私有接入点/公共接入点/全部", required = true)
    private String workerSelectScope;
    /**
     * 接入点选择模式
     */
    @ApiModelProperty(value = "接入点选择模式：AUTO/MANUAL，分别为自动/手动", required = true)
    private String workerSelectMode;
    /**
     * 接入点Id
     */
    @ApiModelProperty(value = "接入点Id，手动选择时传入，自动选择不传")
    private Long workerId;
}
