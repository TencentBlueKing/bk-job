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

package com.tencent.bk.job.execute.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件步骤批次实例
 */
@Getter
@Setter
public class StepInstanceFileBatchDTO {
    /**
     * ID
     */
    protected Long id;
    /**
     * 任务实例id
     */
    protected Long taskInstanceId;
    /**
     * 步骤实例id
     */
    protected Long stepInstanceId;
    /**
     * 源文件批次
     */
    protected int batch;
    /**
     * 当前批次传输的源文件
     */
    private List<FileSourceDTO> fileSourceList;

    public StepInstanceFileBatchDTO() {
    }

    /**
     * 获取简单描述
     *
     * @return 简单描述
     */
    public String getSimpleDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append("StepInstanceFileBatchDTO(");
        sb.append("taskInstanceId=").append(taskInstanceId);
        sb.append(",stepInstanceId=").append(stepInstanceId);
        sb.append(",batch=").append(batch);
        if (!CollectionUtils.isEmpty(fileSourceList)) {
            sb.append(",fileSourceListSize=").append(fileSourceList.size());
            if (fileSourceList.size() <= 20) {
                sb.append(",fileSourceList=").append(
                    fileSourceList.stream()
                        .map(FileSourceDTO::getSimpleDesc)
                        .collect(Collectors.toList())
                );
            } else {
                sb.append(",fileSourceList=")
                    .append(
                        fileSourceList.subList(0, 20)
                            .stream()
                            .map(FileSourceDTO::getSimpleDesc)
                            .collect(Collectors.toList())
                    )
                    .append("...");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
