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

package com.tencent.bk.job.file_gateway.model.resp.esb.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class EsbFileSourceV3DTO extends EsbAppScopeDTO {
    /**
     * id
     */
    @JsonPropertyDescription("File source id")
    private Integer id;
    /**
     * 文件源标识
     */
    @JsonPropertyDescription("File source code")
    private String code;

    /**
     * 文件源别名
     */
    @JsonPropertyDescription("File source alias")
    private String alias;
    /**
     * 状态
     */
    @JsonPropertyDescription("File source status")
    private Integer status;
    /**
     * 文件源类型code
     */
    @JsonProperty("file_source_type_code")
    @JsonPropertyDescription("File source type code")
    private String fileSourceTypeCode;

    /**
     * 是否为公共文件源
     */
    @JsonProperty("is_public")
    @JsonPropertyDescription("Is public file source")
    private boolean publicFlag;

    /**
     * 凭证Id
     */
    @JsonPropertyDescription("File source credential id")
    @JsonProperty("credential_id")
    private String credentialId;

    /**
     * 是否启用
     */
    @JsonPropertyDescription("Is file source enabled")
    private Boolean enable;

    /**
     * 创建人
     */
    @JsonPropertyDescription("Creator")
    private String creator;
    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    @JsonPropertyDescription("Create time")
    private Long createTime;
    /**
     * 更新人
     */
    @JsonProperty("last_modify_user")
    @JsonPropertyDescription("Last modify user")
    private String lastModifyUser;

    /**
     * 更新时间
     */
    @JsonProperty("last_modify_time")
    @JsonPropertyDescription("Last modify time")
    private Long lastModifyTime;

}
