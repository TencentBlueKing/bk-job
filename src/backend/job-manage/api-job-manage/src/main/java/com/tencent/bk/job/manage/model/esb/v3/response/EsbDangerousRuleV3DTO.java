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

package com.tencent.bk.job.manage.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EsbDangerousRuleV3DTO {
    /**
     * 高危语句规则ID
     */
    private Long id;

    /**
     * 表达式
     */
    private String expression;

    /**
     * 脚本语言列表
     */
    @JsonProperty("script_language_list")
    private List<Byte> scriptTypeList;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 处理动作: 1 - 扫描, 2 - 拦截
     */
    private Integer action;

    /**
     * 启用状态: 0 - 停用, 1 - 启用
     */
    private Integer status;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间Unix时间戳（ms）
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * 最近一次修改人
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;

    /**
     * 最近一次修改时间Unix时间戳（ms）
     */
    @JsonProperty("last_modify_time")
    private Long lastModifyTime;

}
