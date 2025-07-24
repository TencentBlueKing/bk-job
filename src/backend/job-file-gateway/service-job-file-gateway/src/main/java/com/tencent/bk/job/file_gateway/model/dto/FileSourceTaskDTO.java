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

package com.tencent.bk.job.file_gateway.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件源后台任务
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FileSourceTaskDTO {
    /**
     * id
     */
    private String id;
    /**
     * 所属批量任务Id
     */
    private String batchTaskId;
    /**
     * appId
     */
    private Long appId;
    /**
     * 步骤实例Id
     */
    private Long stepInstanceId;
    /**
     * 步骤执行次数
     */
    private Integer executeCount;
    /**
     * 文件任务列表
     */
    private List<FileTaskDTO> fileTaskList;
    /**
     * 文件源Id
     */
    private Integer fileSourceId;
    /**
     * 文件Worker Id
     */
    private Long fileWorkerId;
    /**
     * 文件源任务状态
     */
    private Byte status;
    /**
     * FileWorker上的文件是否被清理
     */
    private Boolean fileCleared;
    /**
     * 创建者
     */
    private String creator;
    /**
     * 创建时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    /**
     * 更新时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    /**
     * 任务处于终止态
     *
     * @return
     */
    public boolean isDone() {
        return TaskStatusEnum.isDone(status);
    }
}
