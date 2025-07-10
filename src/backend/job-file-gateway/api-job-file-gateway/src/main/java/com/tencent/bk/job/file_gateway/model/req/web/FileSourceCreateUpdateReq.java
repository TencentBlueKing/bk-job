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

package com.tencent.bk.job.file_gateway.model.req.web;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import com.tencent.bk.job.file_gateway.consts.FileSourceTypeEnum;
import com.tencent.bk.job.file_gateway.validate.ValidFileSourceInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("文件源创建、更新请求")
public class FileSourceCreateUpdateReq {

    /**
     * 文件源Code
     */
    @ApiModelProperty(value = "文件源Code", required = true)
    @NotBlankField(fieldName = "code")
    private String code;
    /**
     * 文件源别名
     */
    @ApiModelProperty(value = "文件源名称", required = true)
    @NotBlankField(fieldName = "alias")
    private String alias;

    @ApiModelProperty(value = "存储类型", required = true)
    @NotBlankField(fieldName = "storageType")
    private String storageType;

    @ApiModelProperty(value = "文件源类型Code", required = true)
    @NotBlankField(fieldName = "fileSourceTypeCode")
    private String fileSourceTypeCode;
    /**
     * 文件源信息Map
     */
    @ApiModelProperty(value = "文件源信息Map")
    @ValidFileSourceInfo
    private Map<String, Object> fileSourceInfoMap;
    /**
     * 是否为公共文件源
     */
    @ApiModelProperty(value = "是否为公共文件源", required = true)
    private Boolean publicFlag;
    /**
     * 共享的资源范围列表
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
    @NotBlankField(fieldName = "credentialId")
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
    @NotBlankField(fieldName = "workerSelectScope")
    private String workerSelectScope;
    /**
     * 接入点选择模式
     */
    @ApiModelProperty(value = "接入点选择模式：AUTO/MANUAL，分别为自动/手动", required = true)
    @NotBlankField(fieldName = "workerSelectMode")
    private String workerSelectMode;
    /**
     * 接入点Id
     */
    @ApiModelProperty(value = "接入点Id，手动选择时传入，自动选择不传")
    private Long workerId;

    /**
     * 判断是否为蓝鲸制品库类型的文件源
     *
     * @return 布尔值
     */
    @JsonIgnore
    public boolean isBlueKingArtifactoryType() {
        return FileSourceTypeEnum.isBlueKingArtifactory(fileSourceTypeCode);
    }

    /**
     * 获取蓝鲸制品库根地址
     *
     * @return 蓝鲸制品库根地址
     */
    @JsonIgnore
    public String getBkArtifactoryBaseUrl() {
        return (String) fileSourceInfoMap.get(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL);
    }
}
