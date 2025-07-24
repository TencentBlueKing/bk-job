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

package com.tencent.bk.job.execute.model.esb.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class EsbStepInstanceDetailV3DTO {

    /**
     * 步骤实例ID
     */
    private Long id;

    /**
     * 步骤类型：1-脚本，2-文件，3-人工确认
     */
    @JsonProperty("type")
    private Integer type;
    /**
     * 步骤名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 脚本步骤信息
     */
    @JsonProperty("script_step_info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ScriptStepInfo scriptStepInfo;

    /**
     * 文件步骤信息
     */
    @JsonProperty("file_step_info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FileStepInfo fileStepInfo;

    /**
     * 审批步骤信息
     */
    @JsonProperty("approval_step_info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ApprovalStepInfo approvalStepInfo;

    @Setter
    @Getter
    public static class ScriptStepInfo {

        /**
         * 脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本
         */
        @JsonProperty("script_source")
        private Integer scriptSource;

        /**
         * 脚本 ID
         */
        @JsonProperty("script_id")
        private String scriptId;

        /**
         * 脚本版本 ID
         */
        @JsonProperty("script_version_id")
        private Long scriptVersionId;

        /**
         * 脚本内容
         */
        private String content;

        /**
         * 脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql
         */
        @JsonProperty("script_language")
        private Integer scriptLanguage;

        /**
         * 脚本参数
         */
        @JsonProperty("script_param")
        private String scriptParam;

        /**
         * 脚本超时时间，单位为秒
         */
        private Integer timeout;

        /**
         * 执行账号ID
         */
        @JsonProperty("account_id")
        private Long accountId;

        /**
         * 执行账号名称
         */
        @JsonProperty("account_name")
        private String accountName;

        /**
         * 执行目标机器
         */
        @JsonProperty("execute_target")
        private EsbServerV3DTO executeTarget;

        /**
         * 参数是否为敏感参数：0-不敏感，1-敏感
         */
        @JsonProperty("secure_param")
        private Integer secureParam;

        /**
         * 是否忽略错误：0-不忽略，1-忽略
         */
        @JsonProperty("ignore_error")
        private Integer ignoreError;
    }

    @Setter
    @Getter
    public static class FileStepInfo {

        /**
         * 源文件列表
         */
        @JsonProperty("file_source_list")
        private List<FileSource> fileSourceList;
        /**
         * 目标信息
         */
        @JsonProperty("file_destination")
        private FileDestination fileDestination;

        /**
         * 超时，单位为秒
         */
        private Integer timeout;

        /**
         * 上传文件限速，单位为MB/s，没有值表示不限速
         */
        @JsonProperty("upload_speed_limit")
        private Integer uploadSpeedLimit;

        /**
         * 下载文件限速，单位为MB/s，没有值表示不限速
         */
        @JsonProperty("download_speed_limit")
        private Integer downloadSpeedLimit;

        /**
         * 传输模式： 1-严谨模式，2-强制模式，3-安全模式
         */
        @JsonProperty("transfer_mode")
        private Integer transferMode;

        /**
         * 是否忽略错误：0-不忽略，1-忽略
         */
        @JsonProperty("ignore_error")
        private Integer ignoreError;
    }

    @Setter
    @Getter
    public static class FileSource {

        /**
         * 文件类型：1-服务器文件，2-本地文件，3-文件源文件
         */
        @JsonProperty("file_type")
        private Integer fileType;

        /**
         * 文件路径列表
         */
        @JsonProperty("file_location")
        private List<String> fileLocation;

        /**
         * 文件Hash值，仅本地文件该字段有值
         */
        @JsonProperty("file_hash")
        private String fileHash;

        /**
         * 文件大小，单位为字节，仅本地文件该字段有值
         */
        @JsonProperty("file_size")
        private Integer fileSize;

        /**
         * 源文件所在机器
         */
        private EsbServerV3DTO host;

        /**
         * 执行账号ID
         */
        @JsonProperty("account_id")
        private Long accountId;

        /**
         * 执行账号名称
         */
        @JsonProperty("account_name")
        private String accountName;

        /**
         * 第三方文件源ID
         */
        @JsonProperty("file_source_id")
        private Long fileSourceId;

        /**
         * 第三方文件源名称
         */
        @JsonProperty("file_source_name")
        private String fileSourceName;
    }

    @Setter
    @Getter
    public static class FileDestination {
        /**
         * 目标路径
         */
        private String path;

        /**
         * 执行账号ID
         */
        @JsonProperty("account_id")
        private Long accountId;

        /**
         * 执行账号名称
         */
        @JsonProperty("account_name")
        private String accountName;

        /**
         * 分发目标机器
         */
        @JsonProperty("target_server")
        private EsbServerV3DTO targetServer;
    }

    @Setter
    @Getter
    public static class ApprovalStepInfo {
        /**
         * 确认消息
         */
        @JsonProperty("approval_message")
        private String approvalMessage;
    }
}
