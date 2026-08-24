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

package com.tencent.bk.job.file_gateway.model.resp.inner;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "文件源")
@Data
public class ServiceFileSourceDTO {
    /**
     * id
     */
    @Schema(description = "id")
    private Integer id;
    /**
     * 业务Id
     */
    @Schema(description = "业务Id")
    private Long appId;
    /**
     * 文件源标识
     */
    @Schema(description = "文件源标识")
    private String code;
    /**
     * 文件源别名
     */
    @Schema(description = "文件源别名")
    private String alias;
    /**
     * 状态
     */
    @Schema(description = "状态：-1：文件Worker不存在，0：文件Worker异常，1：文件Worker正常")
    private Integer status;
    /**
     * 存储类型
     */
    @Schema(description = "存储类型：FILE_SYSTEM(文件系统)，OSS(对象存储)")
    private String storageType;
    /**
     * 类型
     */
    @Schema(description = "类型")
    private ServiceFileSourceTypeDTO fileSourceType;
    /**
     * 区域Code
     */
    @Schema(description = "区域Code")
    private String regionCode;
    /**
     * 区域名称
     */
    @Schema(description = "区域名称")
    private String regionName;
    /**
     * EndPoint域名
     */
    @Schema(description = "EndPoint域名")
    private String endPointDomain;
    /**
     * EndPoint自定义字段Json串
     */
    @Schema(description = "EndPoint自定义字段Json串")
    private String customInfo;
    /**
     * 是否为公共文件源
     */
    @Schema(description = "是否为公共文件源")
    private Boolean publicFlag;
    /**
     * 共享的业务Id列表
     */
    @Schema(description = "共享的业务Id列表")
    private List<Long> sharedAppIdList;
    /**
     * 是否共享到全业务
     */
    @Schema(description = "是否共享到全业务", required = true)
    private Boolean shareToAllApp;
    /**
     * 凭证Id
     */
    @Schema(description = "凭证Id")
    private String credentialId;
    /**
     * 文件前缀
     */
    @Schema(description = "文件前缀")
    private String filePrefix;
    /**
     * 接入点选择范围
     */
    @Schema(description = "接入点选择范围:APP/PUBLIC/ALL，分别为业务私有接入点/公共接入点/全部")
    private String workerSelectScope;
    /**
     * 接入点选择模式
     */
    @Schema(description = "接入点选择模式：AUTO/MANUAL，分别为自动选择/手动选择")
    private String workerSelectMode;
    /**
     * 接入点Id
     */
    @Schema(description = "接入点Id")
    private Long workerId;
    /**
     * 是否开启
     */
    @Schema(description = "是否开启")
    private Boolean enable;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String creator;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Long createTime;
    /**
     * 更新人
     */
    @Schema(description = "更新人")
    private String lastModifyUser;

    @Schema(description = "更新时间")
    private Long lastModifyTime;
}

