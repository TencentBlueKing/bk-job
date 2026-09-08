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
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * v4 文件分发的源文件。
 * <p>
 * 与 v3 的 EsbFileSourceV3DTO 的两处协议差异：
 * <ul>
 *     <li>源文件所在主机用 v4 的 {@link V4ExecuteTargetDTO}，不再用 v3 的 server 结构；</li>
 *     <li>账号不再嵌一层 account 对象，直接平铺 accountId / accountAlias，与请求体顶层的账号写法一致。</li>
 * </ul>
 */
@Data
public class V4FileSourceDTO {

    /**
     * 源文件路径列表
     */
    @JsonProperty("file_list")
    private List<String> files;

    /**
     * 文件源类型：1-服务器文件，2-本地文件，3-第三方文件源文件。不传默认为服务器文件
     */
    @JsonProperty("file_type")
    private Integer fileType;

    /**
     * 源文件所在主机的执行账号 ID，与 accountAlias 二者至少填一个（服务器文件必填）
     */
    @JsonProperty("account_id")
    private Long accountId;

    /**
     * 源文件所在主机的执行账号别名，与 accountId 二者至少填一个（服务器文件必填）
     */
    @JsonProperty("account_alias")
    private String accountAlias;

    /**
     * 源文件所在主机（服务器文件必填）
     */
    @JsonProperty("execute_target")
    @Valid
    private V4ExecuteTargetDTO executeTarget;

    /**
     * 第三方文件源 ID，与 fileSourceCode 二者填一个（第三方文件源文件必填）
     */
    @JsonProperty("file_source_id")
    private Integer fileSourceId;

    /**
     * 第三方文件源标识，与 fileSourceId 二者填一个（第三方文件源文件必填）
     */
    @JsonProperty("file_source_code")
    private String fileSourceCode;

    @JsonIgnore
    public List<String> getTrimmedFiles() {
        if (CollectionUtils.isEmpty(files)) {
            return files;
        }
        return files.stream()
            .map(file -> file == null ? null : file.trim())
            .collect(Collectors.toList());
    }
}
