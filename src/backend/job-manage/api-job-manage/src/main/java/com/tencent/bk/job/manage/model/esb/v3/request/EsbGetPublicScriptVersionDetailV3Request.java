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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.ValidationConstants;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.manage.validation.provider.EsbGetPublicScriptVersionDetailV3GroupSequenceProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 查询公共脚本详情请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@GroupSequenceProvider(EsbGetPublicScriptVersionDetailV3GroupSequenceProvider.class)
public class EsbGetPublicScriptVersionDetailV3Request extends EsbJobReq {
    /**
     * 脚本版本ID，若传入则以此条件为准屏蔽其他条件
     */
    @NotNull(
        message = "{validation.constraints.ScriptVersionId_empty.message}",
        groups = ValidationGroups.Script.ScriptVersionId.class
    )
    @Min(
        value = ValidationConstants.COMMON_MIN_1,
        message = "{validation.constraints.ScriptVersionId_empty.message}",
        groups = ValidationGroups.Script.ScriptVersionId.class
    )
    private Long id;

    /**
     * 脚本ID（可与version一起传入定位某个脚本版本）
     */
    @JsonProperty("script_id")
    @NotBlankField(
        message = "{validation.constraints.ScriptId_empty.message}",
        groups = ValidationGroups.Script.ScriptId.class
    )
    private String scriptId;

    /**
     * 脚本版本（可与script_id一起传入定位某个脚本版本）
     */
    @NotBlankField(
        message = "{validation.constraints.ScriptVersion_empty.message}",
        groups = ValidationGroups.Script.ScriptId.class
    )
    private String version;
}
