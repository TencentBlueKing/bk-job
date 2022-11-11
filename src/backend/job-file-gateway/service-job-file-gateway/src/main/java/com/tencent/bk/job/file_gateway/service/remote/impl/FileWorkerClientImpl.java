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

package com.tencent.bk.job.file_gateway.service.remote.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.service.remote.FileWorkerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class FileWorkerClientImpl implements FileWorkerClient {

    private final RestTemplate restTemplate;

    @Autowired
    public FileWorkerClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String post(HttpReq req) {
        logReq(req);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.APPLICATION_JSON;
        headers.setContentType(type);
        String requestJson = req.getBody();
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> respEntity = restTemplate.postForEntity(
            req.getUrl(),
            entity,
            String.class
        );
        if (respEntity.getStatusCode() == HttpStatus.OK) {
            String respStr = respEntity.getBody();
            logRespStr(respStr);
            return respStr;
        }
        logAndThrow(respEntity);
        return null;
    }

    private void logReq(HttpReq req) {
        log.info(
            "url={},body={},headers={}",
            req.getUrl(),
            req.getBody(),
            JsonUtils.toJson(req.getHeaders())
        );
    }

    private void logRespStr(String respStr) {
        log.info("respStr={}", respStr);
    }

    private void logAndThrow(ResponseEntity<String> respEntity) {
        log.error("Fail to request fileWorker, status={}, msg={}", respEntity.getStatusCode(), respEntity.getBody());
        throw new ServiceException(
            ErrorType.INTERNAL,
            ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_WITH_REASON,
            new Object[]{
                "status=" + respEntity.getStatusCode() + ", msg=" + respEntity.getBody()
            }
        );
    }
}
