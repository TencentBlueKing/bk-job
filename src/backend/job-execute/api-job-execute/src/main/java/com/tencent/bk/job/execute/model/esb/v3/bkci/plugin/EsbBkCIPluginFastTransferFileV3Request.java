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

package com.tencent.bk.job.execute.model.esb.v3.bkci.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbRollingConfigDTO;
import com.tencent.bk.job.execute.validation.ValidTimeoutLimit;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;


@Setter
@Getter
public class EsbBkCIPluginFastTransferFileV3Request extends EsbAppScopeReq {

    /**
     * 文件分发任务名称
     */
    @JsonProperty("task_name")
    private String name;
    /**
     * 源文件
     */
    @JsonProperty("file_source_list")
    private List<EsbFileSourceV3DTO> fileSources;

    /**
     * 目标路径
     */
    @JsonProperty("file_target_path")
    private String targetPath;

    /**
     * 目标文件名
     */
    @JsonProperty("file_target_name")
    private String targetName;

    /**
     * 目标服务器账户别名
     */
    @JsonProperty("account_alias")
    private String accountAlias;

    /**
     * 目标服务器账号ID
     */
    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("execute_object")
    private OpenApiExecuteTargetDTO executeObject;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    /**
     * 下载限速，单位MB
     */
    @JsonProperty("download_speed_limit")
    private Integer downloadSpeedLimit;

    /**
     * 上传限速，单位MB
     */
    @JsonProperty("upload_speed_limit")
    private Integer uploadSpeedLimit;

    /**
     * 超时时间，单位秒
     */
    @JsonProperty("timeout")
    @ValidTimeoutLimit
    private Integer timeout;

    /**
     * 传输模式。1-严谨模式，2-强制模式
     */
    @JsonProperty("transfer_mode")
    private Integer transferMode;

    /**
     * 滚动配置
     */
    @JsonProperty("rolling_config")
    @Valid
    private EsbRollingConfigDTO rollingConfig;

    @JsonIgnore
    public EsbBkCIPluginFastTransferFileV3Request trimTargetPath() {
        targetPath = targetPath == null ? null : targetPath.trim();
        return this;
    }

}
