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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * 文件分发执行日志
 */
@Data
@NoArgsConstructor
public class AtomicFileTaskLog {
    /**
     * 文件任务ID
     */
    @JsonProperty("taskId")
    private String taskId;

    /**
     * 文件分发类型，mode: upload-0/download-1
     */
    @JsonProperty("mode")
    private Integer mode;

    /**
     * 文件源-执行对象
     */
    private ExecuteObject srcExecuteObject;

    /**
     * 分发目标-执行对象
     */
    private ExecuteObject destExecuteObject;

    /**
     * 目标文件路径
     */
    private String destFile;

    /**
     * 源文件类型
     */
    private Integer srcFileType;

    /**
     * 源文件路径 - 真实路径
     */
    private String srcFile;

    /**
     * 源文件路径 - 用于显示
     */
    private String displaySrcFile;

    /**
     * 文件大小
     */
    private String size;

    /**
     * 文件任务状态
     */
    private Integer status;

    /**
     * 文件任务状态描述
     */
    private String statusDesc;

    /**
     * 速度
     */
    private String speed;

    /**
     * 进度
     */
    private String process;

    /**
     * 日志内容
     */
    private String content;

    public AtomicFileTaskLog(Integer mode,
                             ExecuteObject destExecuteObject,
                             String destFile,
                             ExecuteObject srcExecuteObject,
                             Integer srcFileType,
                             String srcFile,
                             String displaySrcFile,
                             String size,
                             Integer status,
                             String statusDesc,
                             String speed,
                             String process,
                             String content) {
        this.mode = mode;
        this.destExecuteObject = destExecuteObject;
        this.destFile = destFile;
        this.srcExecuteObject = srcExecuteObject;
        this.srcFileType = srcFileType;
        this.srcFile = srcFile;
        this.displaySrcFile = displaySrcFile;
        this.size = size;
        this.status = status;
        this.statusDesc = statusDesc;
        this.speed = speed;
        this.process = process;
        this.content = content;
    }

    public static AtomicFileTaskLog fromServiceExecuteObjectLogDTO(
        ServiceFileTaskLogDTO fileTaskLog,
        Function<ServiceFileTaskLogDTO, ExecuteObject> srcExecuteObjectProvider,
        Function<ServiceFileTaskLogDTO, ExecuteObject> destExecuteObjectProvider) {
        AtomicFileTaskLog atomicFileTaskLog = new AtomicFileTaskLog();
        atomicFileTaskLog.setMode(fileTaskLog.getMode());
        atomicFileTaskLog.setSrcExecuteObject(srcExecuteObjectProvider.apply(fileTaskLog));
        atomicFileTaskLog.setSrcFile(fileTaskLog.getSrcFile());
        atomicFileTaskLog.setSrcFileType(fileTaskLog.getSrcFileType());
        atomicFileTaskLog.setDisplaySrcFile(fileTaskLog.getDisplaySrcFile());
        atomicFileTaskLog.setDestExecuteObject(destExecuteObjectProvider.apply(fileTaskLog));
        atomicFileTaskLog.setDestFile(fileTaskLog.getDestFile());
        atomicFileTaskLog.setProcess(fileTaskLog.getProcess());
        atomicFileTaskLog.setContent(fileTaskLog.getContent());
        atomicFileTaskLog.setSize(fileTaskLog.getSize());
        atomicFileTaskLog.setSpeed(fileTaskLog.getSpeed());
        atomicFileTaskLog.setStatus(fileTaskLog.getStatus());
        atomicFileTaskLog.setStatusDesc(fileTaskLog.getStatusDesc());
        atomicFileTaskLog.setTaskId(fileTaskLog.getTaskId());
        return atomicFileTaskLog;
    }
}


