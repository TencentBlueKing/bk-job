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

package com.tencent.bk.job.execute.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;

/**
 * 脚本执行请求
 */
@Getter
@Setter
public class EsbFastExecuteScriptV3Request extends EsbReq {
    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 脚本执行任务名称
     */
    @JsonProperty("task_name")
    private String name;

    /**
     * "脚本内容，BASE64编码
     */
    @JsonProperty("script_content")
    private String content;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_alias")
    private String accountAlias;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_id")
    private Long accountId;

    /**
     * 脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell
     */
    @JsonProperty("script_language")
    private Integer scriptLanguage;

    /**
     * 脚本参数， BASE64编码
     */
    @JsonProperty("script_param")
    private String scriptParam;

    /**
     * 脚本ID
     */
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 脚本版本ID
     */
    @JsonProperty("script_version_id")
    private Long scriptVersionId;

    /**
     * 是否敏感参数
     */
    @JsonProperty("is_param_sensitive")
    private Integer isParamSensitive = 0;

    /**
     * 执行超时时间,单位秒
     */
    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("target_server")
    @Valid
    private EsbServerV3DTO targetServer;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    public void trimIps() {
        if (this.targetServer != null) {
            trimIps(this.targetServer.getIps());
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
