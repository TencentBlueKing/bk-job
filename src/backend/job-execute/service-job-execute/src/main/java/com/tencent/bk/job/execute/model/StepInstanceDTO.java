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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 步骤实例
 */
@Getter
@Setter
@ToString(exclude = {"dbPass"})
public class StepInstanceDTO extends StepInstanceBaseDTO {
    /**
     * 任务超时时间
     */
    private Integer timeout;
    /**
     * 系统账号ID
     */
    private Long accountId;
    /**
     * 目标机器的执行账户名
     */
    private String account;
    /**
     * 执行账号别名
     */
    private String accountAlias;
    // --------------脚本步骤字段--------------//
    /**
     * 脚本ID
     */
    private String scriptId;

    /**
     * 脚本版本ID
     */
    private Long scriptVersionId;

    /**
     * 执行脚本的内容
     */
    private String scriptContent;

    /**
     * 脚本名称
     */
    private String scriptName;

    /**
     * 脚本来源：1-手工录入 2-引用业务脚本 3-引用公共脚本
     */
    private Integer scriptSource;

    /**
     * 执行脚本的类型:1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)
     *
     * @see com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum
     */
    private Integer scriptType;
    /**
     * 执行脚本的执行参数
     */
    private String scriptParam;
    /**
     * 变量解析之后的执行脚本的执行参数
     */
    private String resolvedScriptParam;
    /**
     * 是否敏感参数
     */
    private boolean secureParam;

    /**
     * 步骤实例增加db SQL执行的账号,密码,端口,SQL内容,参数
     */
    private String dbAccount;
    private String dbPass;
    private Integer dbPort;
    private Integer dbType;
    private Long dbAccountId;

    // --------------文件步骤字段--------------//
    /**
     * 文件传输的源文件
     */
    private List<FileSourceDTO> fileSourceList;
    /**
     * 文件传输的目标目录
     */
    private String fileTargetPath;

    /**
     * 文件分发到目标主机的对应名称
     */
    private String fileTargetName;
    /**
     * 变量解析之后的目标路径
     */
    private String resolvedFileTargetPath;
    /**
     * 上传文件限速，单位KB
     */
    private Integer fileUploadSpeedLimit;

    /**
     * 下载文件限速，单位KB
     */
    private Integer fileDownloadSpeedLimit;
    /**
     * 文件分发-重名文件处理，1-覆盖，2-追加源IP目录
     */
    private Integer fileDuplicateHandle = DuplicateHandlerEnum.OVERWRITE.getId();
    /**
     * 目标路径不存在：路径处理，1-直接创建，2-直接失败
     */
    private Integer notExistPathHandler;
    // --------------人工确认步骤字段--------------//
    private String confirmMessage;
    private String confirmReason;
    private List<String> confirmUsers;
    private List<String> confirmRoles;
    private List<String> notifyChannels;

    // --------------构造函数-------------------//
    public StepInstanceDTO() {
    }

    public StepInstanceDTO(StepInstanceBaseDTO stepInstanceBase) {
        this.id = stepInstanceBase.id;
        this.executeCount = stepInstanceBase.executeCount;
        this.stepId = stepInstanceBase.stepId;
        this.taskInstanceId = stepInstanceBase.taskInstanceId;
        this.appId = stepInstanceBase.appId;
        this.name = stepInstanceBase.name;
        this.executeType = stepInstanceBase.executeType;
        this.operator = stepInstanceBase.operator;
        this.status = stepInstanceBase.status;
        this.startTime = stepInstanceBase.startTime;
        this.endTime = stepInstanceBase.endTime;
        this.totalTime = stepInstanceBase.totalTime;
        this.createTime = stepInstanceBase.createTime;
        this.ignoreError = stepInstanceBase.ignoreError;
        this.targetServers = stepInstanceBase.targetServers;
        this.rollingConfigId = stepInstanceBase.rollingConfigId;
        this.batch = stepInstanceBase.getBatch();
    }

    // -------------公共方法------------------//
    public void fillScriptStepInfo(ScriptStepInstanceDTO scriptStepInstance) {
        if (scriptStepInstance == null) {
            return;
        }
        this.timeout = scriptStepInstance.getTimeout();
        this.accountId = scriptStepInstance.getAccountId();
        this.account = scriptStepInstance.getAccount();
        this.scriptId = scriptStepInstance.getScriptId();
        this.scriptVersionId = scriptStepInstance.getScriptVersionId();
        this.scriptContent = scriptStepInstance.getScriptContent();
        this.scriptType = scriptStepInstance.getScriptType();
        this.scriptParam = scriptStepInstance.getScriptParam();
        this.dbAccount = scriptStepInstance.getDbAccount();
        this.dbAccountId = scriptStepInstance.getDbAccountId();
        this.dbPass = scriptStepInstance.getDbPass();
        this.dbType = scriptStepInstance.getDbType();
        this.dbPort = scriptStepInstance.getDbPort();
        this.scriptSource = scriptStepInstance.getScriptSource();
        this.resolvedScriptParam = scriptStepInstance.getResolvedScriptParam();
        this.secureParam = scriptStepInstance.isSecureParam();
    }

    public void fillFileStepInfo(FileStepInstanceDTO fileStepInstance) {
        if (fileStepInstance == null) {
            return;
        }
        this.timeout = fileStepInstance.getTimeout();
        this.accountId = fileStepInstance.getAccountId();
        this.account = fileStepInstance.getAccount();
        this.fileSourceList = fileStepInstance.getFileSourceList();
        this.fileTargetPath = fileStepInstance.getFileTargetPath();
        this.fileTargetName = fileStepInstance.getFileTargetName();
        this.resolvedFileTargetPath = fileStepInstance.getResolvedFileTargetPath();
        this.fileUploadSpeedLimit = fileStepInstance.getFileUploadSpeedLimit();
        this.fileDownloadSpeedLimit = fileStepInstance.getFileDownloadSpeedLimit();
        this.fileDuplicateHandle = fileStepInstance.getFileDuplicateHandle();
        this.notExistPathHandler = fileStepInstance.getNotExistPathHandler();
    }

    public void fillConfirmStepInfo(ConfirmStepInstanceDTO confirmStepInstance) {
        if (confirmStepInstance == null) {
            return;
        }
        this.confirmMessage = confirmStepInstance.getConfirmMessage();
        this.confirmReason = confirmStepInstance.getConfirmReason();
        this.confirmUsers = confirmStepInstance.getConfirmUsers();
        this.confirmRoles = confirmStepInstance.getConfirmRoles();
        this.notifyChannels = confirmStepInstance.getNotifyChannels();
    }
}
