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
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 作业执行方案详情
 */
@Data
@Getter
@Setter
public class EsbJobDetailDTO {
    /**
     * id
     */
    @JsonProperty("bk_job_id")
    private Long id;

    /**
     * 业务id
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 模板ID
     */
    @JsonProperty("template_id")
    private Long templateId;

    /**
     * 名称
     */
    @JsonProperty("name")
    private String name;
    /**
     * 脚本创建者
     */
    @JsonProperty("creator")
    private String creator;
    /**
     * 脚本创建时间
     */
    @JsonProperty("create_time")
    private String createTime;
    /**
     * 脚本的最后修改人
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;
    /**
     * 脚本的最后修改时间
     */
    @JsonProperty("last_modify_time")
    private String lastModifyTime;
    /**
     * 作业的步骤
     */
    @JsonProperty("steps")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<EsbStepDTO> steps;
    /**
     * 全局变量
     */
    @JsonProperty("global_vars")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<EsbTaskVariableDTO> variables;
}
