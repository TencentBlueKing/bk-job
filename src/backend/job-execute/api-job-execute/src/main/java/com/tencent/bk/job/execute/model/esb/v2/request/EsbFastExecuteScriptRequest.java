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

package com.tencent.bk.job.execute.model.esb.v2.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.MySQLTextDataType;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.EsbServerDTO;
import com.tencent.bk.job.common.validation.NotExceedMySQLTextFieldLength;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import java.util.List;

/**
 * 脚本执行请求
 */
@Getter
@Setter
public class EsbFastExecuteScriptRequest extends EsbAppScopeReq {

    /**
     * 脚本执行任务名称
     */
    @JsonProperty("task_name")
    private String name;

    /**
     * "脚本内容，BASE64编码
     */
    @JsonProperty("script_content")
    @NotExceedMySQLTextFieldLength(
        fieldName = "script_content",
        fieldType = MySQLTextDataType.MEDIUMTEXT,
        base64 = true
    )
    private String content;

    /**
     * 执行账号/别名
     */
    private String account;

    /**
     * 脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell
     */
    @JsonProperty("script_type")
    private Integer scriptType;

    /**
     * 脚本参数， BASE64编码
     */
    @JsonProperty("script_param")
    @NotExceedMySQLTextFieldLength(
        fieldName = "script_param",
        fieldType = MySQLTextDataType.TEXT,
        base64 = true
    )
    private String scriptParam;

    /**
     * 脚本版本ID
     */
    @JsonProperty("script_id")
    private Long scriptVersionId;

    /**
     * 是否敏感参数
     */
    @JsonProperty("is_param_sensitive")
    private Integer isParamSensitive = 0;

    /**
     * 执行超时时间,单位秒
     */
    @JsonProperty("script_timeout")
    @Range(min = JobConstants.MIN_JOB_TIMEOUT_SECONDS, max = JobConstants.MAX_JOB_TIMEOUT_SECONDS,
        message = "{validation.constraints.InvalidJobTimeout_outOfRange.message}")
    private Integer timeout;

    /**
     * ip列表
     */
    @JsonProperty("ip_list")
    private List<EsbIpDTO> ipList;

    /**
     * 动态分组ID列表
     */
    @JsonProperty("custom_query_id")
    private List<String> dynamicGroupIdList;

    @JsonProperty("target_server")
    private EsbServerDTO targetServer;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("bk_callback_url")
    private String callbackUrl;

    public void trimIps() {
        trimIps(this.ipList);
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
