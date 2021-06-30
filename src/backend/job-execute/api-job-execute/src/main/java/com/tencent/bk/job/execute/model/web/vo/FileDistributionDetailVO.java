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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("文件分发执行详情")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDistributionDetailVO implements Comparable {
    @ApiModelProperty(name = "taskId", value = "文件任务ID,用于检索单个文件分发的结果")
    private String taskId;
    @ApiModelProperty(name = "destIp", value = "下载目标IP")
    private String destIp;
    @ApiModelProperty(name = "srcIp", value = "上传源IP")
    private String srcIp;
    @ApiModelProperty("文件名称")
    private String fileName;
    @ApiModelProperty("文件大小")
    private String fileSize;
    @ApiModelProperty("状态,0-Pulling,1-Waiting,2-Uploading,3-Downloading,4-Finished,5-Failed")
    private Integer status;
    @ApiModelProperty("状态描述")
    private String statusDesc;
    @ApiModelProperty("速率")
    private String speed;
    @ApiModelProperty("进度")
    private String progress;
    @ApiModelProperty("文件任务上传下载标识,0-上传,1-下载")
    private Integer mode;
    @ApiModelProperty("日志内容")
    private String logContent;

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return 1;
        }
        FileDistributionDetailVO other = (FileDistributionDetailVO) o;
        // 从文件源拉取文件的详情日志放在最前面
        if (this.status == 0 && other.status > 0) {
            return -1;
        }
        int compareFileNameResult = compareString(this.fileName, other.getFileName());
        if (compareFileNameResult != 0) {
            return compareFileNameResult;
        }
        return compareString(this.srcIp, other.getSrcIp());
    }

    private int compareString(String a, String b) {
        if (a == null && b == null) {
            return 0;
        } else if (a != null && b == null) {
            return 1;
        } else if (a == null) {
            return -1;
        } else {
            return a.compareTo(b);
        }
    }
}
