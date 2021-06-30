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

package com.tencent.bk.job.manage.model.inner;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("步骤文件信息")
public class ServiceTaskFileStepDTO {

    @ApiModelProperty("源文件列表")
    private List<ServiceTaskFileInfoDTO> originFileList;

    @ApiModelProperty("目标路径")
    private String destinationFileLocation;

    @ApiModelProperty("执行账户")
    private ServiceAccountDTO account;

    @ApiModelProperty("目标机器列表")
    private ServiceTaskTargetDTO executeTarget;

    @ApiModelProperty("下载限速，单位MB")
    private Integer downloadSpeedLimit;

    @ApiModelProperty("上传限速，单位MB")
    private Integer uploadSpeedLimit;

    @ApiModelProperty("超时时间，单位秒")
    private Integer timeout;

    @ApiModelProperty(value = "是否自动忽略错误")
    private Boolean ignoreError;

    @ApiModelProperty(value = "文件重名处理，1-覆盖，2-追加源IP目录")
    private Integer fileDuplicateHandle;

    @ApiModelProperty(value = "目标路径不存在：路径处理，1-直接创建，2-直接失败")
    private Integer notExistPathHandler;
}
