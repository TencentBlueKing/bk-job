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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.inner.ServiceFileSourceDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件源
 */
@Slf4j
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@PersistenceObject
public class FileSourceDTO implements Cloneable {

    /**
     * 文件类型
     */
    @JsonProperty("fileType")
    private Integer fileType;
    /**
     * 文件列表
     */
    @JsonProperty("files")
    private List<FileDetailDTO> files;
    /**
     * 是否是本地上传的文件
     */
    @JsonProperty("localUpload")
    private boolean localUpload;
    /**
     * 账号名
     */
    @JsonProperty("account")
    private String account;
    /**
     * 账号别名
     */
    @JsonProperty("accountAlias")
    private String accountAlias;
    /**
     * 账号ID
     */
    @JsonProperty("accountId")
    private Long accountId;
    /**
     * 文件源服务器
     */
    @JsonProperty("servers")
    private ExecuteTargetDTO servers;

    /**
     * 文件源ID
     */
    private Integer fileSourceId;

    /**
     * 正在等待的文件源下载任务ID
     */
    private String fileSourceTaskId;

    public FileSourceDTO clone() {
        FileSourceDTO cloneFileSourceDTO = new FileSourceDTO();
        cloneFileSourceDTO.setFileType(fileType);
        if (files != null) {
            List<FileDetailDTO> cloneFiles = new ArrayList<>(files.size());
            files.forEach(fileDetailDTO -> {
                cloneFiles.add(fileDetailDTO.clone());
            });
            cloneFileSourceDTO.setFiles(cloneFiles);
        }
        cloneFileSourceDTO.setLocalUpload(localUpload);
        cloneFileSourceDTO.setAccountId(accountId);
        cloneFileSourceDTO.setAccount(account);
        cloneFileSourceDTO.setFileSourceId(fileSourceId);
        cloneFileSourceDTO.setFileSourceTaskId(fileSourceTaskId);
        if (servers != null) {
            cloneFileSourceDTO.setServers(servers.clone());
        }
        return cloneFileSourceDTO;
    }

    public ServiceFileSourceDTO toServiceFileSourceDTO() {
        ServiceFileSourceDTO serviceFileSourceDTO = new ServiceFileSourceDTO();
        serviceFileSourceDTO.setFileType(fileType);
        if (CollectionUtils.isNotEmpty(files)) {
            serviceFileSourceDTO.setFiles(
                files.stream()
                    .map(FileDetailDTO::toServiceFileDetailDTO)
                    .collect(Collectors.toList())
            );
        }
        serviceFileSourceDTO.setServers(servers.toServiceExecuteTargetDTO());
        return serviceFileSourceDTO;
    }

    /**
     * 获取文件数量
     *
     * @return 文件数量
     */
    public int getFileNum() {
        if (CollectionUtils.isEmpty(files)) {
            return 0;
        }
        return files.size();
    }

    /**
     * 获取执行对象数量
     *
     * @return 执行对象数量
     */
    public int getExecuteObjectNum() {
        // 本地文件与第三方源文件只有一台源机器
        if (localUpload || fileSourceId != null) {
            return 1;
        }
        List<ExecuteObject> executeObjects = servers.getExecuteObjects();
        if (CollectionUtils.isEmpty(executeObjects)) {
            log.warn("executeObjects is empty");
            return 0;
        }
        return executeObjects.size();
    }

    /**
     * 获取简单描述
     *
     * @return 简单描述
     */
    public String getSimpleDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileSourceDTO(");
        if (fileType != null) {
            sb.append("fileType=").append(fileType);
        }
        sb.append(",localUpload=").append(localUpload);
        if (fileSourceId != null) {
            sb.append(",fileSourceId:").append(fileSourceId);
            sb.append(",fileSourceTaskId:").append(fileSourceTaskId);
        }
        sb.append(",executeObjectNum=").append(getExecuteObjectNum());
        sb.append(",fileNum=").append(getFileNum());
        sb.append(")");
        return sb.toString();
    }

    /**
     * 是否需要校验源主机，当前只有服务器文件需要校验
     *
     * @return 布尔值
     */
    @JsonIgnore
    public boolean needToCheckHosts() {
        return getFileType() == TaskFileTypeEnum.SERVER.getType();
    }
}
