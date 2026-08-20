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

package com.tencent.bk.job.execute.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.execute.model.esb.v3.EsbRollingConfigDTO;
import com.tencent.bk.job.execute.validate.ValidCallbackUrl;
import com.tencent.bk.job.execute.validation.ValidTimeoutLimit;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import java.util.List;

/**
 * v4 分发文件请求。
 * <p>
 * 与 v3 的 EsbFastTransferFileV3Request 的协议差异：
 * <ul>
 *     <li>不再提供 bk_biz_id 兼容字段，业务范围只用 bk_scope_type + bk_scope_id；</li>
 *     <li>分发目标与源文件所在主机统一用 v4 的 {@link V4ExecuteTargetDTO}，支持容器执行对象；</li>
 *     <li>源文件的账号平铺为 account_id / account_alias，见 {@link V4FileSourceDTO}。</li>
 * </ul>
 */
@Getter
@Setter
public class V4FastTransferFileRequest extends EsbAppScopeReq {

    /**
     * 文件分发任务名称，不传时由系统生成
     */
    @JsonProperty("task_name")
    @Length(max = 512, message = "{validation.constraints.TaskName_outOfLength.message}")
    private String name;

    /**
     * 源文件列表
     */
    @JsonProperty("file_source_list")
    @Valid
    private List<V4FileSourceDTO> fileSources;

    /**
     * 文件分发到目标主机的目录
     */
    @JsonProperty("file_target_path")
    private String targetPath;

    /**
     * 文件分发到目标主机的文件名
     */
    @JsonProperty("file_target_name")
    private String targetName;

    /**
     * 目标主机的执行账号别名，与 accountId 二者至少填一个
     */
    @JsonProperty("account_alias")
    private String accountAlias;

    /**
     * 目标主机的执行账号 ID，与 accountAlias 二者至少填一个
     */
    @JsonProperty("account_id")
    private Long accountId;

    /**
     * 分发目标
     */
    @JsonProperty("execute_target")
    @Valid
    private V4ExecuteTargetDTO executeTarget;

    /**
     * 任务执行完成后的回调 url
     */
    @JsonProperty("callback_url")
    @ValidCallbackUrl
    private String callbackUrl;

    /**
     * 下载限速，单位 MB
     */
    @JsonProperty("download_speed_limit")
    private Integer downloadSpeedLimit;

    /**
     * 上传限速，单位 MB
     */
    @JsonProperty("upload_speed_limit")
    private Integer uploadSpeedLimit;

    /**
     * 执行超时时间，单位秒
     */
    @JsonProperty("timeout")
    @ValidTimeoutLimit
    private Integer timeout;

    /**
     * 传输模式。1-严谨模式（目标路径不存在则失败），2-强制模式（目标路径不存在则创建、同名文件覆盖），
     * 3-保险模式（按源主机分目录存放），4-保险模式（按日期与源主机分目录存放）。
     * 不传默认强制模式，传枚举外的值报错
     */
    @JsonProperty("transfer_mode")
    private Integer transferMode;

    /**
     * 滚动配置
     */
    @JsonProperty("rolling_config")
    @Valid
    private EsbRollingConfigDTO rollingConfig;

    /**
     * 是否立即启动任务
     */
    @JsonProperty("start_task")
    private Boolean startTask = true;

    @JsonIgnore
    public String getTrimmedTargetPath() {
        return targetPath == null ? null : targetPath.trim();
    }
}
