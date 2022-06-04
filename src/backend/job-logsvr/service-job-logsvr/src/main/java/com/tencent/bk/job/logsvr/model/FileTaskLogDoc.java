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

package com.tencent.bk.job.logsvr.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 文件任务执行日志 - MongoDB Doc
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Document
public class FileTaskLogDoc {
    /**
     * 任务ID
     */
    @JsonProperty("taskId")
    @Field("taskId")
    private String taskId;

    /**
     * 文件任务模式，mode: 0-upload;1-download
     */
    @JsonProperty("mode")
    @Field("mode")
    private Integer mode;

    /**
     * ip - 真实IP。当mode=0时,ip=上传源IP;mode=1时,ip=下载目标IP
     */
    @CompatibleImplementation(name = "rolling_execute", explain = "兼容字段，后续用hostId替换", version = "3.7.x")
    @JsonProperty("ip")
    @Field("ip")
    private String ip;

    /**
     * hostId。当mode=0时,hostId=上传源hostId;mode=1时,hostId=下载目标hostId
     */
    @JsonProperty("hostId")
    @Field("hostId")
    private Long hostId;

    /**
     * 文件源IP
     */
    @CompatibleImplementation(name = "rolling_execute", explain = "兼容字段，后续用srcHostId替换", version = "3.7.x")
    @JsonProperty("srcIp")
    @Field("srcIp")
    private String srcIp;

    /**
     * 文件源主机ID
     */
    @JsonProperty("srcHostId")
    @Field("srcHostId")
    private Long srcHostId;

    /**
     * 文件源IP - 显示
     */
    @JsonProperty("displaySrcIp")
    @Field("displaySrcIp")
    private String displaySrcIp;

    /**
     * 目标文件路径
     */
    @JsonProperty("destFile")
    @Field("destFile")
    private String destFile;

    /**
     * 源文件路径 - 用于显示
     */
    @JsonProperty("displaySrcFile")
    @Field("displaySrcFile")
    private String displaySrcFile;

    /**
     * 源文件路径 - 真实路径
     */
    @JsonProperty("srcFile")
    @Field("srcFile")
    private String srcFile;

    /**
     * 文件大小
     */
    @JsonProperty("size")
    @Field("size")
    private String size;

    /**
     * 文件任务状态
     */
    @JsonProperty("status")
    @Field("status")
    private Integer status;

    /**
     * 文件任务状态描述
     */
    @JsonProperty("statusDesc")
    @Field("statusDesc")
    private String statusDesc;

    /**
     * 速度
     */
    @JsonProperty("speed")
    @Field("speed")
    private String speed;

    /**
     * 进度
     */
    @JsonProperty("process")
    @Field("process")
    private String process;

    /**
     * 日志内容在mongodb中按照list的方式存储，与mongodb中的字段contentList对应
     */
    @JsonProperty("contentList")
    @Field("contentList")
    private List<String> contentList;

    /**
     * 日志内容
     */
    private String content;

    public static FileTaskLogDoc convert(ServiceFileTaskLogDTO serviceFileLog) {
        FileTaskLogDoc fileLog = new FileTaskLogDoc();
        fileLog.setContent(serviceFileLog.getContent());
        fileLog.setMode(serviceFileLog.getMode());
        if (FileTaskModeEnum.UPLOAD.getValue().equals(serviceFileLog.getMode())) {
            fileLog.setSrcIp(serviceFileLog.getSrcIp());
            fileLog.setIp(serviceFileLog.getSrcIp());
            fileLog.setHostId(serviceFileLog.getSrcHostId());
            fileLog.setSrcFile(serviceFileLog.getSrcFile());
            fileLog.setDisplaySrcFile(serviceFileLog.getDisplaySrcFile());
            fileLog.setDisplaySrcIp(serviceFileLog.getDisplaySrcIp());
        } else if (FileTaskModeEnum.DOWNLOAD.getValue().equals(serviceFileLog.getMode())) {
            fileLog.setHostId(serviceFileLog.getDestHostId());
            fileLog.setIp(serviceFileLog.getDestIp());
            fileLog.setSrcIp(serviceFileLog.getSrcIp());
            fileLog.setSrcHostId(serviceFileLog.getSrcHostId());
            fileLog.setDisplaySrcIp(serviceFileLog.getDisplaySrcIp());
            fileLog.setSrcFile(serviceFileLog.getSrcFile());
            fileLog.setDisplaySrcFile(serviceFileLog.getDisplaySrcFile());
            fileLog.setDestFile(serviceFileLog.getDestFile());
        }
        fileLog.setSize(serviceFileLog.getSize());
        fileLog.setProcess(serviceFileLog.getProcess());
        fileLog.setSpeed(serviceFileLog.getSpeed());
        fileLog.setStatusDesc(serviceFileLog.getStatusDesc());
        fileLog.setStatus(serviceFileLog.getStatus());
        fileLog.setTaskId(fileLog.buildTaskId());
        return fileLog;
    }

    public ServiceFileTaskLogDTO toServiceFileTaskLogDTO() {
        ServiceFileTaskLogDTO fileLog = new ServiceFileTaskLogDTO();
        fileLog.setTaskId(getTaskId());
        if (StringUtils.isNotEmpty(content)) {
            fileLog.setContent(content);
        } else if (CollectionUtils.isNotEmpty(contentList)) {
            content = StringUtils.join(contentList, null);
            fileLog.setContent(content);
        }
        fileLog.setMode(mode);
        fileLog.setSrcHostId(srcHostId);
        fileLog.setSrcIp(srcIp);
        fileLog.setDisplaySrcIp(displaySrcIp);
        fileLog.setDisplaySrcFile(displaySrcFile);
        fileLog.setSrcFile(srcFile);
        fileLog.setDestHostId(hostId);
        fileLog.setDestIp(ip);
        fileLog.setDestFile(destFile);
        fileLog.setProcess(process);
        fileLog.setSize(size);
        fileLog.setSpeed(speed);
        fileLog.setStatusDesc(statusDesc);
        fileLog.setStatus(status);
        return fileLog;
    }

    @CompatibleImplementation(name = "rolling_execute", explain = "兼容方法，后续不再使用ip相关参数，发布完成后删除",
        version = "3.6.x")
    public String buildTaskId() {
        StringBuilder sb = new StringBuilder();
        sb.append(mode).append("_");
        if (FileTaskModeEnum.UPLOAD.getValue().equals(mode)) {
            if (srcHostId != null) {
                sb.append(hostId).append("_");
            } else {
                sb.append(displaySrcIp).append("_");
            }
            sb.append(displaySrcFile);
        } else {
            if (srcHostId != null) {
                sb.append(srcHostId).append("_");
            } else {
                sb.append(displaySrcIp).append("_");
            }
            // 暂时不加入源文件，GSE暂不支持
//            sb.append(displaySrcFile).append("_");
            if (hostId != null) {
                sb.append(hostId).append("_");
            } else {
                sb.append(ip).append("_");
            }
            sb.append(destFile);
        }
        return sb.toString();
    }

    public String getTaskId() {
        return this.taskId == null ? buildTaskId() : this.taskId;
    }
}
