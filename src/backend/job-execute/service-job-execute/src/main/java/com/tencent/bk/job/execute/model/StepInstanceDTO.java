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
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.model.inner.ServiceFileStepInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceStepInstanceDTO;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
     */
    private ScriptTypeEnum scriptType;
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
    /**
     * Windows解释器路径
     */
    private String windowsInterpreter;

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
        this.targetExecuteObjects = stepInstanceBase.targetExecuteObjects;
        this.rollingConfigId = stepInstanceBase.rollingConfigId;
        this.batch = stepInstanceBase.getBatch();
        this.stepOrder = stepInstanceBase.getStepOrder();
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
        this.windowsInterpreter = scriptStepInstance.getWindowsInterpreter();
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


    public List<Container> extractStaticContainerList() {
        if (targetExecuteObjects == null) {
            return Collections.emptyList();
        }

        List<Container> containers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(targetExecuteObjects.getStaticContainerList())) {
            containers.addAll(targetExecuteObjects.getStaticContainerList());
        }
        if (isFileStep()) {
            for (FileSourceDTO fileSource : fileSourceList) {
                ExecuteTargetDTO executeTargetDTO = fileSource.getServers();
                if (executeTargetDTO != null
                    && CollectionUtils.isNotEmpty(executeTargetDTO.getStaticContainerList())) {
                    containers.addAll(executeTargetDTO.getStaticContainerList());
                }
            }
        }
        return containers;
    }

    public Set<HostDTO> extractAllHosts() {
        if (targetExecuteObjects == null) {
            return Collections.emptySet();
        }

        Set<HostDTO> hosts = new HashSet<>(targetExecuteObjects.getHostsCompatibly());

        if (CollectionUtils.isNotEmpty(fileSourceList)) {
            fileSourceList.forEach(fileSource -> {
                if (fileSource.getServers() != null) {
                    List<HostDTO> fileSourceHosts = fileSource.getServers().getHostsCompatibly();
                    if (CollectionUtils.isNotEmpty(fileSourceHosts)) {
                        hosts.addAll(fileSourceHosts);
                    }
                }
            });
        }
        return hosts;
    }

    /**
     * 步骤包含的执行对象 ExecuteObjectsDTO 的最终处理
     *
     * @param isSupportExecuteObjectFeature 是否执行执行对象特性
     */
    public void buildStepFinalExecuteObjects(boolean isSupportExecuteObjectFeature) {
        if (targetExecuteObjects != null) {
            targetExecuteObjects.buildMergedExecuteObjects(isSupportExecuteObjectFeature);
        }
        if (CollectionUtils.isNotEmpty(fileSourceList)) {
            fileSourceList.forEach(fileSource -> {
                if (fileSource.getServers() != null) {
                    fileSource.getServers().buildMergedExecuteObjects(isSupportExecuteObjectFeature);
                }
            });
        }
    }

    /**
     * 遍历步骤实例包含的所有执行目标（执行目标+文件分发源执行目标）
     *
     * @param consumer 消费者
     */
    public void forEachExecuteObjects(Consumer<ExecuteTargetDTO> consumer) {
        if (targetExecuteObjects != null) {
            consumer.accept(targetExecuteObjects);
        }
        if (isFileStep()) {
            for (FileSourceDTO fileSource : fileSourceList) {
                ExecuteTargetDTO fileSourceExecuteObjects = fileSource.getServers();
                if (fileSourceExecuteObjects != null) {
                    consumer.accept(fileSourceExecuteObjects);
                }
            }
        }
    }

    public ServiceStepInstanceDTO toServiceStepInstanceDTO() {
        ServiceStepInstanceDTO serviceStepInstanceDTO = new ServiceStepInstanceDTO();
        serviceStepInstanceDTO.setId(id);
        serviceStepInstanceDTO.setName(name);
        serviceStepInstanceDTO.setExecuteType(executeType.getValue());
        serviceStepInstanceDTO.setStatus(status.getValue());
        serviceStepInstanceDTO.setCreateTime(createTime);
        if (executeType == StepExecuteTypeEnum.EXECUTE_SCRIPT || executeType == StepExecuteTypeEnum.EXECUTE_SQL) {
            ServiceScriptStepInstanceDTO scriptStepInstance = new ServiceScriptStepInstanceDTO();
            scriptStepInstance.setStepInstanceId(id);
            scriptStepInstance.setScriptType(scriptType.getValue());
            scriptStepInstance.setScriptContent(scriptContent);
            scriptStepInstance.setScriptParam(scriptParam);
            scriptStepInstance.setSecureParam(secureParam);
            serviceStepInstanceDTO.setScriptStepInstance(scriptStepInstance);
        } else if (executeType == StepExecuteTypeEnum.SEND_FILE) {
            ServiceFileStepInstanceDTO fileStepInstance = new ServiceFileStepInstanceDTO();
            if (fileSourceList != null) {
                fileStepInstance.setFileSourceList(
                    fileSourceList.stream()
                        .map(FileSourceDTO::toServiceFileSourceDTO)
                        .collect(Collectors.toList())
                );
            }
            if (targetExecuteObjects != null) {
                fileStepInstance.setTargetExecuteObjects(targetExecuteObjects.toServiceExecuteTargetDTO());
            }
            serviceStepInstanceDTO.setFileStepInstance(fileStepInstance);
        }
        return serviceStepInstanceDTO;
    }
}
