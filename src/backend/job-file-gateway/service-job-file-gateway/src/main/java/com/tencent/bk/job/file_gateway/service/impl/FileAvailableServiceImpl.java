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

package com.tencent.bk.job.file_gateway.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.NoTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.FileAvailableService;
import com.tencent.bk.job.file_gateway.service.dispatch.DispatchService;
import com.tencent.bk.job.file_gateway.service.remote.FileSourceReqGenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class FileAvailableServiceImpl implements FileAvailableService {

    private final NoTenantFileSourceDAO noTenantFileSourceDAO;
    private final DispatchService dispatchService;
    private final FileSourceReqGenService fileSourceReqGenService;
    private final JobHttpClient jobHttpClient;

    @Autowired
    public FileAvailableServiceImpl(NoTenantFileSourceDAO noTenantFileSourceDAO,
                                    DispatchService dispatchService,
                                    FileSourceReqGenService fileSourceReqGenService,
                                    JobHttpClient jobHttpClient) {
        this.noTenantFileSourceDAO = noTenantFileSourceDAO;
        this.dispatchService = dispatchService;
        this.fileSourceReqGenService = fileSourceReqGenService;
        this.jobHttpClient = jobHttpClient;
    }

    private FileWorkerDTO getFileWorker(FileSourceDTO fileSourceDTO, String requestSource) {
        if (fileSourceDTO == null) {
            throw new InternalException(ErrorCode.FILE_SOURCE_NOT_EXIST);
        }
        return dispatchService.findBestFileWorker(fileSourceDTO, requestSource);
    }

    @Override
    public boolean isFileAvailable(Integer fileSourceId) {
        FileSourceDTO fileSourceDTO = noTenantFileSourceDAO.getFileSourceById(fileSourceId);
        FileWorkerDTO fileWorkerDTO = getFileWorker(fileSourceDTO, "isFileAvailable");
        if (fileWorkerDTO == null) {
            throw new InternalException(ErrorCode.CAN_NOT_FIND_AVAILABLE_FILE_WORKER);
        }
        log.info("choose file worker:" + fileWorkerDTO.getBasicDesc());
        // 访问文件Worker接口，拿到available状态信息
        HttpReq req = fileSourceReqGenService.genFileAvailableReq(fileWorkerDTO, fileSourceDTO);
        String respStr;
        try {
            respStr = jobHttpClient.post(req);
            Response<Boolean> resp = JsonUtils.fromJson(respStr,
                new TypeReference<Response<Boolean>>() {
                });
            return resp.getData();
        } catch (Exception e) {
            log.error("Fail to request remote worker:", e);
            return false;
        }
    }
}
