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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件源
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
    private ServersDTO servers;

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
}
