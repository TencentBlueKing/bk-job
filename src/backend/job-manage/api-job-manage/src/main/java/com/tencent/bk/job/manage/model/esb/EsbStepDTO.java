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

package com.tencent.bk.job.manage.model.esb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.job.EsbFileSourceDTO;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbStepDTO {
    @JsonProperty("step_id")
    private Long id;

    @JsonProperty("script_id")
    private Long scriptId;

    private String account;

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("is_param_sensitive")
    private Integer isParamSensive;

    @JsonProperty("script_timeout")
    private Long scriptTimeout;

    @JsonProperty("script_type")
    private Integer scriptType;

    @JsonProperty("db_account_id")
    private Integer dbAccountId;

    @JsonProperty("script_param")
    private String scriptParam;

    @JsonProperty("script_content")
    private String scriptContent;

    @JsonProperty("file_target_path")
    private String fileTargetPath;

    @JsonProperty("file_source")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<EsbFileSourceDTO> fileSources;

    @JsonProperty("ip_list")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<EsbIpDTO> ipList;

    @JsonProperty("custom_query_id")
    private List<String> customQueryId;

    /**
     * 步骤名称
     */
    @JsonProperty("name")
    private String name;
    /**
     * 步骤类型：1、执行脚本，2、传输文件 3、人工确认
     */
    @JsonProperty("type")
    private Integer type;

    /**
     * 步骤的执行次序
     */
    @JsonProperty("order")
    private Integer order;
    /**
     * 创建者
     */
    @JsonProperty("creator")
    private String creator;
    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    private String createTime;
    /**
     * 最后修改人
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;
    /**
     * 最后修改时间
     */
    @JsonProperty("last_modify_time")
    private String lastModifyTime;

    /**
     * 人工确认消息
     */
    @JsonProperty("confirm_message")
    private String confirmMessage;
}

