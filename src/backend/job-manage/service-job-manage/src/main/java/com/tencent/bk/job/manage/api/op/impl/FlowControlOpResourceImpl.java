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

package com.tencent.bk.job.manage.api.op.impl;

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.manage.api.op.FlowControlOpResource;
import com.tencent.bk.job.manage.model.op.req.ConfigFlowControlReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class FlowControlOpResourceImpl implements FlowControlOpResource {

    private final FlowController globalFlowController;

    @Autowired
    public FlowControlOpResourceImpl(FlowController flowController) {
        globalFlowController = flowController;
    }

    protected void logInput(Object... args) {
        StringBuilder msgTpl = new StringBuilder("Input=(");
        for (int i = 0; i < args.length; i++) {
            msgTpl.append("{}");
            if (i < args.length - 1) {
                msgTpl.append(",");
            }
        }
        msgTpl.append(")");
        log.info(msgTpl.toString(), args);
    }

    @Override
    public Response<Map<String, Long>> getCurrentFlowControlConfig(String username) {
        logInput(username);
        return Response.buildSuccessResp(globalFlowController.getCurrentConfig());
    }

    @Override
    public Response<Integer> configFlowControl(String username, ConfigFlowControlReq req) {
        logInput(username, req);
        return Response.buildSuccessResp(globalFlowController.updateConfig(req.getConfigMap()));
    }

    @Override
    public Response<Map<String, Long>> getCurrentRateMap(String username) {
        logInput(username);
        return Response.buildSuccessResp(globalFlowController.getCurrentRateMap());
    }

    @Override
    public Response<Long> getCurrentRate(String username, String resourceId) {
        logInput(username, resourceId);
        return Response.buildSuccessResp(globalFlowController.getCurrentRate(resourceId));
    }
}
