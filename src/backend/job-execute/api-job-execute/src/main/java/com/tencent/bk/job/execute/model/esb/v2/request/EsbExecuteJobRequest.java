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

package com.tencent.bk.job.execute.model.esb.v2.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.EsbCallbackDTO;
import com.tencent.bk.job.common.esb.model.job.EsbGlobalVarDTO;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 作业执行请求
 */
@Getter
@Setter
public class EsbExecuteJobRequest extends EsbAppScopeReq {

    /**
     * 执行方案 ID
     */
    @JsonProperty("bk_job_id")
    private Long taskId;

    @JsonProperty("global_vars")
    private List<EsbGlobalVarDTO> globalVars;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("bk_callback_url")
    private String callbackUrl;

    /**
     * 任务执行完成之后回调参数，比callbackUrl优先级高
     */
    @JsonProperty("callback")
    private EsbCallbackDTO callback;


    public void trimIps() {
        if (globalVars != null && globalVars.size() > 0) {
            globalVars.forEach(globalVar -> {
                trimIps(globalVar.getIpList());
                if (globalVar.getTargetServer() != null) {
                    trimIps(globalVar.getTargetServer().getIps());
                }
            });
        }
    }

    private void trimIps(List<EsbIpDTO> ips) {
        if (ips != null && ips.size() > 0) {
            ips.forEach(host -> {
                host.setIp(host.getIp().trim());
            });
        }
    }


}
