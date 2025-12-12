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

package com.tencent.bk.job.logsvr.model.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 文件分发执行日志
 */
@ApiModel("文件分发执行日志")
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceFileTaskLogDTO {
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
     * 目标主机（云区域ID:IPv4)
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @JsonProperty("destIp")
    private String destIp;

    /**
     * 目标主机（云区域ID:IPv6)
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @JsonProperty("destIpv6")
    private String destIpv6;

    /**
     * 目标主机ID
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @JsonProperty("destHostId")
    private Long destHostId;

    /**
     * 目标执行对象ID
     */
    @JsonProperty("destExecuteObjectId")
    private String destExecuteObjectId;

    /**
     * 目标文件路径
     */
    @JsonProperty("destFile")
    private String destFile;

    /**
     * 文件源主机(云区域ID:IPv4)
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @JsonProperty("srcIp")
    private String srcIp;

    /**
     * 文件源主机(云区域ID:IPv6)
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @JsonProperty("srcIpv6")
    private String srcIpv6;

    /**
     * 文件源主机ID
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @JsonProperty("srcHostId")
    private Long srcHostId;

    /**
     * 文件源执行对象ID
     */
    @JsonProperty("srcExecuteObjectId")
    private String srcExecuteObjectId;

    /**
     * 源文件类型
     */
    @JsonProperty("srcFileType")
    private Integer srcFileType;

    /**
     * 源文件路径 - 真实路径
     */
    @JsonProperty("srcFile")
    private String srcFile;

    /**
     * 源文件路径 - 用于显示
     */
    @JsonProperty("displaySrcFile")
    private String displaySrcFile;

    /**
     * 文件大小
     */
    @JsonProperty("size")
    private String size;

    /**
     * 文件任务状态
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 文件任务状态描述
     */
    @JsonProperty("statusDesc")
    private String statusDesc;

    /**
     * 速度
     */
    @JsonProperty("speed")
    private String speed;

    /**
     * 进度
     */
    @JsonProperty("process")
    private String process;

    /**
     * 日志内容
     */
    @CompatibleImplementation(
        deprecatedVersion = "3.12.1",
        explain = "兼容获取文件分发日志内容，content字段为老用法，新版本使用contentList，发布完成后重构代码移除对此字段使用",
        type = CompatibleType.HISTORY_LOGIC
    )
    @JsonProperty("content")
    private String content;

    /**
     * 新版本使用，时区改造时添加
     * 日志内容列表（包含时间戳和原始日志）
     */
    @JsonProperty("contentList")
    private List<FileTaskTimeAndRawLogDTO> contentList;

    public ServiceFileTaskLogDTO(Integer mode,
                                 String destExecuteObjectId,
                                 String destFile,
                                 String srcExecuteObjectId,
                                 Integer srcFileType,
                                 String srcFile,
                                 String displaySrcFile,
                                 String size,
                                 Integer status,
                                 String statusDesc,
                                 String speed,
                                 String process,
                                 List<FileTaskTimeAndRawLogDTO> contentList) {
        this.mode = mode;
        this.destExecuteObjectId = destExecuteObjectId;
        this.destFile = destFile;
        this.srcExecuteObjectId = srcExecuteObjectId;
        this.srcFileType = srcFileType;
        this.srcFile = srcFile;
        this.displaySrcFile = displaySrcFile;
        this.size = size;
        this.status = status;
        this.statusDesc = statusDesc;
        this.speed = speed;
        this.process = process;
        this.contentList = contentList;
    }

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    public ServiceFileTaskLogDTO(Integer mode,
                                 Long destHostId,
                                 String destIp,
                                 String destIpv6,
                                 String destFile,
                                 Long srcHostId,
                                 String srcIp,
                                 String srcIpv6,
                                 Integer srcFileType,
                                 String srcFile,
                                 String displaySrcFile,
                                 String size,
                                 Integer status,
                                 String statusDesc,
                                 String speed,
                                 String process,
                                 List<FileTaskTimeAndRawLogDTO> contentList) {
        this.mode = mode;
        this.destHostId = destHostId;
        this.destIp = destIp;
        this.destIpv6 = destIpv6;
        this.destFile = destFile;
        this.srcHostId = srcHostId;
        this.srcIp = srcIp;
        this.srcIpv6 = srcIpv6;
        this.srcFileType = srcFileType;
        this.srcFile = srcFile;
        this.displaySrcFile = displaySrcFile;
        this.size = size;
        this.status = status;
        this.statusDesc = statusDesc;
        this.speed = speed;
        this.process = process;
        this.contentList = contentList;
    }

    /**
     * 从contentList中获取完整的日志内容，包括时间和原始日志
     * @return 日志内容
     */
    @JsonIgnore
    public String getFullContentWithTime() {
        // 优先使用contentList（新版本）
        if (CollectionUtils.isNotEmpty(contentList)) {
            StringBuilder sb = new StringBuilder();
            for (FileTaskTimeAndRawLogDTO timeAndRawLog : contentList) {
                // mongodb中，同一个文件分发任务，新老结构日志共存的情况
                if (timeAndRawLog.getTime() == null) {
                    sb.append(timeAndRawLog.getRawLog());
                } else {
                    ZoneId zoneId =
                        JobContextUtil.getTimeZone() != null ? JobContextUtil.getTimeZone() : ZoneId.systemDefault();
                    String timeStr = DateUtils.formatUnixTimestamp(
                        timeAndRawLog.getTime(),
                        ChronoUnit.MILLIS,
                        DateUtils.FILE_TASK_LOG_FORMAT,
                        zoneId
                    );
                    sb.append("[").append(timeStr).append("] ").append(timeAndRawLog.getRawLog());
                }
            }
            return sb.toString();
        }
        return "";
    }
}


