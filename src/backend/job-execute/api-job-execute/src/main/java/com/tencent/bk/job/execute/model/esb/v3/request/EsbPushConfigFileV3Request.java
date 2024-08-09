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
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.ValidationConstants;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v3.validation.EsbPushConfigFileV3RequestGroupSequenceProvider;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 分发配置文件请求
 */
@Getter
@Setter
@GroupSequenceProvider(EsbPushConfigFileV3RequestGroupSequenceProvider.class)
public class EsbPushConfigFileV3Request extends EsbAppScopeReq {

    /**
     * 用户自定义任务名称
     */
    @JsonProperty("task_name")
    private String name;


    /**
     * 目标路径
     */
    @JsonProperty("file_target_path")
    @NotBlankField(message = "{validation.constraints.InvalidFileTargetPath_empty.message}")
    private String targetPath;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_alias")
    @NotBlankField(
        message = "{validation.constraints.AccountAlias_empty.message}",
        groups = ValidationGroups.Account.AccountAlias.class
    )
    private String accountAlias;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_id")
    @NotNull(
        message = "{validation.constraints.AccountId_empty.message}",
        groups = ValidationGroups.Account.AccountId.class
    )
    @Min(
        value = ValidationConstants.COMMON_MIN_1,
        message = "{validation.constraints.AccountId_empty.message}",
        groups = ValidationGroups.Account.AccountId.class
    )
    private Long accountId;

    /**
     * 目标服务器
     */
    @JsonProperty("target_server")
    @NotNull(message = "{validation.constraints.ExecuteTarget_empty.message}")
    @Valid
    private EsbServerV3DTO targetServer;

    @JsonProperty("file_list")
    @NotEmpty(message = "validation.constraints.InvalidSourceFileList_empty.message")
    @Valid
    private List<EsbConfigFileDTO> fileList;

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

    @Setter
    @Getter
    public static class EsbConfigFileDTO {

        @JsonProperty("file_name")
        @NotBlankField(message = "{validation.constraints.InvalidFileName_empty.message}")
        private String fileName;
        /**
         * 文件内容Base64
         */
        @NotBlankField(message = "{validation.constraints.InvalidFileContent_empty.message}")
        private String content;
    }


}
