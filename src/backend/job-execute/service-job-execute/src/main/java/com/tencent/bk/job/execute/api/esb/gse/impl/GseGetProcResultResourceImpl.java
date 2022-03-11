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

package com.tencent.bk.job.execute.api.esb.gse.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.tencent.bk.gse.taskapi.api_map_rsp;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.api.esb.gse.GseGetProcResultResource;
import com.tencent.bk.job.execute.gse.GseApiExecutor;
import com.tencent.bk.job.execute.model.esb.gse.req.EsbGseGetProcResultRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class GseGetProcResultResourceImpl implements GseGetProcResultResource {
    private final MessageI18nService i18nService;

    @Autowired
    public GseGetProcResultResourceImpl(MessageI18nService i18nService) {
        this.i18nService = i18nService;
    }

    @Override
    public EsbResp<Map<String, Object>> gseGetProcResult(EsbGseGetProcResultRequest request) {
        log.info("Gse process result, request={}", request);
        if (!checkRequest(request)) {
            return EsbResp.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        String gseTaskId = request.getGseTaskId();

        api_map_rsp resp = new GseApiExecutor("").getGetProcRst(gseTaskId);

        if (resp == null) {
            return EsbResp.buildCommonFailResp(ErrorCode.GSE_ERROR, i18nService);
        }
        int RESULT_RUNNING = 1000115;

        if (resp.getBk_error_code() != ErrorCode.RESULT_OK && resp.getBk_error_code() != RESULT_RUNNING) {
            return EsbResp.buildCommonFailResp(resp.getBk_error_code(), resp.getBk_error_msg());
        }

        Map<String, Object> resultData = Maps.newHashMap();
        resultData.put("status", resp.getStatus());

        // 把GSE返回的字符串转换为json
        Map<String, Object> jsonResult = new HashMap<>();
        if (resp.getResult() != null && !resp.getResult().isEmpty()) {
            for (Map.Entry<String, String> resultKV : resp.getResult().entrySet()) {
                log.info("Get proc result from gse, key={},value={}", resultKV.getKey(), resultKV.getValue());
                ObjectNode procObjectNode = (ObjectNode) JsonUtils.toJsonNode(resultKV.getValue());
                if (procObjectNode != null) {
                    if (procObjectNode.get("content") != null
                        && StringUtils.isNotEmpty(procObjectNode.get("content").textValue())) {
                        ObjectNode contentNode =
                            (ObjectNode) JsonUtils.toJsonNode(procObjectNode.get("content").textValue());
                        procObjectNode.replace("content", contentNode);
                    }
                    jsonResult.put(resultKV.getKey(), procObjectNode);
                }
            }
        }
        resultData.put("result", jsonResult);

        return EsbResp.buildSuccessResp(resultData);
    }

    private boolean checkRequest(EsbGseGetProcResultRequest request) {
        if (request.getAppId() == null || request.getAppId() <= 0) {
            log.warn("AppId is empty!");
            return false;
        }
        if (StringUtils.isBlank(request.getGseTaskId())) {
            log.warn("GseTaskid is empty!");
            return false;
        }
        return true;
    }

    @Data
    @AllArgsConstructor
    private static class GseResp {
        private Integer bk_error_code;
        private String bk_error_msg;
        private Map<String, String> result;
    }
}
