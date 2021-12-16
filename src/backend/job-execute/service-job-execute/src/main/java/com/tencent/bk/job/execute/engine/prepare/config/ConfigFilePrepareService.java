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

package com.tencent.bk.job.execute.engine.prepare.config;

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.consts.Consts;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContextUtil;
import com.tencent.bk.job.execute.engine.util.NFSUtils;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ConfigFilePrepareService {

    private final TaskInstanceService taskInstanceService;
    private final AgentService agentService;
    private final StorageSystemConfig storageSystemConfig;

    @Autowired
    public ConfigFilePrepareService(TaskInstanceService taskInstanceService,
                                    AgentService agentService,
                                    StorageSystemConfig storageSystemConfig) {
        this.taskInstanceService = taskInstanceService;
        this.agentService = agentService;
        this.storageSystemConfig = storageSystemConfig;
    }

    public void prepareConfigFiles(
        long stepInstanceId,
        String operator,
        List<FileSourceDTO> fileSourceList,
        ConfigFilePrepareTaskResultHandler resultHandler
    ) {
        List<List<Pair<String, String>>> configFileSourceList = new ArrayList<>();
        fileSourceList.forEach(fileSourceDTO -> {
            if (fileSourceDTO.getFileType() == TaskFileTypeEnum.CONFIG_FILE.getType()) {
                if (fileSourceDTO.isLocalUpload()) {
                    log.warn("Config file was set localupload unexpectedly, please check");
                    fileSourceDTO.setLocalUpload(false);
                }
                String uploadPath = NFSUtils.getFileDir(storageSystemConfig.getJobStorageRootPath(),
                    FileDirTypeConf.UPLOAD_FILE_DIR);
                List<Pair<String, String>> configFileList = new ArrayList<>();
                for (FileDetailDTO fileDetailDTO : fileSourceDTO.getFiles()) {
                    String fileName = fileDetailDTO.getFileName();
                    String base64EncodeContent = fileDetailDTO.getBase64Content();
                    String configFileRelativePath = JobUUID.getUUID() + File.separatorChar +
                        operator + File.separatorChar + fileName;
                    String fullFilePath = uploadPath.concat(configFileRelativePath);
                    configFileList.add(Pair.of(fullFilePath, base64EncodeContent));
                    fileDetailDTO.setFilePath(configFileRelativePath);
                }
                if (!configFileList.isEmpty()) {
                    configFileSourceList.add(configFileList);
                }
                List<IpDTO> ipDTOList = new ArrayList<>();
                ipDTOList.add(new IpDTO((long) Consts.DEFAULT_CLOUD_ID, agentService.getLocalAgentBindIp()));
                ServersDTO servers = new ServersDTO();
                servers.setStaticIpList(ipDTOList);
                servers.setIpList(ipDTOList);
                fileSourceDTO.setServers(servers);
            }
        });
        // 更新配置文件任务内容
        taskInstanceService.updateResolvedSourceFile(stepInstanceId, fileSourceList);
        if (!configFileSourceList.isEmpty()) {
            log.info("[{}] configFile parsed, begin to prepare", stepInstanceId);
            new ConfigFilePrepareTask(
                stepInstanceId,
                false,
                resultHandler,
                configFileSourceList
            ).execute();
        } else {
            resultHandler.onSuccess(JobTaskContextUtil.getSimpleTaskContext(false));
        }
    }
}
