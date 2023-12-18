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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
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
@Document
public class FileTaskLogDoc {
    /**
     * 步骤实例ID
     */
    @Field(FileTaskLogDocField.STEP_ID)
    private Long stepId;
    /**
     * 执行次数
     */
    @Field(FileTaskLogDocField.EXECUTE_COUNT)
    private Integer executeCount;
    /**
     * 滚动执行批次
     */
    @Field(FileTaskLogDocField.BATCH)
    private Integer batch;
    /**
     * 任务ID
     */
    @Field(FileTaskLogDocField.TASK_ID)
    private String taskId;

    /**
     * 文件任务模式，mode: 0-upload;1-download
     */
    @Field(FileTaskLogDocField.MODE)
    private Integer mode;

    /**
     * 执行对象 ID。当mode=0时,executeObjectId=上传源的执行对象ID;mode=1时,executeObjectId=下载目标的执行对象ID
     */
    @Field(FileTaskLogDocField.EXECUTE_OBJECT_ID)
    private String executeObjectId;

    /**
     * hostId。当mode=0时,hostId=上传源hostId;mode=1时,hostId=下载目标hostId
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.HOST_ID)
    private Long hostId;

    /**
     * 文件源-执行对象 ID
     */
    @Field(FileTaskLogDocField.SRC_EXECUTE_OBJECT_ID)
    private String srcExecuteObjectId;

    /**
     * 文件源主机(云区域:IPv4)
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.SRC_IP)
    private String srcIp;

    /**
     * 文件源主机(云区域:IPv6)
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.SRC_IPV6)
    private String srcIpv6;

    /**
     * 文件源主机ID
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.SRC_HOST_ID)
    private Long srcHostId;

    /**
     * 目标-执行对象 ID
     */
    @Field(FileTaskLogDocField.DEST_EXECUTE_OBJECT_ID)
    private String destExecuteObjectId;

    /**
     * 目标主机(云区域:IPv4)
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.DEST_IP)
    private String destIp;

    /**
     * 目标主机(云区域:IPv6)
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.DEST_IPV6)
    private String destIpv6;

    /**
     * 目标主机主机ID
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Deprecated
    @Field(FileTaskLogDocField.DEST_HOST_ID)
    private Long destHostId;

    /**
     * 目标文件路径
     */
    @Field(FileTaskLogDocField.DEST_FILE)
    private String destFile;

    /**
     * 源文件路径 - 用于显示
     */
    @Field(FileTaskLogDocField.DISPLAY_SRC_FILE)
    private String displaySrcFile;

    /**
     * 源文件路径 - 真实路径
     */
    @Field(FileTaskLogDocField.SRC_FILE)
    private String srcFile;

    /**
     * 源文件类型
     */
    @Field(FileTaskLogDocField.SRC_FILE_TYPE)
    private Integer srcFileType;

    /**
     * 文件大小
     */
    @Field(FileTaskLogDocField.SIZE)
    private String size;

    /**
     * 文件任务状态
     */
    @Field(FileTaskLogDocField.STATUS)
    private Integer status;

    /**
     * 文件任务状态描述
     */
    @Field(FileTaskLogDocField.STATUS_DESC)
    private String statusDesc;

    /**
     * 速度
     */
    @Field(FileTaskLogDocField.SPEED)
    private String speed;

    /**
     * 进度
     */
    @Field(FileTaskLogDocField.PROCESS)
    private String process;

    /**
     * 日志内容在mongodb中按照list的方式存储
     */
    @Field(FileTaskLogDocField.CONTENT_LIST)
    private List<String> contentList;

    /**
     * 拼接处理后的日志的内容。该字段不会写入到db
     */
    private String content;

    public static FileTaskLogDoc convert(ServiceFileTaskLogDTO serviceFileLog) {
        FileTaskLogDoc fileLog = new FileTaskLogDoc();
        fileLog.setContent(serviceFileLog.getContent());
        fileLog.setMode(serviceFileLog.getMode());
        if (FileTaskModeEnum.UPLOAD.getValue().equals(serviceFileLog.getMode())) {
            fileLog.setHostId(serviceFileLog.getSrcHostId());
            fileLog.setExecuteObjectId(serviceFileLog.getSrcExecuteObjectId());
        } else if (FileTaskModeEnum.DOWNLOAD.getValue().equals(serviceFileLog.getMode())) {
            fileLog.setHostId(serviceFileLog.getDestHostId());
            fileLog.setExecuteObjectId(serviceFileLog.getDestExecuteObjectId());
            // dest
            fileLog.setDestExecuteObjectId(serviceFileLog.getDestExecuteObjectId());
            fileLog.setDestHostId(serviceFileLog.getDestHostId());
            fileLog.setDestIp(serviceFileLog.getDestIp());
            fileLog.setDestIpv6(serviceFileLog.getDestIpv6());
            fileLog.setDestFile(serviceFileLog.getDestFile());
        }
        // source
        fileLog.setSrcExecuteObjectId(serviceFileLog.getSrcExecuteObjectId());
        fileLog.setSrcHostId(serviceFileLog.getSrcHostId());
        fileLog.setSrcIp(serviceFileLog.getSrcIp());
        fileLog.setSrcIpv6(serviceFileLog.getSrcIpv6());
        fileLog.setSrcFile(serviceFileLog.getSrcFile());
        fileLog.setDisplaySrcFile(serviceFileLog.getDisplaySrcFile());
        fileLog.setSrcFileType(serviceFileLog.getSrcFileType());

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
        fileLog.setSrcExecuteObjectId(srcExecuteObjectId);
        fileLog.setSrcHostId(srcHostId);
        fileLog.setSrcIp(srcIp);
        fileLog.setSrcIpv6(srcIpv6);
        fileLog.setSrcFileType(srcFileType);
        fileLog.setDisplaySrcFile(displaySrcFile);
        fileLog.setSrcFile(srcFile);
        if (FileTaskModeEnum.DOWNLOAD.getValue().equals(mode)) {
            fileLog.setDestExecuteObjectId(destExecuteObjectId);
            fileLog.setDestHostId(destHostId != null ? destHostId : hostId);
            fileLog.setDestIp(destIp);
            fileLog.setDestIpv6(destIpv6);
            fileLog.setDestFile(destFile);
        }
        fileLog.setProcess(process);
        fileLog.setSize(size);
        fileLog.setSpeed(speed);
        fileLog.setStatusDesc(statusDesc);
        fileLog.setStatus(status);
        return fileLog;
    }

    public String buildTaskId() {
        StringBuilder sb = new StringBuilder();
        sb.append(mode).append("_");
        if (FileTaskModeEnum.UPLOAD.getValue().equals(mode)) {
            sb.append(buildCompatibleExecuteObjectId(executeObjectId, hostId))
                .append("_")
                .append(displaySrcFile);
        } else {
            sb.append(buildCompatibleExecuteObjectId(srcExecuteObjectId, srcHostId))
                .append("_")
                .append(displaySrcFile)
                .append("_")
                .append(buildCompatibleExecuteObjectId(executeObjectId, hostId))
                .append("_")
                .append(destFile);
        }
        return sb.toString();
    }

    private String buildCompatibleExecuteObjectId(String executeObjectId, Long hostId) {
        return StringUtils.isNotEmpty(executeObjectId) ? executeObjectId : String.valueOf(hostId);
    }

    public String getTaskId() {
        return this.taskId == null ? buildTaskId() : this.taskId;
    }
}
