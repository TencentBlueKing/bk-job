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

package com.tencent.bk.job.file_gateway.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.common.ExecuteActionReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesDTO;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesVO;
import com.tencent.bk.job.file_gateway.service.DispatchService;
import com.tencent.bk.job.file_gateway.service.FileService;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.job.file_gateway.service.remote.FileSourceReqGenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final FileSourceService fileSourceService;
    private final DispatchService dispatchService;
    private final FileSourceReqGenService fileSourceReqGenService;
    private final JobHttpClient jobHttpClient;

    @Autowired
    public FileServiceImpl(FileSourceService fileSourceService,
                           DispatchService dispatchService,
                           FileSourceReqGenService fileSourceReqGenService,
                           JobHttpClient jobHttpClient) {
        this.fileSourceService = fileSourceService;
        this.dispatchService = dispatchService;
        this.fileSourceReqGenService = fileSourceReqGenService;
        this.jobHttpClient = jobHttpClient;
    }

    private FileWorkerDTO getFileWorker(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO == null) {
            throw new InternalException(ErrorCode.FILE_SOURCE_NOT_EXIST);
        }
        return dispatchService.findBestFileWorker(fileSourceDTO);
    }

    @Override
    public boolean isFileAvailable(String username, Long appId, Integer fileSourceId) {
        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(appId, fileSourceId);
        FileWorkerDTO fileWorkerDTO = getFileWorker(fileSourceDTO);
        if (fileWorkerDTO == null) {
            throw new InternalException(ErrorCode.CAN_NOT_FIND_AVAILABLE_FILE_WORKER);
        }
        log.info("choose file worker:" + fileWorkerDTO);
        // 访问文件Worker接口，拿到available状态信息
        HttpReq req = fileSourceReqGenService.genFileAvailableReq(appId, fileWorkerDTO, fileSourceDTO);
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

    @Override
    public FileNodesVO listFileNode(String username, Long appId, Integer fileSourceId, String path, String name,
                                    Integer start, Integer pageSize) {
        if (name == null) name = "";
        final String finalName = name;
        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(appId, fileSourceId);
        FileWorkerDTO fileWorkerDTO = getFileWorker(fileSourceDTO);
        if (fileWorkerDTO == null) {
            throw new InternalException(ErrorCode.CAN_NOT_FIND_AVAILABLE_FILE_WORKER);
        }
        log.info("choose file worker:" + fileWorkerDTO);
        // 访问文件Worker接口，拿到FileNode信息
        HttpReq req = fileSourceReqGenService.genListFileNodeReq(appId, path, finalName, start, pageSize,
            fileWorkerDTO, fileSourceDTO);
        String respStr;
        try {
            respStr = jobHttpClient.post(req);
        } catch (Exception e) {
            log.error("Fail to request remote worker:", e);
            throw new InternalException(ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_LIST_FILE_NODE,
                new String[]{e.getMessage()});
        }
        log.info("respStr={}", respStr);
        FileNodesDTO fileNodesDTO = parseFileNodesDTO(respStr);
        FileNodesVO fileNodesVO = FileNodesDTO.toFileNodesVO(fileNodesDTO);
        fileNodesVO.setFileSourceInfo(FileSourceDTO.toSimpleFileSourceVO(fileSourceDTO));
        return fileNodesVO;
    }

    @Override
    public Boolean executeAction(String username, Long appId, Integer fileSourceId, ExecuteActionReq executeActionReq) {
        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(appId, fileSourceId);
        FileWorkerDTO fileWorkerDTO = getFileWorker(fileSourceDTO);
        if (fileWorkerDTO == null) {
            throw new InternalException(ErrorCode.CAN_NOT_FIND_AVAILABLE_FILE_WORKER);
        }
        log.info("choose file worker:" + fileWorkerDTO);
        HttpReq req = fileSourceReqGenService.genExecuteActionReq(appId, executeActionReq.getActionCode(),
            executeActionReq.getParams(), fileWorkerDTO, fileSourceDTO);
        String respStr;
        try {
            respStr = jobHttpClient.post(req);
            Response<Boolean> resp = JsonUtils.fromJson(respStr, new TypeReference<Response<Boolean>>() {
            });
            if (resp.isSuccess()) {
                return resp.getData();
            } else {
                throw new InternalException(resp.getCode());
            }
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                log.error("Fail to request remote worker:", e);
                throw new InternalException(ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_EXECUTE_ACTION,
                    new String[]{e.getMessage()});
            }
        }
    }

    private FileNodesDTO parseFileNodesDTO(String respStr) {
        Response<FileNodesDTO> resp;
        try {
            resp = JsonUtils.fromJson(respStr, new TypeReference<Response<FileNodesDTO>>() {
            });
        } catch (Exception e) {
            log.error("Fail to parse bucket from response={}", respStr, e);
            throw new InternalException(e.getMessage(), ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_LIST_FILE_NODE);
        }
        if (resp.isSuccess()) {
            return resp.getData();
        } else {
            log.error("get failed bucket response={}", respStr);
            throw new InternalException(resp.getCode());
        }
    }
}
