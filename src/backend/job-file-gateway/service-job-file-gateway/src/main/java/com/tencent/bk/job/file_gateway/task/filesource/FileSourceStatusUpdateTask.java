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

package com.tencent.bk.job.file_gateway.task.filesource;

import com.tencent.bk.job.file_gateway.consts.FileSourceStatusEnum;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.DispatchService;
import com.tencent.bk.job.file_gateway.service.FileService;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FileSourceStatusUpdateTask {

    private final FileService fileService;
    private final DispatchService dispatchService;
    private final FileSourceService fileSourceService;

    @Autowired
    public FileSourceStatusUpdateTask(FileService fileService, DispatchService dispatchService,
                                      FileSourceService fileSourceService) {
        this.fileService = fileService;
        this.dispatchService = dispatchService;
        this.fileSourceService = fileSourceService;
    }

    public void run() {
        List<FileSourceDTO> fileSourceDTOList;
        int start = 0;
        int pageSize = 20;
        do {
            fileSourceDTOList = fileSourceService.listWorkTableFileSource(
                null,
                null,
                null,
                start,
                pageSize
            );
            for (FileSourceDTO fileSourceDTO : fileSourceDTOList) {
                FileWorkerDTO fileWorkerDTO = dispatchService.findBestFileWorker(fileSourceDTO);
                int status;
                if (fileWorkerDTO == null) {
                    log.info(
                        "cannot find available file worker for fileSource {}:{}",
                        fileSourceDTO.getId(),
                        fileSourceDTO.getAlias()
                    );
                    status = FileSourceStatusEnum.NO_WORKER.getStatus().intValue();
                } else {
                    int onlineStatus = fileWorkerDTO.getOnlineStatus().intValue();
                    if (onlineStatus == 0) {
                        status = onlineStatus;
                    } else {
                        // 通过Worker调用listFileNode接口，OK的才算正常
                        try {
                            if (fileService.isFileAvailable(fileSourceDTO.getCreator(), fileSourceDTO.getAppId(),
                                fileSourceDTO.getId())) {
                                status = 1;
                            } else {
                                status = 0;
                            }
                        } catch (Throwable t) {
                            status = 0;
                        }
                    }
                }
                fileSourceService.updateFileSourceStatus(fileSourceDTO.getId(), status);
                log.debug("Update fileSource:fileSourceId={}, fileSourceCode={}, status={}", fileSourceDTO.getId(),
                    fileSourceDTO.getCode(), status);
            }
            start += pageSize;
        } while (fileSourceDTOList.size() == pageSize);
        log.info("Updated status of {} fileSources", start - pageSize + fileSourceDTOList.size());
    }
}
