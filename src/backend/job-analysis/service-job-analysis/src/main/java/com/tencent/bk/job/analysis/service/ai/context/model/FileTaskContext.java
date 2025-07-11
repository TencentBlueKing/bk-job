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

package com.tencent.bk.job.analysis.service.ai.context.model;

import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.model.inner.ServiceExecuteObject;
import com.tencent.bk.job.execute.model.inner.ServiceExecuteTargetDTO;
import com.tencent.bk.job.execute.model.inner.ServiceFileSourceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceFileStepInstanceDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public class FileTaskContext {
    /**
     * 文件任务名称
     */
    private String name;
    /**
     * 文件分发任务实例
     */
    ServiceFileStepInstanceDTO fileStepInstance;
    /**
     * 文件分发任务错误根源分析结果
     */
    FileTaskErrorSourceResult errorSourceResult;

    Map<String, ServiceExecuteObject> executeObjectMap;

    public FileTaskContext(String name,
                           ServiceFileStepInstanceDTO fileStepInstance,
                           FileTaskErrorSourceResult errorSourceResult) {
        this.name = name;
        this.fileStepInstance = fileStepInstance;
        this.errorSourceResult = errorSourceResult;
    }

    private void buildExecuteObjectMapIfNeeded() {
        if (executeObjectMap != null) {
            return;
        }
        executeObjectMap = new HashMap<>();
        List<ServiceFileSourceDTO> fileSourceList = fileStepInstance.getFileSourceList();
        if (CollectionUtils.isNotEmpty(fileSourceList)) {
            for (ServiceFileSourceDTO fileSource : fileSourceList) {
                ServiceExecuteTargetDTO servers = fileSource.getServers();
                if (servers == null) {
                    continue;
                }
                List<ServiceExecuteObject> executeObjects = servers.getExecuteObjects();
                if (CollectionUtils.isEmpty(executeObjects)) {
                    continue;
                }
                for (ServiceExecuteObject executeObject : executeObjects) {
                    executeObjectMap.put(executeObject.getId(), executeObject);
                }
            }
        }
        ServiceExecuteTargetDTO targetExecuteObjects = fileStepInstance.getTargetExecuteObjects();
        if (targetExecuteObjects == null) {
            return;
        }
        List<ServiceExecuteObject> executeObjects = targetExecuteObjects.getExecuteObjects();
        if (CollectionUtils.isEmpty(executeObjects)) {
            return;
        }
        for (ServiceExecuteObject executeObject : executeObjects) {
            executeObjectMap.put(executeObject.getId(), executeObject);
        }
    }

    public String getFileTaskErrorSourceI18nKey() {
        return errorSourceResult.getErrorSource().getI18nKey();
    }

    @SuppressWarnings("DuplicatedCode")
    public String getUploadFileErrorData() {
        buildExecuteObjectMapIfNeeded();
        List<UploadFileErrorInfo> uploadFileErrorInfoList = new ArrayList<>();
        List<ServiceFileTaskLogDTO> uploadFailLogs = errorSourceResult.getUploadFailLogs();
        if (CollectionUtils.isEmpty(uploadFailLogs)) {
            return JsonUtils.toJson(uploadFileErrorInfoList);
        }
        for (ServiceFileTaskLogDTO uploadFailLog : uploadFailLogs) {
            UploadFileErrorInfo uploadFileErrorInfo = new UploadFileErrorInfo();
            String srcExecuteObjectId = uploadFailLog.getSrcExecuteObjectId();
            ServiceExecuteObject executeObject = executeObjectMap.get(srcExecuteObjectId);
            ExecuteObjectTypeEnum executeObjectType = ExecuteObjectTypeEnum.valOf(executeObject.getType());
            uploadFileErrorInfo.setSourceExecuteObjectType(executeObjectType.name());
            uploadFileErrorInfo.setSourceFilePath(uploadFailLog.getDisplaySrcFile());
            if (executeObjectType == ExecuteObjectTypeEnum.HOST) {
                HostDescription sourceHostDescription = new HostDescription();
                sourceHostDescription.setCloudIp(executeObject.getHost().toCloudIp());
                uploadFileErrorInfo.setSourceHostDescription(sourceHostDescription);
            } else if (executeObjectType == ExecuteObjectTypeEnum.CONTAINER) {
                ContainerDescription sourceContainerDescription = new ContainerDescription();
                sourceContainerDescription.setUid(executeObject.getContainer().getContainerId());
                uploadFileErrorInfo.setSourceContainerDescription(sourceContainerDescription);
            }
            // 填充错误日志信息
            String lastLineLog = getLastLineLog(uploadFailLog.getContent());
            uploadFileErrorInfo.setErrorLog(lastLineLog);

            uploadFileErrorInfoList.add(uploadFileErrorInfo);
        }
        return JsonUtils.toJson(uploadFileErrorInfoList);
    }

    @SuppressWarnings("DuplicatedCode")
    public String getDownloadFileErrorData() {
        buildExecuteObjectMapIfNeeded();
        List<DownloadFileErrorInfo> downloadFileErrorInfoList = new ArrayList<>();
        List<ServiceFileTaskLogDTO> downloadFailLogs = errorSourceResult.getDownloadFailLogs();
        if (CollectionUtils.isEmpty(downloadFailLogs)) {
            return JsonUtils.toJson(downloadFileErrorInfoList);
        }
        for (ServiceFileTaskLogDTO downloadFailLog : downloadFailLogs) {
            DownloadFileErrorInfo downloadFileErrorInfo = new DownloadFileErrorInfo();
            // 填充下载文件的源执行对象信息
            String srcExecuteObjectId = downloadFailLog.getSrcExecuteObjectId();
            ServiceExecuteObject srcExecuteObject = executeObjectMap.get(srcExecuteObjectId);
            ExecuteObjectTypeEnum srcExecuteObjectType = ExecuteObjectTypeEnum.valOf(srcExecuteObject.getType());
            downloadFileErrorInfo.setSourceExecuteObjectType(srcExecuteObjectType.name());
            if (srcExecuteObjectType == ExecuteObjectTypeEnum.HOST) {
                HostDescription sourceHostDescription = new HostDescription();
                sourceHostDescription.setCloudIp(srcExecuteObject.getHost().toCloudIp());
                downloadFileErrorInfo.setSourceHostDescription(sourceHostDescription);
            } else if (srcExecuteObjectType == ExecuteObjectTypeEnum.CONTAINER) {
                ContainerDescription sourceContainerDescription = new ContainerDescription();
                sourceContainerDescription.setUid(srcExecuteObject.getContainer().getContainerId());
                downloadFileErrorInfo.setSourceContainerDescription(sourceContainerDescription);
            }

            // 填充下载文件的目标执行对象信息
            String destExecuteObjectId = downloadFailLog.getDestExecuteObjectId();
            ServiceExecuteObject targetExecuteObject = executeObjectMap.get(destExecuteObjectId);
            ExecuteObjectTypeEnum targetExecuteObjectType = ExecuteObjectTypeEnum.valOf(targetExecuteObject.getType());
            downloadFileErrorInfo.setTargetExecuteObjectType(targetExecuteObjectType.name());
            downloadFileErrorInfo.setTargetFilePath(downloadFailLog.getDestFile());
            if (targetExecuteObjectType == ExecuteObjectTypeEnum.HOST) {
                HostDescription targetHostDescription = new HostDescription();
                targetHostDescription.setCloudIp(targetExecuteObject.getHost().toCloudIp());
                downloadFileErrorInfo.setTargetHostDescription(targetHostDescription);
            } else if (targetExecuteObjectType == ExecuteObjectTypeEnum.CONTAINER) {
                ContainerDescription targetContainerDescription = new ContainerDescription();
                targetContainerDescription.setUid(targetExecuteObject.getContainer().getContainerId());
                downloadFileErrorInfo.setTargetContainerDescription(targetContainerDescription);
            }
            // 填充错误日志信息
            downloadFileErrorInfo.setErrorLog(getLastLineLog(downloadFailLog.getContent()));
            downloadFileErrorInfoList.add(downloadFileErrorInfo);
        }
        return JsonUtils.toJson(downloadFileErrorInfoList);
    }

    /**
     * 获取日志的最后一行
     *
     * @param log 原始日志
     * @return 最后一行日志内容
     */
    private String getLastLineLog(String log) {
        if (log != null) {
            String[] lines = log.split("\n");
            return lines[lines.length - 1];
        }
        return null;
    }
}
