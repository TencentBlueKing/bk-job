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

package com.tencent.bk.job.file_gateway.model.req.inner;

import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateFileSourceTaskReq {
    @ApiModelProperty(value = "ID")
    String fileSourceTaskId;
    @ApiModelProperty(value = "文件路径（含bucketName）")
    String filePath;
    @ApiModelProperty(value = "文件下载到机器上的真实路径")
    String downloadPath;
    @ApiModelProperty(value = "任务文件状态", required = true)
    TaskStatusEnum status;
    @ApiModelProperty(value = "文件大小（字节）", required = true)
    private Long fileSize;
    @ApiModelProperty(value = "速度", required = true)
    private String speed;
    @ApiModelProperty(value = "进度", required = true)
    private Integer progress;
    @ApiModelProperty(value = "日志内容", required = true)
    private String content;

    public void setContent(String content) {
        if (log.isDebugEnabled()) {
//            this.content = "[" + Thread.currentThread().getName() + "]" + content;
            this.content = content;
        }
    }
}
