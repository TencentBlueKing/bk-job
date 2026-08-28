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

package com.tencent.bk.job.file_gateway.model.resp.inner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 单个文件源对某业务的可用性判定结果。
 * 只描述事实，不含鉴权结论：调用方拿到 ownerAppId 后自行决定是否要做 view_file_source 鉴权。
 */
@Schema(description = "文件源可用性")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceFileSourceAvailabilityDTO {

    @Schema(description = "文件源ID")
    private Integer id;

    /**
     * 是否在目标业务的可见范围内：归属该业务/已显式共享给该业务/已共享给所有业务，三者之一成立。
     * 不含启用状态，便于调用方把「不可见」与「可见但已禁用」区分成不同的错误。
     */
    @Schema(description = "是否在目标业务的可见范围内，不含启用状态")
    private boolean inAppScope;

    /**
     * 归属业务ID。文件源不存在时为 null。
     */
    @Schema(description = "归属业务ID，文件源不存在时为null")
    private Long ownerAppId;

    /**
     * 别名。文件源不存在时为 null。调用方构造权限申请信息时需要它，避免再回查一次资源名。
     */
    @Schema(description = "文件源别名，文件源不存在时为null")
    private String alias;

    /**
     * 是否启用。文件源不存在时为 null。
     */
    @Schema(description = "是否启用，文件源不存在时为null")
    private Boolean enable;

    /**
     * 文件源是否存在。
     */
    @JsonIgnore
    public boolean exists() {
        return ownerAppId != null;
    }

    /**
     * 是否可被目标业务引用。
     */
    @JsonIgnore
    public boolean isAvailable() {
        return inAppScope && Boolean.TRUE.equals(enable);
    }

    @JsonIgnore
    public String getDisplayName() {
        if (StringUtils.isNotBlank(alias)) {
            return id + "(" + alias + ")";
        } else {
            return String.valueOf(id);
        }
    }
}
